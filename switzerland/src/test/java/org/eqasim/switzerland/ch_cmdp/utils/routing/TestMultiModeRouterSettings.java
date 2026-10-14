package org.eqasim.switzerland.ch_cmdp.utils.routing;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eqasim.switzerland.ch_cmdp.tolls.MarginalCostOfTolls;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleType;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

public class TestMultiModeRouterSettings {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void omittedCliValuesComeFromConfigAndExplicitZeroOverridesConfig() throws Exception {
        Path config = config("""
                <module name="global"><param name="numberOfThreads" value="3" /></module>
                <module name="eqasim"><param name="routingDistanceUtility" value="0.8" /></module>
                <module name="eqasim:networkCalibration">
                  <param name="tollsValueOfTime" value="27" /><param name="tollsSigma" value="0.7" />
                </module>
                """);
        var settings = settings(config);
        settings.applyConfigDefaults(MultiModeRouter.loadConfig(settings));
        assertEquals(3, settings.threads);
        assertEquals(0.8, settings.routingDistanceUtility, 0);
        assertEquals(27, settings.tollsValueOfTime, 0);
        assertEquals(0.7, settings.tollsSigma, 0);

        settings = settings(config, "--threads", "6", "--routingDistanceUtility", "0",
                "--tolls-value-of-time", "12", "--tolls-sigma", "0");
        settings.applyConfigDefaults(MultiModeRouter.loadConfig(settings));
        assertEquals(6, settings.threads);
        assertEquals(0, settings.routingDistanceUtility, 0);
        assertEquals(12, settings.tollsValueOfTime, 0);
        assertEquals(0, settings.tollsSigma, 0);
    }

    @Test
    public void missingModulesAndMissingIndividualParametersKeepSettingsDefaults() throws Exception {
        for (String body : List.of("", """
                <module name="global"><param name="coordinateSystem" value="EPSG:2056" /></module>
                <module name="eqasim"><param name="sampleSize" value="0.1" /></module>
                <module name="eqasim:networkCalibration"><param name="activate" value="false" /></module>
                """)) {
            var settings = settings(config(body));
            settings.threads = 7;
            settings.routingDistanceUtility = 0.2;
            settings.tollsValueOfTime = 19;
            settings.tollsSigma = 0.3;
            settings.applyConfigDefaults(MultiModeRouter.loadConfig(settings));
            assertEquals(7, settings.threads);
            assertEquals(0.2, settings.routingDistanceUtility, 0);
            assertEquals(19, settings.tollsValueOfTime, 0);
            assertEquals(0.3, settings.tollsSigma, 0);
        }
        var partial = settings(config("""
                <module name="eqasim:networkCalibration"><param name="tollsSigma" value="0.9" /></module>
                """));
        partial.applyConfigDefaults(MultiModeRouter.loadConfig(partial));
        assertEquals(15, partial.tollsValueOfTime, 0);
        assertEquals(0.9, partial.tollsSigma, 0);
    }

    @Test
    public void withoutConfigDefaultsAndExplicitOptionsArePreserved() throws Exception {
        var defaults = settings(null);
        defaults.applyConfigDefaults(MultiModeRouter.loadConfig(defaults));
        assertEquals(Runtime.getRuntime().availableProcessors(), defaults.threads);
        assertEquals(0, defaults.routingDistanceUtility, 0);
        assertEquals(15, defaults.tollsValueOfTime, 0);
        assertEquals(0, defaults.tollsSigma, 0);
        var explicit = settings(null, "--threads", "2", "--tolls-sigma", "0.4", "--tolls-value-of-time", "22");
        explicit.applyConfigDefaults(null);
        assertEquals(2, explicit.threads);
        assertEquals(0.4, explicit.tollsSigma, 0);
        assertEquals(22, explicit.tollsValueOfTime, 0);
    }

    @Test
    public void invalidConfigTollParametersFailAndCliOverrideTakesPrecedence() throws Exception {
        Path config = config("""
                <module name="eqasim:networkCalibration"><param name="tollsSigma" value="-1" /></module>
                """);
        var invalid = settings(config);
        assertThrows(IllegalArgumentException.class, () -> new MultiModeRouter(invalid));
        var explicit = settings(config, "--tolls-sigma", "0");
        explicit.applyConfigDefaults(MultiModeRouter.loadConfig(explicit));
        assertEquals(0, explicit.tollsSigma, 0);
    }

