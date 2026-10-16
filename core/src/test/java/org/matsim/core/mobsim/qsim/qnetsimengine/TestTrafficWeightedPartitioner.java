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
