package org.eqasim.core.components.routing.speedy;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import static org.junit.Assert.*;

public class TestMemoryEfficientSpeedyALT {
    @Test public void thirtyTwoLandmarksSharePreprocessingAndMatchPinnedMatsim() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new MemoryEfficientSpeedyALTFactory(0));
        assertThrows(IllegalArgumentException.class, () -> new MemoryEfficientSpeedyALTFactory(-1));
        for (boolean restrictions : new boolean[]{false, true}) {
            var network = grid(8, restrictions, 0);
            var cost = costs(0.042);
            var factory16 = new MemoryEfficientSpeedyALTFactory();
            factory16.createPathCalculator(network, cost, TIMES);
            var factory32 = new MemoryEfficientSpeedyALTFactory(32);
            var router32 = factory32.createPathCalculator(network, cost, TIMES);
            var otherWorker = factory32.createPathCalculator(network, cost, TIMES);
            assertNotSame(router32, otherWorker);
            assertEquals(16, factory16.getStatistics().get(0).landmarksPerSet());
            var statistics = factory32.getStatistics().get(0);
            assertEquals(32, statistics.landmarksPerSet());
            assertEquals(1, statistics.landmarkSets());
            assertEquals(16L * 32 * statistics.routingNodes(), statistics.uncompressedLandmarkBytes());
            // Compare the adaptation with the pinned MATSim algorithm at the same
            // count. Different counts can change restricted-network search results.
            var reference16 = new org.matsim.core.router.speedy.SpeedyALTFactory()
                    .createPathCalculator(network, cost, TIMES);
            Object referenceData = field(reference16, "astarData");
            Object referenceGraph = field(referenceData, "graph");
            var dataConstructor = referenceData.getClass().getDeclaredConstructor(
                    referenceGraph.getClass(), int.class, TravelDisutility.class, int.class);
            dataConstructor.setAccessible(true);
            Object data32 = dataConstructor.newInstance(referenceGraph, 32, cost, 1);
            var routerConstructor = reference16.getClass().getDeclaredConstructor(
                    referenceData.getClass(), TravelTime.class, TravelDisutility.class);
            routerConstructor.setAccessible(true);
            var reference32 = (LeastCostPathCalculator) routerConstructor.newInstance(data32, TIMES, cost);
            assertLandmarksEqual(reference32, router32);
            var nodes = new ArrayList<>(network.getNodes().values());
            var random = new Random(851);
            for (int i = 0; i < 200; i++) {
                var from = nodes.get(random.nextInt(nodes.size()));
                var to = nodes.get(random.nextInt(nodes.size()));
                var expected = reference32.calcLeastCostPath(from, to, 18000, null, null);
                var actual = router32.calcLeastCostPath(from, to, 18000, null, null);
                var repeated = otherWorker.calcLeastCostPath(from, to, 18000, null, null);
                same(expected, actual);
                if (expected != null) {
                    assertEquals(expected.travelCost, actual.travelCost, 1e-8);
                    assertEquals(expected.travelTime, actual.travelTime, 1e-8);
                    assertEquals(actual.travelCost, repeated.travelCost, 0);
                    assertEquals(actual.travelTime, repeated.travelTime, 0);
                }
            }
        }
    }

    private static final TravelTime TIMES = (link, time, person, vehicle) ->
            link.getLength() / link.getFreespeed() * (1 + ((int) (time / 900) % 3) * 0.2);

    private static TravelDisutility costs(double distanceWeight) {
        return new TravelDisutility() {
            public double getLinkTravelDisutility(Link link, double time,
                    org.matsim.api.core.v01.population.Person person, org.matsim.vehicles.Vehicle vehicle) {
                return TIMES.getLinkTravelTime(link, time, person, vehicle) + distanceWeight * link.getLength();
            }
            public double getLinkMinimumTravelDisutility(Link link) {
                return link.getLength() / link.getFreespeed() + distanceWeight * link.getLength();
            }
        };
    }

    @Test public void growingQueuePreservesOriginalOrderingAcrossResizesAndReuse() throws Exception {
        Class<?> type = Class.forName("org.matsim.core.router.speedy.DAryMinHeap");
        var constructor = type.getDeclaredConstructor(int.class, int.class); constructor.setAccessible(true);
        Object original = constructor.newInstance(5000, 6);
        var insert = type.getDeclaredMethod("insert", int.class, double.class); insert.setAccessible(true);
        var decrease = type.getDeclaredMethod("decreaseKey", int.class, double.class); decrease.setAccessible(true);
        var poll = type.getDeclaredMethod("poll"); poll.setAccessible(true);
        var optimized = new DAryMinHeap(5000, 6);
        var random = new Random(91);
        for (int round = 0; round < 3; round++) {
            for (int node = 0; node < 5000; node++) {
                double priority = 100 + random.nextInt(100); // Many exact ties.
                insert.invoke(original, node, priority); optimized.insert(node, priority);
            }
            for (int node = 0; node < 5000; node += 7) {
                decrease.invoke(original, node, 50.0); optimized.decreaseKey(node, 50.0);
            }
            for (int node = 0; node < 5000; node++) assertEquals(poll.invoke(original), optimized.poll());
            assertTrue(optimized.isEmpty());
        }
    }

    @Test public void exactPathsTimesCostsAndLandmarksWithAndWithoutRestrictions() throws Exception {
        for (boolean restrictions : new boolean[]{false, true}) {
            // Allocate unrelated IDs deliberately: the original graph has holes.
            for (int i = 0; i < 300; i++) {
                Id.createNodeId("unusedNode" + i);
                Id.createLinkId("unusedLink" + i);
            }
            Network network = grid(5, restrictions, 0);
            var cost = costs(0.042);
            var original = new org.matsim.core.router.speedy.SpeedyALTFactory()
                    .createPathCalculator(network, cost, TIMES);
            var factory = new MemoryEfficientSpeedyALTFactory();
            var optimized = factory.createPathCalculator(network, cost, TIMES);
            for (Node from : network.getNodes().values()) for (Node to : network.getNodes().values()) {
                for (double departure : new double[]{18000, 18899, 18900, 79100}) {
                    same(original.calcLeastCostPath(from, to, departure, null, null),
                            optimized.calcLeastCostPath(from, to, departure, null, null));
                }
            }
            var links = new ArrayList<>(network.getLinks().values());
            Random random = new Random(55);
            for (int i = 0; i < 1000; i++) {
                Link from = links.get(random.nextInt(links.size())), to = links.get(random.nextInt(links.size()));
                double time = 18000 + random.nextDouble() * 61200;
                sameLinkQuery(restrictions, from, to, original.calcLeastCostPath(from, to, time, null, null),
                        optimized.calcLeastCostPath(from, to, time, null, null));
            }
            assertLandmarksEqual(original, optimized);
            assertTrue(factory.getStatistics().get(0).routingNodes() < factory.getStatistics().get(0).legacyNodeSlots());
            assertTrue(searchArrayBytes(optimized) <= 40L * ((SpeedyGraph) field(optimized, "graph")).nodeCount + 256);
            assertTrue(searchArrayBytes(optimized) < searchArrayBytes(original));
        }
    }

    @Test public void tiesDisconnectedNodesAndTinyGraphsMatch() throws Exception {
        for (int size : new int[]{1, 2, 3}) {
            Network network = grid(size, false, 0);
            var isolated = network.getFactory().createNode(Id.createNodeId("isolated" + size), new Coord(-1, -1));
            network.addNode(isolated);
            var cost = costs(0);
            var original = new org.matsim.core.router.speedy.SpeedyALTFactory().createPathCalculator(network, cost, TIMES);
            var optimized = new MemoryEfficientSpeedyALTFactory().createPathCalculator(network, cost, TIMES);
            for (Node from : network.getNodes().values()) for (Node to : network.getNodes().values()) {
                same(original.calcLeastCostPath(from, to, 18000, null, null),
                        optimized.calcLeastCostPath(from, to, 18000, null, null));
            }
            assertLandmarksEqual(original, optimized);
        }
    }

    @Test public void optionalRealNetworkMatchesPinnedMatsim() throws Exception {
        String input = System.getProperty("speedy.validationNetwork");
        org.junit.Assume.assumeTrue("Optional real-network regression", input != null);
        var full = NetworkUtils.readNetwork(input);
        Network network = NetworkUtils.createNetwork();
        new org.matsim.core.network.algorithms.TransportModeNetworkFilter(full).filter(network, Set.of("car"));
        var cost = costs(0.042);
        var original = new org.matsim.core.router.speedy.SpeedyALTFactory().createPathCalculator(network, cost, TIMES);
        var optimized = new MemoryEfficientSpeedyALTFactory().createPathCalculator(network, cost, TIMES);
        assertLandmarksEqual(original, optimized);
        var nodes = new ArrayList<>(network.getNodes().values());
        var links = new ArrayList<>(network.getLinks().values());
        var random = new Random(518745);
        for (int i = 0; i < 1000; i++) {
            double time = 18000 + random.nextDouble() * 61200;
            Node from = nodes.get(random.nextInt(nodes.size())), to = nodes.get(random.nextInt(nodes.size()));
            same(original.calcLeastCostPath(from, to, time, null, null),
                    optimized.calcLeastCostPath(from, to, time, null, null));
            Link first = links.get(random.nextInt(links.size())), last = links.get(random.nextInt(links.size()));
            sameLinkQuery(NetworkUtils.hasTurnRestrictions(network), first, last, original.calcLeastCostPath(first, last, time, null, null),
                    optimized.calcLeastCostPath(first, last, time, null, null));
        }
    }

    @Test public void sparseAndDenseQueriesHaveIdenticalPathsBeforeAndAfterAnUnreachableTrip() {
        Network network = grid(20, false, 222);
        Node isolated = network.getFactory().createNode(Id.createNodeId("storage-isolated"), new Coord(-1, -1));
        network.addNode(isolated);
        Node from = network.getNodes().values().iterator().next();
        Node near = from.getOutLinks().values().iterator().next().getToNode();
        var cost = costs(0.042);
        var reference = new org.matsim.core.router.speedy.SpeedyALTFactory().createPathCalculator(network, cost, TIMES);
        var optimized = (SpeedyALT) new MemoryEfficientSpeedyALTFactory().createPathCalculator(network, cost, TIMES);
        same(reference.calcLeastCostPath(from, near, 18000, null, null),
                optimized.calcLeastCostPath(from, near, 18000, null, null));
        assertFalse(optimized.getSearchStatistics().dense());
        same(reference.calcLeastCostPath(from, isolated, 18000, null, null),
                optimized.calcLeastCostPath(from, isolated, 18000, null, null));
        assertTrue(optimized.getSearchStatistics().maxVisited() >= 400);
        same(reference.calcLeastCostPath(from, near, 18900, null, null),
                optimized.calcLeastCostPath(from, near, 18900, null, null));
        assertTrue(optimized.getSearchStatistics().dense());
        assertEquals(3, optimized.getSearchStatistics().queries());
    }

    @Test public void differentLowerBoundsGetSeparateLandmarksAndConcurrentRequestsShareCompatibleData() throws Exception {
        Network network = grid(4, true, 9);
        var factory = new MemoryEfficientSpeedyALTFactory();
        var executor = Executors.newFixedThreadPool(8);
        try {
            var jobs = new ArrayList<java.util.concurrent.Callable<LeastCostPathCalculator>>();
            for (int i = 0; i < 16; i++) jobs.add(() -> factory.createPathCalculator(network, costs(0.042), TIMES));
            var routers = executor.invokeAll(jobs);
            Object shared = field(routers.get(0).get(), "astarData");
            for (var router : routers) assertSame(shared, field(router.get(), "astarData"));
            assertNotSame(routers.get(0).get(), routers.get(1).get());
            var other = factory.createPathCalculator(network, costs(0.5), TIMES);
            assertNotSame(shared, field(other, "astarData"));
            assertEquals(2, factory.getStatistics().get(0).landmarkSets());
            var reference = new org.matsim.core.router.speedy.SpeedyALTFactory().createPathCalculator(network, costs(0.5), TIMES);
            assertLandmarksEqual(reference, other);
        } finally { executor.shutdownNow(); }
    }

    static Network grid(int size, boolean restricted, int seed) {
        var network = NetworkUtils.createNetwork();
        var factory = network.getFactory();
        var nodes = new Node[size][size];
        for (int x = 0; x < size; x++) for (int y = 0; y < size; y++) {
            nodes[x][y] = factory.createNode(Id.createNodeId("n" + seed + ":" + x + ":" + y), new Coord(x * 100, y * 100));
            network.addNode(nodes[x][y]);
        }
        for (int x = 0; x < size; x++) for (int y = 0; y < size; y++) {
            if (x + 1 < size) { add(network, nodes[x][y], nodes[x+1][y]); add(network, nodes[x+1][y], nodes[x][y]); }
            if (y + 1 < size) { add(network, nodes[x][y], nodes[x][y+1]); add(network, nodes[x][y+1], nodes[x][y]); }
        }
        if (restricted) for (int x = 0; x + 1 < size; x++) for (int y = 0; y + 1 < size; y++) {
            Link start = network.getLinks().get(linkId(nodes[x][y], nodes[x+1][y]));
            var restriction = new DisallowedNextLinks();
            restriction.addDisallowedLinkSequence("car", List.of(linkId(nodes[x+1][y], nodes[x+1][y+1])));
            if (y + 2 < size) restriction.addDisallowedLinkSequence("car", List.of(
                    linkId(nodes[x+1][y], nodes[x][y]), linkId(nodes[x][y], nodes[x][y+1]), linkId(nodes[x][y+1], nodes[x][y+2])));
            NetworkUtils.setDisallowedNextLinks(start, restriction);
        }
        return network;
    }

    private static Id<Link> linkId(Node from, Node to) { return Id.createLinkId(from.getId() + "->" + to.getId()); }
    private static void add(Network network, Node from, Node to) {
        var link = network.getFactory().createLink(linkId(from, to), from, to);
        link.setLength(100); link.setFreespeed(10); link.setCapacity(1000); link.setNumberOfLanes(1);
        link.setAllowedModes(Set.of("car")); network.addLink(link);
    }

    private static void sameLinkQuery(boolean restricted, Link first, Link last,
            LeastCostPathCalculator.Path before, LeastCostPathCalculator.Path after) {
        // Exact parity still applies without restrictions. With restrictions,
        // the upstream endpoint heuristic can overestimate colored approaches,
        // and its final turn can be illegal. Check legality and that legal
        // baseline routes are not made more costly; independent Dijkstra tests
        // cover optimality rather than reproducing those upstream defects.
        if (!restricted || before == null) {
            same(before, after);
        } else {
            if (after != null) assertTrue(TestDestinationLinkRestrictions.legal(first, last, after));
            if (TestDestinationLinkRestrictions.legal(first, last, before)) {
                assertNotNull(after);
                assertTrue("from=" + first.getId() + " to=" + last.getId() + " before=" + before.travelCost + " after=" + after.travelCost + " before links=" + before.links + " after links=" + after.links, after.travelCost <= before.travelCost + 1e-8);
            }
        }
    }

    private static void same(LeastCostPathCalculator.Path before, LeastCostPathCalculator.Path after) {
        if (before == null) { assertNull(after); return; }
        assertNotNull(after);
        assertEquals(before.links, after.links);
        assertEquals(before.nodes, after.nodes);
        assertEquals(Double.doubleToLongBits(before.travelTime), Double.doubleToLongBits(after.travelTime));
        assertEquals(Double.doubleToLongBits(before.travelCost), Double.doubleToLongBits(after.travelCost));
    }

    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }

    private static long searchArrayBytes(Object router) throws Exception {
        if (router instanceof SpeedyALT optimized) return optimized.getSearchStatistics().primitiveBytes();
        long bytes = 0;
        for (Field f : router.getClass().getDeclaredFields()) {
            f.setAccessible(true); Object value = f.get(router);
            if (value instanceof int[] a) bytes += 4L * a.length;
            if (value instanceof double[] a) bytes += 8L * a.length;
        }
        Object queue = field(router, "pq");
        for (String name : List.of("heap", "pos")) bytes += 4L * ((int[]) field(queue, name)).length;
        bytes += 8L * ((double[]) field(queue, "cost")).length;
        return bytes;
    }

    private static void assertLandmarksEqual(Object original, Object optimized) throws Exception {
        Object originalData = field(original, "astarData"), optimizedData = field(optimized, "astarData");
        Node[] oldNodes = (Node[]) field(field(original, "graph"), "nodes");
        double[] before = (double[]) field(originalData, "nodesData");
        SpeedyALTData after = (SpeedyALTData) optimizedData;
        int width = 2 * (int) field(originalData, "landmarksCount");
        int dense = 0;
        for (int i = 0; i < oldNodes.length; i++) if (oldNodes[i] != null) {
            for (int j = 0; j < width; j++) assertEquals("landmark state=" + i + " column=" + j,
                    Double.doubleToLongBits(before[i * width + j]), Double.doubleToLongBits((j & 1) == 0
                            ? after.getTravelCostFromLandmark(dense, j / 2) : after.getTravelCostToLandmark(dense, j / 2)));
            dense++;
        }
        assertEquals(((SpeedyGraph) field(optimized, "graph")).nodeCount, dense);
    }
}
