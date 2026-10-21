package org.eqasim.switzerland.ch_cmdp.utils.routing;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

public class TestMarouterAssignment {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private static final String HEADER = "identifier,origin_x,origin_y,destination_x,destination_y,departure_time,mode\n";

    @Test
    public void assignmentDivertsTrafficSharesCarTruckFlowAndReportsFinalTimesInInputOrder() throws Exception {
        Path network = writeNetwork(parallelRoads());
        Path config = config(vdf(1, 2), 1, true);
        StringBuilder input = new StringBuilder(HEADER);
        for (int i = 0; i < 20; i++) input.append("passenger-").append(i).append(",0,0,2000,0,0,car_passenger\n");
        for (int i = 0; i < 100; i++) input.append("car-").append(i).append(",0,0,2000,0,0,car\n");
        for (int i = 0; i < 50; i++) input.append("truck-").append(i).append(",0,0,2000,0,0,truck\n");
        input.append("next-bin,0,0,2000,0,3600,car\nwalk,0,0,2000,0,0,walk\n"
                + "bike,0,0,2000,0,0,bike\nunreachable,2000,0,0,0,0,car\n");
        Path trips = write(input.toString());
        Path output = folder.getRoot().toPath().resolve("assigned.csv");
        var settings = settings(network, config, "--marouter", "true", "--compute-cost", "true");
        try (var router = new MultiModeRouter(settings)) {
            assertTrue(router.isMarouterEnabled());
            assertTrue(router.getAvailableModes().contains("truck"));
            assertEquals(174, router.route(trips, output));
            var rows = rows(output);
            double fastPcu = 0, slowPcu = 0;
            for (int i = 20; i < 170; i++) {
                var row = rows.get(i);
                assertEquals(i < 120 ? "car-" + (i - 20) : "truck-" + (i - 120), row.get("identifier"));
                double pcu = row.get("mode").equals("truck") ? 2 : 1;
                if (row.get("links").equals("fast1-fast2")) fastPcu += pcu;
                else { assertEquals("slow1-slow2", row.get("links")); slowPcu += pcu; }
            }
            assertEquals(200, fastPcu + slowPcu, 0);
            assertTrue(fastPcu > 0 && slowPcu > 0);
            double fastTime = 100 * (1 + Math.pow(fastPcu / 20, 2));
            double slowTime = 150 * (1 + Math.pow(slowPcu / 1000, 2));
            for (int i = 0; i < 170; i++) {
                var row = rows.get(i);
                assertEquals(row.get("links").equals("fast1-fast2") ? fastTime : slowTime,
                        Double.parseDouble(row.get("travel_time")), 1e-6);
                if (i < 20) assertEquals(0, Double.parseDouble(row.get("cost")), 0);
                else if (i >= 120) assertEquals("NaN", row.get("cost"));
            }
            assertEquals(100.25, Double.parseDouble(rows.get(170).get("travel_time")), 1e-6);
            assertEquals(0, Double.parseDouble(rows.get(171).get("cost")), 0);
            assertEquals(0, Double.parseDouble(rows.get(172).get("cost")), 0);
            assertEquals("NaN", rows.get(173).get("travel_time"));
            byte[] first = Files.readAllBytes(output);
            router.route(trips, output);
            assertArrayEquals(first, Files.readAllBytes(output)); // Request-local flows and seeded randomness.
        }
        assertNoSpools();
    }

    @Test
    public void resultsDoNotDependOnWorkerCountOrCsvChunkBoundaries() throws Exception {
        Path network = writeNetwork(parallelRoads());
        Path config = config(vdf(1, 2), 1, true);
        StringBuilder input = new StringBuilder(HEADER);
        for (int i = 0; i < 80; i++) input.append(i).append(",0,0,2000,0,0,")
                .append(i % 3 == 0 ? "truck" : "car").append('\n');
        Path trips = write(input.toString());
        Path a = folder.getRoot().toPath().resolve("a.csv");
        Path b = folder.getRoot().toPath().resolve("b.csv");
        try (var router = new MultiModeRouter(settings(network, config, "--marouter", "true", "--threads", "1", "--chunk-size", "7"))) {
            router.route(trips, a);
        }
        try (var router = new MultiModeRouter(settings(network, config, "--marouter", "true", "--threads", "4", "--chunk-size", "31"))) {
            router.route(trips, b);
        }
        assertArrayEquals(Files.readAllBytes(a), Files.readAllBytes(b));
        assertNoSpools();
    }

