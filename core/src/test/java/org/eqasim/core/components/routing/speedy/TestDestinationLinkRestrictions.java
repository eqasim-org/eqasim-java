package org.eqasim.core.components.routing.speedy;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

import static org.junit.Assert.*;

public class TestDestinationLinkRestrictions {
    private static final TravelDisutility COST = new TravelDisutility() {
        public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
            return link.getLength();
        }
        public double getLinkMinimumTravelDisutility(Link link) { return link.getLength(); }
    };

    @Test public void forbiddenFinalUturnUsesLegalAlternative() {
        Network n = border(true, true);
        var path = route(router(n), n, "start", "entry");
        assertEquals(List.of("alternative1", "alternative2"), ids(path));
        assertEquals(20, path.travelCost, 0);
    }

    @Test public void noLegalFinalApproachReturnsNullAndNodeQueriesStillWork() {
        Network n = border(true, false);
        var router = router(n);
        assertNull(route(router, n, "start", "entry"));
        // A node query does not imply entering the activity link afterwards.
        assertEquals(List.of("reverse"), ids(router.calcLeastCostPath(
                n.getNodes().get(Id.createNodeId("B")), n.getNodes().get(Id.createNodeId("C")), 0, null, null)));
        assertNull(route(router, n, "start", "entry"));
    }

    @Test public void adjacentRestrictedStartAndDestinationCannotUseEmptyPath() {
        Network n = border(true, true);
        assertNull(route(router(n), n, "reverse", "entry"));
    }

    @Test public void unrestrictedFinalTurnKeepsTheShortestPath() {
        Network n = border(false, true);
        var path = route(router(n), n, "start", "entry");
        assertEquals(List.of("reverse"), ids(path));
        assertEquals(1, path.travelCost, 0);
    }

    @Test public void multiLinkRestrictionIncludesDestinationLink() {
        Network n = NetworkUtils.createNetwork();
        Node a = node(n, "A", 0, 0), b = node(n, "B", 1, 0), c = node(n, "C", 2, 0);
        Node d = node(n, "D", 3, 0), x = node(n, "X", 1, 1);
        Link start = link(n, "start", a, b, 1), via = link(n, "via", b, c, 1);
        Link destination = link(n, "destination", c, d, 1);
        link(n, "alternative1", b, x, 10); link(n, "alternative2", x, c, 10);
        link(n, "return", d, a, 1);
        NetworkUtils.addDisallowedNextLinks(start, "car", List.of(via.getId(), destination.getId()));
        assertEquals(List.of("alternative1", "alternative2"), ids(route(router(n), n, "start", "destination")));
    }

    @Test public void existingRestrictionsInsideTheRouteRemainEffective() {
        Network n = border(false, true);
        NetworkUtils.addDisallowedNextLinks(n.getLinks().get(Id.createLinkId("start")), "car",
                List.of(Id.createLinkId("reverse")));
        assertEquals(List.of("alternative1", "alternative2"), ids(route(router(n), n, "start", "entry")));
    }

    @Test public void restrictedGridMatchesDijkstraForAllLinkPairs() {
        Network n = TestMemoryEfficientSpeedyALT.grid(5, true, 741);
        var graph = SpeedyGraphBuilder.build(n, "car");
        var scaledCost = new TravelDisutility() {
            public double getLinkTravelDisutility(Link l, double t, Person p, Vehicle v) { return l.getLength() * 0.162; }
            public double getLinkMinimumTravelDisutility(Link l) { return l.getLength() * 0.142; }
        };
        var router = new MemoryEfficientSpeedyALTFactory(16).createPathCalculator(n, scaledCost,
                (link, time, person, vehicle) -> 12);
        for (Link first : n.getLinks().values()) for (Link last : n.getLinks().values()) {
            // A zero-heuristic search independently checks goal handling and
            // optimality, including alternative colored approaches to a link.
            double expected = dijkstra(graph, first, last) * 0.162;
            var actual = router.calcLeastCostPath(first, last, 0, null, null);
            if (Double.isInfinite(expected)) assertNull(actual);
            else {
                assertNotNull(actual);
                assertEquals(first.getId() + " -> " + last.getId(), expected, actual.travelCost, 1e-8);
                assertTrue(legal(first, last, actual));
            }
        }
    }

    static boolean legal(Link first, Link last, LeastCostPathCalculator.Path path) {
        var route = new java.util.ArrayList<Link>();
        route.add(first); route.addAll(path.links); route.add(last);
        for (int i = 0; i < route.size(); i++) {
            if (i > 0 && !route.get(i - 1).getToNode().equals(route.get(i).getFromNode())) return false;
            var restrictions = NetworkUtils.getDisallowedNextLinks(route.get(i));
            if (restrictions == null) continue;
            for (var sequence : restrictions.getDisallowedLinkSequences("car")) {
                if (i + sequence.size() >= route.size()) continue;
                boolean matches = true;
                for (int j = 0; j < sequence.size(); j++)
                    matches &= sequence.get(j).equals(route.get(i + j + 1).getId());
                if (matches) return false;
            }
        }
        return true;
    }

    private static double dijkstra(SpeedyGraph graph, Link first, Link last) {
        return dijkstra(graph, graph.getStartNodeIndex(first), last);
    }

    static double dijkstra(SpeedyGraph graph, int start, Link last) {
        record Entry(int node, double cost) {}
        var queue = new java.util.PriorityQueue<Entry>(java.util.Comparator.comparingDouble(Entry::cost));
        double[] best = new double[graph.nodeCount];
        java.util.Arrays.fill(best, Double.POSITIVE_INFINITY);
        best[start] = 0;
        queue.add(new Entry(start, 0));
        var edges = graph.getOutLinkIterator();
        while (!queue.isEmpty()) {
            var current = queue.remove();
            if (current.cost() != best[current.node()]) continue;
            edges.reset(current.node());
            while (edges.next()) {
                Link link = graph.getLink(edges.getLinkIndex());
                // Accept only an actual outgoing edge into the destination.
                if (link.getId().equals(last.getId())) return current.cost();
                int next = edges.getToNodeIndex();
                double cost = current.cost() + link.getLength();
                if (cost < best[next]) {
                    best[next] = cost;
                    queue.add(new Entry(next, cost));
                }
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    private static Network border(boolean restricted, boolean alternative) {
        Network n = NetworkUtils.createNetwork();
        Node a = node(n, "A", -1, 0), b = node(n, "B", 0, 0), c = node(n, "C", 1, 0);
        link(n, "start", a, b, 1);
        Link reverse = link(n, "reverse", b, c, 1), entry = link(n, "entry", c, b, 1);
        link(n, "return", b, a, 1);
        if (alternative) {
            Node d = node(n, "D", 0, 1);
            link(n, "alternative1", b, d, 10); link(n, "alternative2", d, c, 10);
        }
        if (restricted) NetworkUtils.addDisallowedNextLinks(reverse, "car", List.of(entry.getId()));
        return n;
    }

    private static LeastCostPathCalculator router(Network n) {
        return new MemoryEfficientSpeedyALTFactory(4).createPathCalculator(n, COST,
                (link, time, person, vehicle) -> link.getLength());
    }
    private static LeastCostPathCalculator.Path route(LeastCostPathCalculator router, Network n, String from, String to) {
        return router.calcLeastCostPath(n.getLinks().get(Id.createLinkId(from)),
                n.getLinks().get(Id.createLinkId(to)), 0, null, null);
    }
    private static List<String> ids(LeastCostPathCalculator.Path path) {
        assertNotNull(path);
        return path.links.stream().map(link -> link.getId().toString()).toList();
    }
    private static Node node(Network n, String id, double x, double y) {
        Node node = n.getFactory().createNode(Id.createNodeId(id), new Coord(x, y)); n.addNode(node); return node;
    }
    private static Link link(Network n, String id, Node from, Node to, double length) {
        Link link = n.getFactory().createLink(Id.createLinkId(id), from, to);
        link.setLength(length); link.setFreespeed(1); link.setCapacity(1000); link.setNumberOfLanes(1);
        link.setAllowedModes(Set.of("car")); n.addLink(link); return link;
    }
}
