package org.eqasim.core.components.traffic_light.delays;

import java.util.Set;
import java.util.List;
import org.eqasim.core.components.flow.FlowDataSet;
import org.eqasim.core.components.traffic_light.TimeBinManager;
import org.eqasim.core.components.traffic_light.delays.shahpar.ShahparConfigGroup;
import org.eqasim.core.components.traffic_light.delays.shahpar.ShahparDelay;
import org.eqasim.core.components.traffic_light.delays.webster.WebsterConfigGroup;
import org.eqasim.core.components.traffic_light.delays.webster.WebsterDelay;
import org.eqasim.core.components.traffic_light.delays.webster.WebsterFormula;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TestRoadIntersectionDelays {
    private static final double TIME = 1800.0;
    private final Network network = NetworkUtils.createNetwork();
    private final Node intersection = addNode("intersection", 0.0, 0.0);
    private final Link car = addApproach("car", -100.0, 0.0, "car");
    private final Link bus = addApproach("bus", 0.0, -100.0, "bus");
    private final Link rail = addApproach("rail", 100.0, 0.0, "rail", "pt");
    private final FlowDataSet flow = mock(FlowDataSet.class);
    private final TimeBinManager bins = mock(TimeBinManager.class);

    public TestRoadIntersectionDelays() {
        Node exit = addNode("exit", 0.0, 100.0);
        Link outgoing = network.getFactory().createLink(Id.createLinkId("out"), intersection, exit);
        outgoing.setAllowedModes(Set.of("car", "bus"));
        outgoing.getAttributes().putAttribute(TrafficLightDelay.TL_ATTRIBUTE, false);
        network.addLink(outgoing);
        when(bins.getBinSize()).thenReturn(3600.0);
        when(bins.getNumberOfBins()).thenReturn(1);
        when(bins.getNumberOfTlBins()).thenReturn(1);
        when(bins.getBinsCenters()).thenReturn(new double[]{TIME});
        when(bins.getTlBinsCenters()).thenReturn(new double[]{TIME});
        when(flow.getFlow_v_h(car.getId(), TIME, 3600.0)).thenReturn(100.0F);
        when(flow.getFlow_v_h(bus.getId(), TIME, 3600.0)).thenReturn(100.0F);
    }

    @Test
    public void unsignalizedIncludesBusGeometryAndFlowButExcludesRail() {
        ShahparDelay delays = new ShahparDelay(network, flow, bins, new ShahparConfigGroup(), 1.0);
        delays.initDelays();
        assertEquals(3.0, delays.getNodeDegree(intersection.getId()), 0.0);
        assertTrue(delays.considerLink(bus));
        assertFalse(delays.considerLink(rail));
        delays.resetDelays();
        double initialCarDelay = delays.getDelay(car, TIME);
        assertTrue(initialCarDelay > 0.0);
        assertTrue(delays.getDelay(bus, TIME) > 0.0);
        when(flow.getFlow_v_h(bus.getId(), TIME, 3600.0)).thenReturn(600.0F);
        delays.resetDelays();
        assertTrue(delays.getDelay(car, TIME) > initialCarDelay);
        verify(flow, never()).getFlow_v_h(rail.getId(), TIME, 3600.0);
    }

    @Test
    public void carRestrictionDoesNotRemoveATurnStillAllowedForBuses() {
        car.setAllowedModes(Set.of("car", "bus"));
        Node exit = addNode("secondExit", 100.0, 100.0);
        Link outgoing = network.getFactory().createLink(Id.createLinkId("secondOut"), intersection, exit);
        outgoing.setAllowedModes(Set.of("car", "bus"));
        network.addLink(outgoing);
        double unrestricted = computeCarDelay();
        DisallowedNextLinks restrictions = mock(DisallowedNextLinks.class);
        car.getAttributes().putAttribute("disallowedNextLinks", restrictions);
        when(restrictions.getDisallowedLinkSequences("car")).thenReturn(List.of(List.of(outgoing.getId())));
        assertEquals(unrestricted, computeCarDelay(), 0.0);
        when(restrictions.getDisallowedLinkSequences("bus")).thenReturn(List.of(List.of(outgoing.getId())));
        assertTrue(computeCarDelay() < unrestricted);
    }

    private double computeCarDelay() {
        ShahparDelay delays = new ShahparDelay(network, flow, bins, new ShahparConfigGroup(), 1.0);
        delays.initDelays();
        return delays.computeDelay(car, TIME);
    }

    @Test
    public void signalizedIncludesBusFlowAndDelaysButExcludesRail() {
        WebsterDelay delays = new WebsterDelay(network, bins,
                new WebsterFormula(new WebsterConfigGroup()), flow, 1.0);
        delays.initDelays();
        delays.resetDelays();
        assertTrue(delays.getDelay(car, TIME) > 0.0);
        assertTrue(delays.getDelay(bus, TIME) > 0.0);
        assertEquals(TrafficLightDelay.INCORRECT_DELAY, delays.getDelay(rail, TIME), 0.0);
        verify(flow).getFlow_v_h(car.getId(), TIME, 3600.0);
        verify(flow).getFlow_v_h(bus.getId(), TIME, 3600.0);
        verify(flow, never()).getFlow_v_h(rail.getId(), TIME, 3600.0);
    }

    private Node addNode(String id, double x, double y) {
        Node node = network.getFactory().createNode(Id.createNodeId(id), new Coord(x, y));
        network.addNode(node);
        return node;
    }

    private Link addApproach(String id, double x, double y, String... modes) {
        Link link = network.getFactory().createLink(Id.createLinkId(id), addNode(id, x, y), intersection);
        link.setAllowedModes(Set.of(modes));
        link.setCapacity(1000.0);
        link.setNumberOfLanes(1.0);
        link.getAttributes().putAttribute(TrafficLightDelay.TL_ATTRIBUTE, true);
        network.addLink(link);
        return link;
    }
}
