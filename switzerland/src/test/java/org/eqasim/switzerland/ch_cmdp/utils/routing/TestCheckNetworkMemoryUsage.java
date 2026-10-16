package org.eqasim.switzerland.ch_cmdp.utils.routing;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;

import static org.junit.Assert.*;

public class TestCheckNetworkMemoryUsage {
    @Test
    public void completeComparisonRunsInFreshJvmsWithoutChangingInput() throws Exception {
        var directory = java.nio.file.Files.createTempDirectory("routing-memory-test");
        var input = directory.resolve("network.xml");
        try {
            var network = network();
            var previous = network.getNodes().get(Id.createNodeId("b"));
            for (int i = 2; i < 40; i++) {
                var next = NetworkUtils.createAndAddNode(network, Id.createNodeId("node" + i), new Coord(i * 100, 0));
                NetworkUtils.createAndAddLink(network, Id.createLinkId("forward" + i), previous, next, 100, 10, 1000, 1);
                NetworkUtils.createAndAddLink(network, Id.createLinkId("backward" + i), next, previous, 100, 10, 1000, 1);
                previous = next;
            }
            var restrictions = new org.matsim.core.network.turnRestrictions.DisallowedNextLinks();
            restrictions.addDisallowedLinkSequence("car", java.util.List.of(Id.createLinkId("ba")));
            NetworkUtils.setDisallowedNextLinks(network.getLinks().get(Id.createLinkId("ab")), restrictions);
            new org.matsim.core.network.io.NetworkWriter(network).write(input.toString());
            byte[] original = java.nio.file.Files.readAllBytes(input);
            var captured = new java.io.ByteArrayOutputStream();
            var originalOut = System.out;
            try (var output = new java.io.PrintStream(captured, true, java.nio.charset.StandardCharsets.UTF_8)) {
                System.setOut(output);
                CheckNetworkMemoryUsage.main(new String[]{"--network-path", input.toString()});
            } finally {
                System.setOut(originalOut);
            }
            String report = captured.toString(java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(report, report.contains("original-with-restrictions ROUTING"));
            assertTrue(report, report.contains("optimized-with-restrictions ROUTING"));
            assertTrue(report, report.contains("without-restrictions ROUTING"));
            assertTrue(report, report.contains("optimized-32-with-restrictions ROUTING"));
            assertTrue(report, report.contains("landmarksPerSet=16"));
            assertTrue(report, report.contains("landmarksPerSet=32"));
            assertTrue(report, report.contains("router_heap_MiB="));
            assertTrue(report, report.contains("Compact routing graphs:"));
            assertTrue(report, report.contains("Shared float travel-time storage:"));
            assertEquals(report, 2, report.split("Shared float travel-time storage:", -1).length - 1);
            assertTrue(report, report.contains("original-with-restrictions Original MATSim travel-time storage:"));
            assertTrue(report, report.contains("without-restrictions Original MATSim travel-time storage:"));
            assertEquals(report, 4, report.split("threads=4 seed=", -1).length - 1);
            assertTrue(report, report.contains("observationCountBytes=0"));
            assertTrue(report, report.contains("SUMMARY: four-case routing comparison"));
            assertTrue(report, report.contains("Original + turns"));
            assertTrue(report, report.contains("Optimized 16"));
            assertTrue(report, report.contains("Original, no turns"));
            assertTrue(report, report.contains("Optimized 32"));
            assertTrue(report, report.contains("Compact landmark payload (MiB)"));
            assertTrue(report, report.contains("Compared cases: optimized-with-restrictions vs optimized-32-with-restrictions"));
            assertEquals(report, 3, report.split("Travel-time similarity: PASS", -1).length - 1);
            assertTrue(report, report.contains("Process peak resident RAM (MiB)"));
            assertTrue(report, report.contains("32 vs 16"));
            assertTrue(report, report.contains("Successful trips / 100"));
            assertTrue(report, report.contains("Travel times within tolerance: 100/100; reachable in both: 100; unreachable in both: 0"));
            assertFalse(report, report.contains("Exact matches"));
            assertFalse(report, report.contains("Route equivalence"));
            assertTrue(report, report.contains("Absolute travel-time difference (s): mean="));
            assertTrue(report, report.contains("Travel-time similarity: PASS"));
            assertFalse(report, report.contains("BENCHMARK_ROUTE"));
            assertFalse(report, report.contains("Router heap estimate (MiB)                          N/A"));
            var matcher = java.util.regex.Pattern.compile("trip=\\d+ from=\\S+ to=\\S+ departure_s=\\S+").matcher(report);
            var odPairs = new java.util.ArrayList<String>();
            while (matcher.find()) odPairs.add(matcher.group());
            assertEquals(report, 400, odPairs.size());
            assertEquals(odPairs.subList(0, 100), odPairs.subList(100, 200));
            assertEquals(odPairs.subList(0, 100), odPairs.subList(200, 300));
            assertEquals(odPairs.subList(0, 100), odPairs.subList(300, 400));
            assertArrayEquals(original, java.nio.file.Files.readAllBytes(input));
        } finally {
            java.nio.file.Files.deleteIfExists(input);
            java.nio.file.Files.deleteIfExists(directory);
        }
    }

    private static final CheckNetworkMemoryUsage.TravelTimeTolerance TOLERANCE =
            new CheckNetworkMemoryUsage.TravelTimeTolerance(1, 1);

    @Test
    public void similarTravelTimesAcceptDifferentPathsAndCosts() {
        var original = route(true, java.util.List.of("ab", "bc"), java.util.List.of("a", "b", "c"), 20, 30);
        var alternative = route(true, java.util.List.of("ad", "dc"), java.util.List.of("a", "d", "c"), 20.25, 100);
        var comparison = CheckNetworkMemoryUsage.compareRoutes(original, alternative, TOLERANCE);
        assertTrue(comparison.similar());
        assertEquals(0.25, comparison.absoluteDifference(), 0);
        assertEquals(1.25, comparison.relativePercent(), 0);
        assertEquals(1, comparison.allowedDifference(), 0);
        String report = CheckNetworkMemoryUsage.routeComparisonReport(java.util.Map.of(0, original),
                java.util.Map.of(0, alternative), TOLERANCE);
        assertTrue(report, report.contains("Travel-time similarity: PASS"));
        assertFalse(report, report.contains("Exact matches"));
        assertTrue(report, report.contains("mean=0.250000 p95=0.250000 max=0.250000"));
    }

    @Test
    public void toleranceUsesTheLargerOfAbsoluteAndRelativeLimits() {
        assertTrue(compare(20, 21).similar());
        assertFalse(compare(20, 21.001).similar());
        assertTrue(compare(1000, 1010).similar());
        assertFalse(compare(1000, 1011).similar());
        assertTrue(compare(0, 0).similar());
        assertTrue(compare(0, 0.5).similar());
        assertTrue(Double.isNaN(compare(0, 0.5).relativePercent()));
        assertFalse(compare(0, 1.1).similar());
        assertTrue(compare(20, Math.nextUp(20)).similar());
        assertFalse(compare(20, Double.NaN).similar());
        assertFalse(compare(20, Double.POSITIVE_INFINITY).similar());
        assertFalse(compare(Double.NaN, Double.NaN).similar());
        assertFalse(compare(20, -1).similar());
    }

    private static CheckNetworkMemoryUsage.RouteComparison compare(double before, double after) {
        return CheckNetworkMemoryUsage.compareRoutes(route(true, java.util.List.of(), java.util.List.of(), before, 0),
                route(true, java.util.List.of(), java.util.List.of(), after, 0), TOLERANCE);
    }

    @Test
    public void reachabilityAndTripDifferencesCannotPassSimilarity() {
        var unreachable = route(false, java.util.List.of(), java.util.List.of(), Double.NaN, Double.NaN);
        assertTrue(CheckNetworkMemoryUsage.compareRoutes(unreachable, unreachable, TOLERANCE).similar());
        var reachable = route(true, java.util.List.of(), java.util.List.of(), 0, 0);
        assertFalse(CheckNetworkMemoryUsage.compareRoutes(unreachable, reachable, TOLERANCE).similar());
        var differentTrip = new CheckNetworkMemoryUsage.Route(0, "a", "c", Double.doubleToLongBits(18001),
                true, java.util.List.of(), java.util.List.of(), reachable.timeBits(), reachable.costBits());
        assertFalse(CheckNetworkMemoryUsage.compareRoutes(reachable, differentTrip, TOLERANCE).similar());
        String report = CheckNetworkMemoryUsage.routeComparisonReport(java.util.Map.of(0, unreachable),
                java.util.Map.of(0, reachable), TOLERANCE);
        assertTrue(report, report.contains("Reachability differences: 1"));
        assertTrue(report, report.contains("Travel-time similarity: DIFFERENCES FOUND"));
        report = CheckNetworkMemoryUsage.routeComparisonReport(java.util.Map.of(0, unreachable),
                java.util.Map.of(0, unreachable), TOLERANCE);
        assertTrue(report, report.contains("NOT EVALUATED"));
    }

    @Test
    public void toleranceOptionsHaveDefaultsAndRejectInvalidNumbers() throws Exception {
        assertEquals(TOLERANCE, tolerance());
        assertEquals(new CheckNetworkMemoryUsage.TravelTimeTolerance(2, 0.5),
                tolerance("--travel-time-tolerance-seconds", "2", "--travel-time-tolerance-percent", "0.5"));
        for (String invalid : java.util.List.of("-1", "NaN", "Infinity", "invalid")) {
            assertThrows(IllegalArgumentException.class, () -> tolerance("--travel-time-tolerance-seconds", invalid));
            assertThrows(IllegalArgumentException.class, () -> tolerance("--travel-time-tolerance-percent", invalid));
        }
    }

    private static CheckNetworkMemoryUsage.TravelTimeTolerance tolerance(String... args) throws Exception {
        return CheckNetworkMemoryUsage.travelTimeTolerance(new org.matsim.core.config.CommandLine.Builder(args)
                .allowOptions("travel-time-tolerance-seconds", "travel-time-tolerance-percent").build());
    }

    private static CheckNetworkMemoryUsage.Route route(boolean reachable, java.util.List<String> links,
            java.util.List<String> nodes, double time, double cost) {
        return new CheckNetworkMemoryUsage.Route(0, "a", "c", Double.doubleToLongBits(18000), reachable,
                links, nodes, Double.doubleToLongBits(time), Double.doubleToLongBits(cost));
    }

    @Test
    public void linuxProcStatusReportsResidentAverageAndHighWaterMark() throws Exception {
        var status = java.nio.file.Files.createTempFile("proc-status", ".txt");
        try {
            java.nio.file.Files.writeString(status, "Name:\tjava\nVmRSS:\t1024 kB\nVmHWM:\t3072 kB\n");
            assertEquals(Long.valueOf(1048576), CheckNetworkMemoryUsage.parseLinuxStatus(
                    java.nio.file.Files.readString(status)).get("VmRSS"));
            try (var sampler = new CheckNetworkMemoryUsage.LinuxMemorySampler(status)) {
                assertEquals(1.0, sampler.metrics().get("rss_average"), 0.0);
                assertEquals(3.0, sampler.metrics().get("rss_peak"), 0.0);
            }
            java.nio.file.Files.writeString(status, "Name:\tjava\nState:\tZ (zombie)\n");
            try (var sampler = new CheckNetworkMemoryUsage.LinuxMemorySampler(status)) {
                assertTrue(sampler.metrics().isEmpty());
            }
        } finally {
            java.nio.file.Files.deleteIfExists(status);
        }
    }

    @Test
    public void tripsAreReproducibleAndHaveDistinctNodesAndRequestedDepartureWindow() {
        var network = network();
        var trips = CheckNetworkMemoryUsage.selectTrips(network, 518745);
        assertEquals(100, trips.size());
        assertEquals(trips, CheckNetworkMemoryUsage.selectTrips(network, 518745));
        for (var trip : trips) {
            assertNotEquals(trip.from(), trip.to());
            assertTrue(trip.departure() >= 5 * 3600);
            assertTrue(trip.departure() < 22 * 3600);
        }
    }

    @Test
    public void floatCollectorIsPopulatedInEveryBinIncludingOverflowAndReleasesCounts() {
        var network = network();
        var config = ConfigUtils.createConfig();
        config.travelTimeCalculator().setTraveltimeBinSize(900);
        config.travelTimeCalculator().setMaxTime(108000);
        var collector = CheckNetworkMemoryUsage.buildTravelTimes(network, config);
        var travelTime = collector.getLinkTravelTimes();
        assertEquals(0, collector.getStorageStatistics().observationCountBytes());
        assertEquals(2L * 121 * Float.BYTES, collector.getStorageStatistics().valueBytes());
        assertSame(travelTime, collector.getLinkTravelTimes());
        // Faster free flow after population distinguishes stored observations
        // from the collector's unobserved-bin free-flow fallback.
        for (var link : network.getLinks().values()) {
            link.setFreespeed(20);
            for (int bin = 0; bin <= 120; bin++) {
                assertEquals(10, travelTime.getLinkTravelTime(link, (bin + 0.5) * 900, null, null), 1e-9);
            }
        }
    }

    @Test
    public void originalCollectorKeepsOriginalPrecisionAndBothUseIdenticalObservations() {
        var network = network();
        network.getLinks().values().forEach(link -> link.setFreespeed(3));
        var config = ConfigUtils.createConfig();
        config.travelTimeCalculator().setTraveltimeBinSize(900);
        config.travelTimeCalculator().setMaxTime(108000);
        var original = CheckNetworkMemoryUsage.buildOriginalTravelTimes(network, config).getLinkTravelTimes();
        var floats = CheckNetworkMemoryUsage.buildTravelTimes(network, config).getLinkTravelTimes();
        network.getLinks().values().forEach(link -> link.setFreespeed(20));
        for (var link : network.getLinks().values()) {
            for (int bin = 0; bin <= 120; bin++) {
                double departure = (bin + 0.5) * 900;
                double before = original.getLinkTravelTime(link, departure, null, null);
                double after = floats.getLinkTravelTime(link, departure, null, null);
                assertEquals(33.333, before, 0);
                assertEquals((double) (float) (100.0 / 3), after, 0);
                assertNotEquals(before, after, 0);
            }
        }
    }

    static Network network() {
        var network = NetworkUtils.createNetwork();
        var factory = network.getFactory();
        var a = factory.createNode(Id.createNodeId("a"), new Coord(0, 0));
        var b = factory.createNode(Id.createNodeId("b"), new Coord(100, 0));
        network.addNode(a);
        network.addNode(b);
        var ab = factory.createLink(Id.createLinkId("ab"), a, b);
        var ba = factory.createLink(Id.createLinkId("ba"), b, a);
        for (var link : java.util.List.of(ab, ba)) {
            link.setLength(100);
            link.setFreespeed(10);
            link.setCapacity(1000);
            link.setNumberOfLanes(1);
            network.addLink(link);
        }
        return network;
    }
}