    @Test
    public void positiveSigmaUsesReproducibleTripSpecificTollSensitivity() throws Exception {
        Path network = network();
        Path config = config("""
                <module name="global"><param name="numberOfThreads" value="2" /></module>
                <module name="eqasim:networkCalibration">
                  <param name="tollsValueOfTime" value="15" /><param name="tollsSigma" value="1" />
                </module>
                """);
        Path input = folder.newFile("trips.csv").toPath();
        StringBuilder csv = new StringBuilder("identifier,origin_x,origin_y,destination_x,destination_y,mode\n");
        for (int i = 0; i < 100; i++) {
            csv.append("trip-").append(i).append(",0,0,2000,0,car\n");
        }
        csv.append(",0,0,2000,0,car\n"); // Anonymous trips also get a deterministic vehicle ID.
        Files.writeString(input, csv);
        Path output = folder.getRoot().toPath().resolve("routes.csv");
        var settings = settings(config, "--return-links", "true", "--chunk-size", "11", "--batch-size", "2");
        settings.networkPath = network.toString();
        try (var router = new MultiModeRouter(settings)) {
            assertEquals(2, settings.threads);
            assertEquals(1, settings.tollsSigma, 0);
            router.route(input, output);
            var rows = rows(output);
            var costs = new MarginalCostOfTolls(1, 15);
            var vehicleType = VehicleUtils.createVehicleType(Id.create("car", VehicleType.class));
            Set<String> chosen = new java.util.HashSet<>();
            for (int i = 0; i < 100; i++) {
                var vehicle = VehicleUtils.createVehicle(Id.createVehicleId("trip-" + i), vehicleType);
                double fastCost = 100 + costs.getMarginalCostOfTolls(vehicle);
                String expected = fastCost < 200 ? "fast1-fast2" : "short1-short2";
                assertEquals(expected, rows.get(i).get("links"));
                chosen.add(expected);
            }
            assertEquals(2, chosen.size());
            byte[] first = Files.readAllBytes(output);
            Files.delete(config);
            Files.delete(network);
            router.route(input, output);
            assertArrayEquals(first, Files.readAllBytes(output));
        }
    }

    private MultiModeRouter.Settings settings(Path config, String... options) throws Exception {
        List<String> args = new ArrayList<>(List.of("--network-path", "unused-network.xml", "--port", "8080"));
        if (config != null) {
            args.addAll(List.of("--config-path", config.toString()));
        }
        args.addAll(List.of(options));
        return MultiModeRouterServer.readSettings(MultiModeRouterServer.commandLine(args.toArray(String[]::new)));
    }

    private Path config(String modules) throws Exception {
        Path path = folder.newFile().toPath();
        Files.writeString(path, "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE config SYSTEM \"http://www.matsim.org/files/dtd/config_v2.dtd\">\n"
                + "<config>" + modules + "</config>");
        return path;
    }

    private Path network() throws Exception {
        Network network = NetworkUtils.createNetwork();
        var factory = network.getFactory();
        Node s = factory.createNode(Id.createNodeId("s"), new Coord(0, 0));
        Node m = factory.createNode(Id.createNodeId("m"), new Coord(1000, 0));
        Node n = factory.createNode(Id.createNodeId("n"), new Coord(1000, 1000));
        Node d = factory.createNode(Id.createNodeId("d"), new Coord(2000, 0));
        List.of(s, m, n, d).forEach(network::addNode);
        link(network, "short1", s, m, 1000, 10, 0);
        link(network, "short2", m, d, 1000, 10, 0);
        link(network, "fast1", s, n, 1500, 30, 0.5);
        link(network, "fast2", n, d, 1500, 30, 0.5);
        Path path = folder.newFile().toPath();
        new NetworkWriter(network).write(path.toString());
        return path;
    }

    private static void link(Network network, String id, Node from, Node to, double length, double speed, double toll) {
        var link = network.getFactory().createLink(Id.createLinkId(id), from, to);
        link.setAllowedModes(Set.of("car", "bike", "walk"));
        link.setLength(length);
        link.setFreespeed(speed);
        link.setCapacity(3600);
        link.setNumberOfLanes(1);
        link.getAttributes().putAttribute("toll", toll);
        network.addLink(link);
    }

    private static List<Map<String, String>> rows(Path path) throws Exception {
        CsvMapper mapper = new CsvMapper();
        try (MappingIterator<Map<String, String>> iterator = mapper.readerFor(
                mapper.getTypeFactory().constructMapType(Map.class, String.class, String.class))
                .with(CsvSchema.emptySchema().withHeader()).readValues(path.toFile())) {
            return iterator.readAll();
        }
    }
}
