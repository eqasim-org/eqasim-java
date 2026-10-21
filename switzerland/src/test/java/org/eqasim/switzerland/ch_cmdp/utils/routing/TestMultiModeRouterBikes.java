package org.eqasim.switzerland.ch_cmdp.utils.routing;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.scenario.routing.ReusableRoute;
import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

public class TestMultiModeRouterBikes {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private static final String HEADER = "identifier,origin_x,origin_y,destination_x,destination_y,departure_time,mode\n";

    @Test
    public void networkBikeUsesGradientsSpeedLimitsAndRoadPenaltiesWithAndWithoutMarouter() throws Exception {
        Path config = config(true);
        Path bike = write(HEADER + "bike,0,0,200,0,0,bike\n");
        for (String roadType : List.of("primary", "motorway")) {
            Network network = network(roadType);
            Path path = folder.newFile().toPath(); new NetworkWriter(network).write(path.toString());
            for (boolean assignment : List.of(false, true)) {
                try (var router = new MultiModeRouter(settings(path, config, assignment))) {
                    assertTrue(router.getAvailableModes().contains("bike"));
                    Path output = folder.newFile().toPath(); router.route(bike, output, "bike");
                    var result = rows(output).getFirst();
                    String links = roadType.equals("primary") ? "flat1-flat2" : "hill1-hill2";
                    // Primary detour wins; motorway penalties make the steep route preferable instead.
                    assertEquals(links, result.get("links"));
                    double expected = roadType.equals("primary") ? 100.0 / 3 + 100.0 / 5.9 : 100.0 / 2 + 100.0 / 7.5;
                    assertEquals(expected, Double.parseDouble(result.get("travel_time")), 1e-5);
                    assertEquals(200, Double.parseDouble(result.get("travel_distance")), 0);
                    assertEquals(0, Double.parseDouble(result.get("cost")), 0);
                    var exported = ReusableRoute.read(result.get("route"));
                    assertEquals("bike", exported.mode());
                    assertEquals(List.of(links.split("-")), exported.parts().getFirst().route().links());
                }
            }
        }
    }

    @Test
    public void mixedBikeRequestsDoNotChangeCarCongestionOrBikeTimes() throws Exception {
        Network shared = network("motorway"); shared.removeLink(Id.createLinkId("car-only"));
        Path network = folder.newFile().toPath(); new NetworkWriter(shared).write(network.toString());
        Path config = config(true);
        StringBuilder cars = new StringBuilder(HEADER), mixed = new StringBuilder(HEADER);
        for (int i = 0; i < 200; i++) mixed.append("bike-").append(i).append(",0,0,200,0,0,bike\n");
        for (int i = 0; i < 2; i++) {
            String row = "car-" + i + ",0,0,200,0,0,car\n"; cars.append(row); mixed.append(row);
        }
        Path carInput = write(cars.toString()), mixedInput = write(mixed.toString());
        Path carOutput = folder.newFile().toPath(), mixedOutput = folder.newFile().toPath();
        try (var router = new MultiModeRouter(settings(network, config, true))) {
            router.route(carInput, carOutput); router.route(mixedInput, mixedOutput);
            var original = rows(carOutput); var results = rows(mixedOutput);
            for (int i = 0; i < 200; i++) {
                assertEquals("hill1-hill2", results.get(i).get("links"));
                assertEquals(100.0 / 2 + 100.0 / 7.5, Double.parseDouble(results.get(i).get("travel_time")), 1e-5);
            }
            for (int i = 0; i < 2; i++) {
                assertEquals("hill1-hill2", original.get(i).get("links"));
                assertEquals(104, Double.parseDouble(original.get(i).get("travel_time")), 1e-9);
                assertEquals(original.get(i).get("links"), results.get(200 + i).get("links"));
                assertEquals(original.get(i).get("travel_time"), results.get(200 + i).get("travel_time"));
            }
        }
    }

