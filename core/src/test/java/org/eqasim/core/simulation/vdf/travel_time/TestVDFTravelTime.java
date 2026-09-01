package org.eqasim.core.simulation.vdf.travel_time;

import org.eqasim.core.components.traffic.CrossingPenalty;
import org.eqasim.core.simulation.vdf.VDFScope;
import org.eqasim.core.simulation.vdf.travel_time.function.VolumeDelayFunction;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import static org.junit.Assert.assertEquals;

public class TestVDFTravelTime {
    @Test
    public void addsCrossingPenaltyOnlyWhenTravelTimeIsRequested() {
        Network network = NetworkUtils.createNetwork();
        var factory = network.getFactory();
        var fromNode = factory.createNode(Id.createNodeId("from"), new Coord(0.0, 0.0));
        var toNode = factory.createNode(Id.createNodeId("to"), new Coord(100.0, 0.0));
        network.addNode(fromNode);
        network.addNode(toNode);

        Link link = factory.createLink(Id.createLinkId("link"), fromNode, toNode);
        link.setLength(100.0);
        link.setFreespeed(10.0);
        network.addLink(link);

        VDFScope scope = new VDFScope(0.0, 3600.0, 3600.0);
        VolumeDelayFunction vdf = (time, flow, capacity, inputLink) -> 0.0;
        CrossingPenalty crossingPenalty = (inputLink, time, vehicleId) -> 3.0;
        VDFTravelTime travelTime = new VDFTravelTime(
                scope, 1.0, 1.0, 1.0, network, vdf, crossingPenalty);

        assertEquals(13.0, travelTime.getLinkTravelTime(link, 100.0, null, null), 0.0);
    }
}
