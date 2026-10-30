package org.eqasim.switzerland.ch_cmdp.dsim;

import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;
import org.matsim.core.scenario.ScenarioUtils;

import static org.junit.Assert.*;

public class TestDsimBikeNetworkAdapter {
    @Test public void createsMinimalUnrestrictedIdempotentLayer() {
        var network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
        var n0 = NetworkUtils.createAndAddNode(network, Id.createNodeId("n0"), new Coord(0, 0, 400));
        var n1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("n1"), new Coord(100, 0, 405));
        var n2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("n2"), new Coord(200, 0, 410));
        var l0 = NetworkUtils.createAndAddLink(network, Id.createLinkId("l0"), n0, n1, 100, 12, 900, 1);
        var l1 = NetworkUtils.createAndAddLink(network, Id.createLinkId("l1"), n1, n2, 100, 8, 600, 1);
        l0.setAllowedModes(Set.of(TransportMode.car, TransportMode.bike));
        l1.setAllowedModes(Set.of(TransportMode.bike));
        l0.getAttributes().putAttribute("osm:way:highway", "secondary");
        l0.getAttributes().putAttribute("unused", "discard me");
        n0.getAttributes().putAttribute("unused", "discard me");
        var restrictions = new DisallowedNextLinks();
        restrictions.addDisallowedLinkSequence(TransportMode.bike, List.of(l1.getId()));
        restrictions.addDisallowedLinkSequence(TransportMode.car, List.of(l1.getId()));
        NetworkUtils.setDisallowedNextLinks(l0, restrictions);

        var result = DsimBikeNetworkAdapter.adapt(network);
        assertEquals(3, result.nodes());
        assertEquals(2, result.links());
        assertFalse(result.reused());
        assertEquals(Set.of(TransportMode.car), l0.getAllowedModes());
        assertTrue(l1.getAllowedModes().isEmpty());
        var copy = network.getLinks().get(Id.createLinkId(DsimBikeNetworkAdapter.LINK_PREFIX + "l0"));
        assertEquals(Set.of(TransportMode.bike), copy.getAllowedModes());
        assertEquals("secondary", copy.getAttributes().getAttribute("osm:way:highway"));
        assertNull(copy.getAttributes().getAttribute("unused"));
        assertEquals(Set.of("osm:way:highway"), copy.getAttributes().getAsMap().keySet());
        assertNotSame(l0.getFromNode(), copy.getFromNode());
        assertEquals(l0.getFromNode().getCoord(), copy.getFromNode().getCoord());
        assertEquals(400.0, copy.getFromNode().getCoord().getZ(), 0.0);
        assertTrue(copy.getFromNode().getAttributes().getAsMap().isEmpty());
        assertNull(NetworkUtils.getDisallowedNextLinks(copy));
        assertNull(NetworkUtils.getDisallowedNextLinks(l1));
        assertEquals(List.of(List.of(l1.getId())),
                NetworkUtils.getDisallowedNextLinks(l0).getDisallowedLinkSequences(TransportMode.car));

        int nodes = network.getNodes().size();
        int links = network.getLinks().size();
        var reused = DsimBikeNetworkAdapter.adapt(network);
        assertTrue(reused.reused());
        assertEquals(nodes, network.getNodes().size());
        assertEquals(links, network.getLinks().size());
    }
}