    @Test
    public void reusesVdfCapacityPeriodSampleScalingAndSpeedFloorWithoutInflatingPartialDemand() {
        Network network = parallelRoads();
        network.setCapacityPeriod(1800);
        VDFConfigGroup config = vdf(1, 2);
        config.setInterval(900);
        config.setCapacityFactor(2);
        config.setMinimumSpeed(4);
        var assignment = new MarouterAssignment(network, config, 0.5, 0.05, 4711, 2, 100);
        int[] fast = {Id.createLinkId("fast1").index(), Id.createLinkId("fast2").index()};
        assignment.begin(0);
        assertEquals(100, assignment.finalTime(fast, false), 0);
        assignment.add(fast, true); // 2 PCU; sampled capacity = 0.5 * 2 * 900/1800 * 20 = 10.
        assignment.update();
        assertEquals(104, assignment.finalTime(fast, false), 1e-9);
        for (int i = 0; i < 1000; i++) assignment.add(fast, true);
        assignment.update();
        assertEquals(500, assignment.finalTime(fast, false), 0); // Two 1000m links at minimum 4m/s.
        assignment.begin(1);
        assertEquals(100, assignment.finalTime(fast, false), 0);
        assignment.reset();
        assertEquals(50, assignment.travelTime(false).getLinkTravelTime(network.getLinks().get(Id.createLinkId("fast1")), 0, null, null), 0);
    }

    @Test
    public void configFallbackCliOverridesAndValidationUseVdfSettings() throws Exception {
        Path network = writeNetwork(parallelRoads());
        VDFConfigGroup vdf = vdf(1, 2);
        vdf.setInterval(1800);
        Path config = config(vdf, 0.5, true);
        var inherited = settings(network, config);
        inherited.applyConfigDefaults(MultiModeRouter.loadConfig(inherited));
        assertTrue(inherited.marouter);
        assertEquals(123, inherited.marouterSeed);
        var disabled = settings(network, config, "--marouter", "false");
        disabled.applyConfigDefaults(MultiModeRouter.loadConfig(disabled));
        assertFalse(disabled.marouter);
        Path trips = write(HEADER + "one,0,0,2000,0,0,car\n");
        Path output = folder.getRoot().toPath().resolve("capacity.csv");
        try (var router = new MultiModeRouter(settings(network, config))) {
            router.route(trips, output);
            // Flow 1; sampled half-hour capacity 0.5*10=5, so the two-link path is 100*(1+.2^2).
            assertEquals(104, Double.parseDouble(rows(output).get(0).get("travel_time")), 1e-6);
        }
        try (var router = new MultiModeRouter(settings(network, config, "--interval", "3600", "--marouter-sample-size", "1"))) {
            router.route(trips, output);
            assertEquals(100.25, Double.parseDouble(rows(output).get(0).get("travel_time")), 1e-6);
        }
        for (String[] args : List.of(new String[]{"--marouter-increment", "0"},
                new String[]{"--marouter-sample-size", "0"}, new String[]{"--marouter-truck-pcu", "-1"})) {
            var invalid = settings(network, config, args);
            assertThrows(IllegalArgumentException.class, () -> new MultiModeRouter(invalid));
        }
        try (var router = new MultiModeRouter(settings(network, config, "--events-path", "missing-events.xml"))) {
            router.route(trips, output);
            assertEquals(104, Double.parseDouble(rows(output).get(0).get("travel_time")), 1e-6);
        }
    }

    @Test
    public void tenPercentSampleReducesCapacityOnceAndMatchesFullPopulationCongestion() throws Exception {
        Network network = parallelRoads();
        network.setCapacityPeriod(1800);
        for (var link : network.getLinks().values()) link.setCapacity(100);
        var vdf = vdf(1, 2); vdf.setInterval(900); vdf.setCapacityFactor(2);
        int[] fast = {Id.createLinkId("fast1").index(), Id.createLinkId("fast2").index()};
        var sampled = new MarouterAssignment(network, vdf, 0.1, 0.05, 4711, 2, 100);
        var full = new MarouterAssignment(network, vdf, 1, 0.05, 4711, 2, 100);
        sampled.begin(0); full.begin(0);
        sampled.add(fast, false); sampled.add(fast, false); sampled.add(fast, true); // 4 PCU.
        for (int i = 0; i < 20; i++) full.add(fast, false);
        for (int i = 0; i < 10; i++) full.add(fast, true); // 40 PCU.
        sampled.update(); full.update();
        // Full interval capacity = 2 * 900/1800 * 100 = 100. Sampled capacity = 10.
        assertEquals(100 * (1 + Math.pow(4.0 / 10, 2)), sampled.finalTime(fast, false), 1e-9);
        assertEquals(full.finalTime(fast, false), sampled.finalTime(fast, false), 1e-9);

        Path networkPath = writeNetwork(parallelRoads());
        Path config = config(vdf(1, 2), 0.1, true);
        Path input = write(HEADER + "car,0,0,2000,0,0,car\n");
        Path output = folder.getRoot().toPath().resolve("ten-percent.csv");
        try (var router = new MultiModeRouter(settings(networkPath, config))) {
            router.route(input, output);
            assertEquals(125, Double.parseDouble(rows(output).getFirst().get("travel_time")), 1e-9);
        }
        try (var router = new MultiModeRouter(settings(networkPath, config, "--marouter-sample-size", "1"))) {
            router.route(input, output);
            assertEquals(100.25, Double.parseDouble(rows(output).getFirst().get("travel_time")), 1e-9);
        }
    }

