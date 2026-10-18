package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Arrays;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.population.routes.RouteUtils;
import java.util.List;
import static org.junit.Assert.*;

public class TestTrafficWeightedPartitioner {
    @Test public void measuredWorkChangesNextIterationOnlyAndIgnoresObsoleteFeedback() {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Node[] nodes = new Node[40];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = scenario.getNetwork().getFactory().createNode(Id.createNodeId("feedback" + i), new Coord(i, 0));
            scenario.getNetwork().addNode(nodes[i]);
        }
        var cache = new FastQSimPartitionCache();
        int[] original = cache.get(nodes, scenario, 4);
        int[] preserved = original.clone();
        double[] work = new double[nodes.length];
        Arrays.fill(work, 1);
        Arrays.fill(work, 0, 8, 100);
        cache.recordWork(scenario, original, nodes, work);
        assertArrayEquals(preserved, original);
        int[] next = cache.get(nodes, scenario, 4);
        assertFalse(Arrays.equals(original, next));
        assertEquals(4, Arrays.stream(next).distinct().count());
        assertSame(next, cache.get(nodes, scenario, 4));
        cache.recordWork(scenario, original, nodes, work);
        assertSame(next, cache.get(nodes, scenario, 4));
    }

    @Test public void graphRefinementReducesBoundariesWithoutEmptyOrOverloadedBlocks() {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        var network = scenario.getNetwork();
        Node[] nodes = new Node[40];
        double[] weights = new double[40];
        Arrays.fill(weights, 1);
        int[] parts = new int[40];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = network.getFactory().createNode(Id.createNodeId("graph" + i), new Coord(i, 0));
            network.addNode(nodes[i]); parts[i] = i / 20;
        }
        for (int i = 0; i < 40; i++) for (int j = i + 2; j < 40; j += 2)
            network.addLink(network.getFactory().createLink(Id.createLinkId(i + "-" + j), nodes[i], nodes[j]));
        int before = boundaryCount(nodes, parts);
        TrafficWeightedPartitioner.refine(nodes, weights, parts, 2);
        assertTrue(boundaryCount(nodes, parts) < before);
        long first = Arrays.stream(parts).filter(p -> p == 0).count();
        assertTrue(first >= 17 && first <= 23);
    }

    private static int boundaryCount(Node[] nodes, int[] parts) {
        var indexes = new java.util.IdentityHashMap<Node, Integer>();
        for (int i = 0; i < nodes.length; i++) indexes.put(nodes[i], i);
        int count = 0;
        for (int i = 0; i < nodes.length; i++) for (var link : nodes[i].getOutLinks().values())
            if (parts[i] != parts[indexes.get(link.getToNode())]) count++;
        return count;
    }

    @Test public void heavyTrafficGetsSmallerSpatialPartitionsAndCacheInvalidatesForGeometry() {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        var network = scenario.getNetwork();
        Node[] nodes = new Node[20];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = network.getFactory().createNode(Id.createNodeId("busy" + i), new Coord(i, 0));
            network.addNode(nodes[i]);
            if (i > 0) {
                network.addLink(network.getFactory().createLink(Id.createLinkId("busy" + (i - 1)), nodes[i - 1], nodes[i]));
            }
        }
        var pf = scenario.getPopulation().getFactory();
        for (int i = 0; i < 100; i++) {
            var person = pf.createPerson(Id.createPersonId("busy" + i));
            var plan = pf.createPlan();
            var leg = pf.createLeg("car");
            leg.setRoute(RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("busy0"),
                    List.of(Id.createLinkId("busy1"), Id.createLinkId("busy2"), Id.createLinkId("busy3"), Id.createLinkId("busy4")),
                    Id.createLinkId("busy5")));
            plan.addLeg(leg); person.addPlan(plan); scenario.getPopulation().addPerson(person);
        }
        var cache = new FastQSimPartitionCache();
        var parts = cache.get(nodes, scenario, 2);
        assertNotEquals(parts[0], parts[4]);
        assertEquals(parts[5], parts[19]);
        assertSame(parts, cache.get(nodes, scenario, 2));
        nodes[0].setCoord(new Coord(-10, 0));
        assertNotSame(parts, cache.get(nodes, scenario, 2));
        assertEquals(3, Arrays.stream(cache.get(nodes, scenario, 3)).distinct().count());
    }

    @Test public void partitionsAreDeterministicNonemptyAndHandleCoincidentCoordinates() {
        for (int count : new int[] {1, 3, 11, 100}) {
            var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
            var network = scenario.getNetwork();
            Node[] nodes = new Node[count];
            for (int i = 0; i < count; i++) {
                nodes[i] = network.getFactory().createNode(Id.createNodeId("p" + count + "-" + i),
                        new Coord(i / 7, i % 7));
                network.addNode(nodes[i]);
            }
            for (int parts = 1; parts <= count; parts++) {
                int[] assignment = TrafficWeightedPartitioner.partition(nodes, scenario, parts);
                assertArrayEquals(assignment, TrafficWeightedPartitioner.partition(nodes, scenario, parts));
                assertEquals(parts, Arrays.stream(assignment).distinct().count());
                for (int p : assignment) assertTrue(p >= 0 && p < parts);
            }
        }
    }
}
