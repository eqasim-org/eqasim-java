package org.eqasim.core.components.routing.speedy;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

public class TestNodeDestinationRestrictions {
    private static final TravelDisutility COST = new TravelDisutility() {
        public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
            return link.getLength();
        }
        public double getLinkMinimumTravelDisutility(Link link) { return link.getLength(); }
    };

    @Test public void nodeGoalInRestrictedTailMayStopBeforeForbiddenContinuation() {
        var network = NetworkUtils.createNetwork();
        Node a = NetworkUtils.createAndAddNode(network, Id.createNodeId("tail-a"), new Coord(0, 0));
        Node b = NetworkUtils.createAndAddNode(network, Id.createNodeId("tail-b"), new Coord(1, 0));
        Node c = NetworkUtils.createAndAddNode(network, Id.createNodeId("tail-c"), new Coord(2, 0));
        Node d = NetworkUtils.createAndAddNode(network, Id.createNodeId("tail-d"), new Coord(3, 0));
        Node x = NetworkUtils.createAndAddNode(network, Id.createNodeId("tail-x"), new Coord(1, 1));
        Link ab = NetworkUtils.createAndAddLink(network, Id.createLinkId("tail-ab"), a, b, 1, 1, 1000, 1);
        Link bc = NetworkUtils.createAndAddLink(network, Id.createLinkId("tail-bc"), b, c, 1, 1, 1000, 1);
        Link cd = NetworkUtils.createAndAddLink(network, Id.createLinkId("tail-cd"), c, d, 1, 1, 1000, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("tail-ax"), a, x, 10, 1, 1000, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("tail-xc"), x, c, 10, 1, 1000, 1);
        NetworkUtils.addDisallowedNextLinks(ab, "car", java.util.List.of(bc.getId(), cd.getId()));
        var router = new MemoryEfficientSpeedyALTFactory(4).createPathCalculator(network, COST,
                (link, time, person, vehicle) -> link.getLength());
        assertEquals(2, router.calcLeastCostPath(a, c, 0, null, null).travelCost, 0);
        assertEquals(21, router.calcLeastCostPath(a, d, 0, null, null).travelCost, 0);
        assertEquals(1, router.calcLeastCostPath(c, d, 0, null, null).travelCost, 0);
        assertEquals(2, router.calcLeastCostPath(a, c, 0, null, null).travelCost, 0);
    }

    @Test public void coloredNodeDestinationsMatchIndependentDijkstraAcrossLandmarkCounts() {
        for (int seed = 0; seed < 3; seed++) {
            var network = TestMemoryEfficientSpeedyALT.grid(6, true, seed + 930);
            var random = new Random(seed + 718);
            for (Link link : network.getLinks().values()) {
                link.setLength(1 + random.nextInt(99));
                link.setFreespeed(1);
            }
            Node isolated = network.getFactory().createNode(Id.createNodeId("node-goal-isolated-" + seed),
                    new Coord(-100, -100));
            network.addNode(isolated);
            var graph = SpeedyGraphBuilder.build(network, null);
            assertTrue(graph.hasTurnRestrictions());
            for (int landmarks : new int[]{4, 16, 32}) {
                var router = new MemoryEfficientSpeedyALTFactory(landmarks).createPathCalculator(network, COST,
                        (link, time, person, vehicle) -> link.getLength());
                for (Node from : network.getNodes().values()) {
                    double[] best = dijkstra(graph, graph.getNodeIndex(from));
                    for (Node to : network.getNodes().values()) {
                        var path = router.calcLeastCostPath(from, to, 0, null, null);
                        double expected = best[graph.getNodeIndex(to)];
                        String context = "seed=" + seed + ", landmarks=" + landmarks
                                + ", from=" + from.getId() + ", to=" + to.getId();
                        if (!Double.isFinite(expected)) {
                            assertNull(context, path);
                        } else {
                            assertNotNull(context, path);
                            assertEquals(context, expected, path.travelCost, 0);
                            assertEquals(context, expected, path.travelTime, 0);
                            assertEquals(context, expected, path.links.stream().mapToDouble(Link::getLength).sum(), 0);
                            assertTrue(context, legal(path));
                        }
                    }
                }
            }
        }
    }

    static boolean legal(org.matsim.core.router.util.LeastCostPathCalculator.Path path) {
        for (int i = 0; i < path.links.size(); i++) {
            Link link = path.links.get(i);
            if (i > 0 && !path.links.get(i - 1).getToNode().getId().equals(link.getFromNode().getId())) return false;
            var restrictions = NetworkUtils.getDisallowedNextLinks(link);
            if (restrictions == null) continue;
            for (var sequence : restrictions.getDisallowedLinkSequences("car")) {
                if (i + sequence.size() >= path.links.size()) continue;
                boolean match = true;
                for (int j = 0; j < sequence.size(); j++) {
                    if (!path.links.get(i + j + 1).getId().equals(sequence.get(j))) { match = false; break; }
                }
                if (match) return false;
            }
        }
        return true;
    }

    /** Independent queue/labels, accepting any colored state at the physical destination. */
    private static double[] dijkstra(SpeedyGraph graph, int start) {
        record Entry(int node, double cost) { }
        var queue = new PriorityQueue<Entry>(Comparator.comparingDouble(Entry::cost));
        double[] best = new double[graph.nodeCount], physical = new double[graph.nodeCount];
        Arrays.fill(best, Double.POSITIVE_INFINITY);
        Arrays.fill(physical, Double.POSITIVE_INFINITY);
        best[start] = 0;
        queue.add(new Entry(start, 0));
        var edges = graph.getOutLinkIterator();
        while (!queue.isEmpty()) {
            var current = queue.remove();
            if (current.cost() != best[current.node()]) continue;
            int original = graph.getOriginalNodeIndex(current.node());
            physical[original] = Math.min(physical[original], current.cost());
            edges.reset(current.node());
            while (edges.next()) {
                int next = edges.getToNodeIndex();
                double cost = current.cost() + graph.getLink(edges.getLinkIndex()).getLength();
                if (cost < best[next]) {
                    best[next] = cost;
                    queue.add(new Entry(next, cost));
                }
            }
        }
        return physical;
    }
}