    @Test
    public void trucksRespectAllowedLinksAndSpeedCeilingAndFailedRequestsKeepOutput() throws Exception {
        Network roads = parallelRoads();
        var shortcut = roads.getFactory().createLink(Id.createLinkId("car-only"), roads.getNodes().get(Id.createNodeId("s")),
                roads.getNodes().get(Id.createNodeId("d")));
        shortcut.setLength(500); shortcut.setFreespeed(20); shortcut.setCapacity(1000); shortcut.setNumberOfLanes(1);
        shortcut.setAllowedModes(Set.of("car")); roads.addLink(shortcut);
        Path network = writeNetwork(roads);
        Path config = config(vdf(0, 2), 1, true);
        Path input = write(HEADER + "car,0,0,2000,0,0,car\ntruck,0,0,2000,0,0,truck\n");
        Path output = folder.getRoot().toPath().resolve("restricted.csv");
        try (var router = new MultiModeRouter(settings(network, config, "--truck-max-speed", "5"))) {
            router.route(input, output);
            var rows = rows(output);
            assertEquals("car-only", rows.get(0).get("links"));
            assertEquals("fast1-fast2", rows.get(1).get("links"));
            assertEquals(400, Double.parseDouble(rows.get(1).get("travel_time")), 0);
            byte[] first = Files.readAllBytes(output);
            Path invalid = write(HEADER + "invalid,NaN,0,2000,0,0,car\n");
            assertThrows(IllegalArgumentException.class, () -> router.route(invalid, output));
            assertArrayEquals(first, Files.readAllBytes(output));
            router.route(input, output);
            assertArrayEquals(first, Files.readAllBytes(output));
        }
        assertNoSpools();
    }

    @Test
    public void manyBinsUseBoundedOpenFilesAndSavedCostRequestsDoNotAssign() throws Exception {
        Path network = writeNetwork(parallelRoads());
        VDFConfigGroup vdf = vdf(1, 2);
        vdf.setInterval(60); vdf.setEndTime(7200);
        Path config = config(vdf, 1, true);
        StringBuilder input = new StringBuilder(HEADER);
        for (int i = 0; i < 80; i++) input.append(i).append(",0,0,2000,0,").append(i * 60).append(",car\n");
        Path trips = write(input.toString());
        Path output = folder.getRoot().toPath().resolve("many-bins.csv");
        try (var router = new MultiModeRouter(settings(network, config, "--compute-cost", "true"))) {
            router.route(trips, output);
            assertEquals(80, rows(output).size());
            Path costs = folder.getRoot().toPath().resolve("costs.csv");
            router.cost(output, costs);
            assertEquals(80, rows(costs).size());
            byte[] savedCosts = Files.readAllBytes(costs);
            router.cost(trips, costs);
            assertArrayEquals(savedCosts, Files.readAllBytes(costs));
            Path empty = write(HEADER);
            assertEquals(0, router.route(empty, output));
            assertTrue(Files.readString(output).contains("travel_time"));
        }
        assertNoSpools();
    }

