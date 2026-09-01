package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.crossborder;

import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficScoringTracker;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestCrossBorderStationDetector {
    @Test
    public void pairsNearbyHighShareLinksAndKeepsUnpairedLinksAsStations() {
        Network network = NetworkUtils.createNetwork();
        List<Id<Link>> nearby = addStation(network, "nearby", 0.0, 0.0);
        List<Id<Link>> far = addStation(network, "far", 1_000.0, 250.0);
        Set<Id<Link>> counted = Set.of(
                nearby.get(0), nearby.get(1), far.get(0), far.get(1));
        TrafficScoringTracker tracker = mock(TrafficScoringTracker.class);
        when(tracker.monitoredLinks()).thenReturn(counted);
        for (Id<Link> linkId : counted) when(tracker.crossBorderShare(linkId)).thenReturn(0.0);
        when(tracker.crossBorderShare(nearby.get(0))).thenReturn(0.8);
        when(tracker.crossBorderShare(nearby.get(1))).thenReturn(0.7);
        when(tracker.crossBorderShare(far.get(0))).thenReturn(0.8);
        when(tracker.crossBorderShare(far.get(1))).thenReturn(0.8);
        when(tracker.crossBorderShare(
                org.mockito.ArgumentMatchers.<Collection<Id<Link>>>any())).thenReturn(0.75);
        CrossBorderStationDetector detector = new CrossBorderStationDetector(network, 0.30);

        List<CrossBorderStation> first = detector.update(tracker);

        assertEquals(3, first.size());
        CrossBorderStation paired = first.stream()
                .filter(station -> station.links().size() == 2)
                .findFirst().orElseThrow();
        assertEquals(Set.copyOf(nearby), Set.copyOf(paired.links()));
        assertEquals(Set.copyOf(far), first.stream()
                .filter(station -> station.links().size() == 1)
                .map(CrossBorderStation::inLink)
                .collect(java.util.stream.Collectors.toSet()));

        when(tracker.crossBorderShare(nearby.get(0))).thenReturn(0.0);
        when(tracker.crossBorderShare(nearby.get(1))).thenReturn(0.0);
        List<CrossBorderStation> second = detector.update(tracker);

        assertEquals(first, second);
    }

    @Test
    public void detectsSingleLinkStationWhenOnlyOneDirectionHasHighShare() {
        Network network = NetworkUtils.createNetwork();
        List<Id<Link>> links = addStation(network, "one-direction", 0.0, 10.0);
        TrafficScoringTracker tracker = mock(TrafficScoringTracker.class);
        when(tracker.monitoredLinks()).thenReturn(Set.copyOf(links));
        when(tracker.crossBorderShare(links.get(0))).thenReturn(0.80);
        when(tracker.crossBorderShare(links.get(1))).thenReturn(0.10);
        when(tracker.crossBorderShare(
                org.mockito.ArgumentMatchers.<Collection<Id<Link>>>any())).thenReturn(0.80);
        CrossBorderStationDetector detector = new CrossBorderStationDetector(network, 0.30);

        List<CrossBorderStation> stations = detector.update(tracker);

        assertEquals(1, stations.size());
        assertEquals(List.of(links.get(0)), stations.getFirst().links());
    }

    @Test
    public void suppressesConnectedSeriesStationWithLowerCrossBorderShare() {
        Network network = NetworkUtils.createNetwork();
        List<Id<Link>> stronger = addStation(network, "stronger", 0.0, 0.0);
        List<Id<Link>> weaker = addStation(network, "weaker", 1_000.0, 0.0);
        Set<Id<Link>> counted = Set.of(
                stronger.get(0), stronger.get(1), weaker.get(0), weaker.get(1));
        TrafficScoringTracker tracker = mock(TrafficScoringTracker.class);
        when(tracker.monitoredLinks()).thenReturn(counted);
        for (Id<Link> linkId : counted) when(tracker.crossBorderShare(linkId)).thenReturn(0.60);
        when(tracker.crossBorderShare(stronger.get(0))).thenReturn(0.90);
        when(tracker.crossBorderShare(stronger.get(1))).thenReturn(0.90);
        when(tracker.crossBorderShare(
                org.mockito.ArgumentMatchers.<Collection<Id<Link>>>any())).thenAnswer(invocation -> {
            Collection<Id<Link>> links = invocation.getArgument(0);
            return links.contains(stronger.get(0)) ? 0.90 : 0.60;
        });
        when(tracker.crossBorderConnectionShare(
                org.mockito.ArgumentMatchers.<Collection<Id<Link>>>any(),
                org.mockito.ArgumentMatchers.<Collection<Id<Link>>>any()))
                .thenReturn(0.75);
        CrossBorderStationDetector detector = new CrossBorderStationDetector(network, 0.30);

        List<CrossBorderStation> stations = detector.update(tracker);

        assertEquals(1, stations.size());
        assertTrue(stations.getFirst().links().containsAll(stronger));
    }

    private static List<Id<Link>> addStation(
            Network network, String prefix, double x, double separation) {
        var factory = network.getFactory();
        var a = factory.createNode(Id.createNodeId(prefix + "_a"), new Coord(x, 0.0));
        var b = factory.createNode(Id.createNodeId(prefix + "_b"), new Coord(x + 20.0, 0.0));
        var c = factory.createNode(Id.createNodeId(prefix + "_c"), new Coord(x, separation));
        var d = factory.createNode(Id.createNodeId(prefix + "_d"), new Coord(x + 20.0, separation));
        network.addNode(a);
        network.addNode(b);
        network.addNode(c);
        network.addNode(d);
        Link inbound = factory.createLink(Id.createLinkId(prefix + "_in"), a, b);
        Link outbound = factory.createLink(Id.createLinkId(prefix + "_out"), d, c);
        network.addLink(inbound);
        network.addLink(outbound);
        return List.of(inbound.getId(), outbound.getId());
    }
}