    @Test
    public void teleportedBikeUsesConfiguredSpeedAndDistanceFactorWithAndWithoutMarouter() throws Exception {
        Path network = folder.newFile().toPath(); new NetworkWriter(network("primary")).write(network.toString());
        Path config = config(false), input = write(HEADER + "bike,0,0,200,0,0,bike\n");
        for (boolean assignment : List.of(false, true)) {
            try (var router = new MultiModeRouter(settings(network, config, assignment))) {
                Path output = folder.newFile().toPath(); router.route(input, output);
                var result = rows(output).getFirst();
                assertEquals(260, Double.parseDouble(result.get("travel_distance")), 1e-9);
                assertEquals(65, Double.parseDouble(result.get("travel_time")), 1e-9);
                assertEquals("", result.get("links"));
                assertEquals(0, Double.parseDouble(result.get("cost")), 0);
                var route = ReusableRoute.read(result.get("route")).parts().getFirst().route();
                assertEquals("generic", route.type()); assertEquals(260, route.distance(), 1e-9);
            }
        }
    }

    private Network network(String roadType) {
        Network network = NetworkUtils.createNetwork(); var factory = network.getFactory();
        var coords = List.of(new Coord(0, 0, 100), new Coord(100, 0, 120), new Coord(100, 100, 100), new Coord(200, 0, 100));
        var nodes = new ArrayList<org.matsim.api.core.v01.network.Node>();
        for (int i = 0; i < coords.size(); i++) {
            var node = factory.createNode(Id.createNodeId(i), coords.get(i)); nodes.add(node); network.addNode(node);
        }
        var names = List.of("hill1", "hill2", "flat1", "flat2", "car-only");
        int[] from = {0, 1, 0, 2, 0}, to = {1, 3, 2, 3, 3};
        for (int i = 0; i < names.size(); i++) {
            var link = factory.createLink(Id.createLinkId(names.get(i)), nodes.get(from[i]), nodes.get(to[i]));
            link.setLength(i == 4 ? 20 : 100); link.setFreespeed(i == 2 ? 3 : 20); link.setCapacity(100); link.setNumberOfLanes(1);
            link.setAllowedModes(i == 4 ? Set.of("car") : Set.of("car", "bike"));
            link.getAttributes().putAttribute("osm:way:highway", i == 2 || i == 3 ? roadType : "residential");
            link.getAttributes().putAttribute("speedFactor", 0.1); // Road calibration must not slow bikes.
            network.addLink(link);
        }
        return network;
    }
    private Path config(boolean networkBike) throws Exception {
        var config = ConfigUtils.createConfig(); config.routing().setNetworkModes(networkBike ? Set.of("car", "bike") : Set.of("car"));
        var bike = config.routing().getOrCreateModeRoutingParams("bike"); bike.setTeleportedModeSpeed(4.0); bike.setBeelineDistanceFactor(1.3);
        var eqasim = new EqasimConfigGroup(); eqasim.setSampleSize(0.1); config.addModule(eqasim);
        var vdf = new VDFConfigGroup(); vdf.setBprFactor(1); vdf.setBprExponent(2); config.addModule(vdf);
        Path path = folder.newFile().toPath(); new ConfigWriter(config).write(path.toString()); return path;
    }
    private MultiModeRouter.Settings settings(Path network, Path config, boolean assignment) throws Exception {
        return MultiModeRouterServer.readSettings(MultiModeRouterServer.commandLine(new String[]{"--network-path", network.toString(),
                "--config-path", config.toString(), "--port", "8080", "--threads", "4", "--chunk-size", "13", "--batch-size", "3",
                "--compute-cost", "true", "--return-links", "true", "--export-routes", "true", "--marouter", String.valueOf(assignment)}));
    }
    private Path write(String text) throws Exception { Path path = folder.newFile().toPath(); Files.writeString(path, text); return path; }
    private static List<Map<String, String>> rows(Path path) throws Exception {
        var csv = new CsvMapper();
        try (MappingIterator<Map<String, String>> rows = csv.readerFor(csv.getTypeFactory().constructMapType(Map.class, String.class, String.class))
                .with(CsvSchema.emptySchema().withHeader()).readValues(path.toFile())) { return rows.readAll(); }
    }
}