    @Test
    public void serverLoadsAssignmentInBackgroundAndExposesReadinessBeforeRouting() throws Exception {
        Path network = writeNetwork(parallelRoads());
        Path config = config(vdf(1, 2), 1, true);
        Path input = write(HEADER + "car,0,0,2000,0,0,car\ntruck,0,0,2000,0,0,truck\n");
        Path output = folder.getRoot().toPath().resolve("http.csv");
        var json = new ObjectMapper();
        try (var server = new MultiModeRouterServer(settings(network, config), "127.0.0.1", 0);
                var client = HttpClient.newHttpClient()) {
            server.start();
            String base = "http://127.0.0.1:" + server.getPort();
            long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
            while (true) {
                var ready = client.send(HttpRequest.newBuilder(URI.create(base + "/ready?mode=truck"))
                        .timeout(Duration.ofSeconds(3)).GET().build(), HttpResponse.BodyHandlers.ofString());
                var state = json.readTree(ready.body());
                assertNotEquals(state.toString(), "failed", state.path("status").asText());
                if (ready.statusCode() == 200) {
                    assertTrue(state.path("marouter").asBoolean());
                    assertTrue(state.path("modes").path("car").path("ready").asBoolean());
                    assertTrue(state.path("modes").path("truck").path("ready").asBoolean());
                    break;
                }
                assertEquals(503, ready.statusCode());
                assertTrue("Background initialization timed out", System.nanoTime() < deadline);
                Thread.sleep(10);
            }
            // Readiness guarantees no further data-file loading is needed by a routing request.
            Files.delete(network); Files.delete(config);
            String body = json.writeValueAsString(Map.of("inputCsv", input.toString(), "outputCsv", output.toString()));
            var response = client.send(HttpRequest.newBuilder(URI.create(base + "/route"))
                    .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(response.body(), 200, response.statusCode());
            assertEquals(2, json.readTree(response.body()).path("trips").asInt());
            assertEquals(2, rows(output).size());
        }
        assertNoSpools();
    }

    private VDFConfigGroup vdf(double factor, double exponent) {
        var config = new VDFConfigGroup();
        config.setBprFactor(factor); config.setBprExponent(exponent); config.setInterval(3600);
        config.setStartTime(0); config.setEndTime(7200); config.setMinimumSpeed(1);
        return config;
    }

    @Test
    public void calibratedSpeedsSetFreeflowAndBprBaselinesWhileKeepingTruckSpeedLimit() throws Exception {
        Network roads = parallelRoads();
        roads.getLinks().get(Id.createLinkId("fast1")).getAttributes().putAttribute("speedFactor", 2.0);
        roads.getLinks().get(Id.createLinkId("fast2")).getAttributes().putAttribute("speedFactor", " 2.0 ");
        Path network = writeNetwork(roads);
        Path config = config(vdf(1, 2), 1, false);
        Path input = write(HEADER + "car,0,0,2000,0,0,car\ntruck,0,0,2000,0,0,truck\n"
                + "passenger,0,0,2000,0,0,car_passenger\n");
        Path output = folder.getRoot().toPath().resolve("speed-factors.csv");
        for (boolean assignment : List.of(false, true)) {
            var settings = settings(network, config, "--marouter", String.valueOf(assignment));
            if (assignment) settings.eventsPath = "does-not-exist.xml";
            try (var router = new MultiModeRouter(settings)) {
                router.route(input, output);
                var results = rows(output);
                double carTime = assignment ? 50 * (1 + Math.pow(3.0 / 20, 2)) : 50;
                assertEquals(carTime, Double.parseDouble(results.get(0).get("travel_time")), 1e-9);
                assertEquals(90, Double.parseDouble(results.get(1).get("travel_time")), 1e-9);
                assertEquals(carTime, Double.parseDouble(results.get(2).get("travel_time")), 1e-9);
                byte[] first = Files.readAllBytes(output);
                router.route(input, output);
                assertArrayEquals(first, Files.readAllBytes(output));
            }
        }
        assertNoSpools();
    }

    @Test
    public void speedFactorsChangeRouteChoiceInBothFreeflowAndAssignment() throws Exception {
        Network roads = parallelRoads();
        for (var link : roads.getLinks().values()) {
            link.getAttributes().putAttribute("speedFactor", link.getId().toString().startsWith("fast") ? 0.5 : 2.0);
        }
        Path network = writeNetwork(roads);
        Path config = config(vdf(0, 2), 1, false);
        Path input = write(HEADER + "car,0,0,2000,0,0,car\ntruck,0,0,2000,0,0,truck\n");
        Path output = folder.getRoot().toPath().resolve("speed-factor-choice.csv");
        for (boolean assignment : List.of(false, true)) {
            try (var router = new MultiModeRouter(settings(network, config, "--marouter", String.valueOf(assignment)))) {
                router.route(input, output);
                var results = rows(output);
                assertEquals("slow1-slow2", results.get(0).get("links"));
                assertEquals("slow1-slow2", results.get(1).get("links"));
                assertEquals(75, Double.parseDouble(results.get(0).get("travel_time")), 1e-9);
                assertEquals(135, Double.parseDouble(results.get(1).get("travel_time")), 1e-9);
            }
        }
    }

    @Test
    public void calibratedSpeedFloorAndInvalidAttributeFallbackMatchCalibration() {
        Network network = parallelRoads();
        var link = network.getLinks().get(Id.createLinkId("fast1"));
        for (Object invalid : List.of(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, "invalid")) {
            link.getAttributes().putAttribute("speedFactor", invalid);
            assertEquals(50, new CalibratedFreeSpeedTravelTime(network).getLinkTravelTime(link, 0, null, null), 0);
        }
        link.getAttributes().putAttribute("speedFactor", 0.001);
        assertEquals(10_000, new CalibratedFreeSpeedTravelTime(network).getLinkTravelTime(link, 0, null, null), 0);
        assertEquals(20, link.getFreespeed(), 0); // Calibration never mutates the shared network.
    }

    private Path config(VDFConfigGroup vdf, double sampleSize, boolean enabled) throws Exception {
        Config config = ConfigUtils.createConfig();
        config.global().setRandomSeed(123);
        config.addModule(vdf);
        var eqasim = new EqasimConfigGroup(); eqasim.setSampleSize(sampleSize); config.addModule(eqasim);
        var router = new org.matsim.core.config.ConfigGroup("multiModeRouter");
        router.addParam("marouter", String.valueOf(enabled)); config.addModule(router);
        Path path = folder.newFile().toPath(); new ConfigWriter(config).write(path.toString()); return path;
    }

    private MultiModeRouter.Settings settings(Path network, Path config, String... options) throws Exception {
        List<String> args = new ArrayList<>(List.of("--network-path", network.toString(), "--config-path", config.toString(),
                "--port", "8080", "--threads", "3", "--chunk-size", "7", "--batch-size", "2", "--return-links", "true"));
        // Replace defaults rather than specifying a CLI option twice.
        for (int i = 0; i < options.length; i += 2) {
            int index = args.indexOf(options[i]);
            if (index >= 0) args.set(index + 1, options[i + 1]);
            else args.addAll(List.of(options[i], options[i + 1]));
        }
        return MultiModeRouterServer.readSettings(MultiModeRouterServer.commandLine(args.toArray(String[]::new)));
    }

    private static Network parallelRoads() {
        Network network = NetworkUtils.createNetwork();
        var factory = network.getFactory();
        Node s = factory.createNode(Id.createNodeId("s"), new Coord(0, 0));
        Node a = factory.createNode(Id.createNodeId("a"), new Coord(1000, 100));
        Node b = factory.createNode(Id.createNodeId("b"), new Coord(1000, -100));
        Node d = factory.createNode(Id.createNodeId("d"), new Coord(2000, 0));
        List.of(s, a, b, d).forEach(network::addNode);
        link(network, "fast1", s, a, 1000, 20); link(network, "fast2", a, d, 1000, 20);
        link(network, "slow1", s, b, 1500, 1000); link(network, "slow2", b, d, 1500, 1000);
        return network;
    }

    private static void link(Network network, String id, Node from, Node to, double length, double capacity) {
        var link = network.getFactory().createLink(Id.createLinkId(id), from, to);
        link.setLength(length); link.setCapacity(capacity); link.setFreespeed(20); link.setNumberOfLanes(1);
        link.setAllowedModes(Set.of("car", "truck")); network.addLink(link);
    }

    private Path writeNetwork(Network network) throws Exception {
        Path path = folder.newFile().toPath(); new NetworkWriter(network).write(path.toString()); return path;
    }

    private Path write(String value) throws Exception {
        Path path = folder.newFile().toPath(); Files.writeString(path, value); return path;
    }

    private static List<Map<String, String>> rows(Path path) throws Exception {
        var mapper = new CsvMapper();
        try (MappingIterator<Map<String, String>> iterator = mapper.readerFor(
                mapper.getTypeFactory().constructMapType(Map.class, String.class, String.class))
                .with(CsvSchema.emptySchema().withHeader()).readValues(path.toFile())) {
            return iterator.readAll();
        }
    }

    private void assertNoSpools() throws Exception {
        try (var files = Files.list(folder.getRoot().toPath())) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().startsWith(".marouter-")));
        }
    }
}
