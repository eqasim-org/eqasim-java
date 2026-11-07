package org.eqasim.core.components.network_calibration;

import static org.junit.Assert.*;

import java.util.Set;
import org.eqasim.core.components.network_calibration.freespeed_calibration.FreespeedCalibrationKey;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

public class TestFreespeedGroupStability {
    @Test public void initialGroupsSurviveFactorThresholdCrossingsAndRoutingCopies() {
        Network network = NetworkUtils.createNetwork();
        Link fast = road(network, "fast", 50 / 3.6);
        Link sameGroup = road(network, "same-group", 50 / 3.6);
        Link slow = road(network, "slow", 36 / 3.6);
        var config = new NetworkCalibrationConfigGroup();
        config.setActivate(true); config.setObjective("freespeed"); config.setCalibrate("freespeed");
        var categorizer = new LinkCategorizer(network, config);
        var promoted = categorizer.getFreespeedCalibrationKey(fast);
        var residential = categorizer.getFreespeedCalibrationKey(slow);
        assertEquals(new FreespeedCalibrationKey(4, "urban", 0), promoted);
        assertEquals(new FreespeedCalibrationKey(5, "urban", 0), residential);
        assertEquals(promoted, categorizer.getFreespeedCalibrationKey(sameGroup));

        for (int pass = 0; pass < 3; pass++) {
            fast.setFreespeed(50 / 3.6 * 0.5);
            slow.setFreespeed(36 / 3.6 * 1.3);
            assertEquals(5, categorizer.getBaseCategory(fast));
            assertEquals(4, categorizer.getBaseCategory(slow));
            assertSame(promoted, categorizer.getFreespeedCalibrationKey(fast));
            assertSame(residential, categorizer.getFreespeedCalibrationKey(slow));
            var copy = new RoadNetwork(network);
            assertSame(promoted, categorizer.getFreespeedCalibrationKey(copy.getLinks().get(fast.getId())));
            assertSame(residential, categorizer.getFreespeedCalibrationKey(copy.getLinks().get(slow.getId())));
            // Penalty categorization deliberately continues to follow the live network.
            assertEquals(5, categorizer.getPenaltyGroupKey(fast).getLinkCategory());
            fast.setFreespeed(50 / 3.6);
            slow.setFreespeed(36 / 3.6);
        }
        assertSame(promoted, categorizer.getFreespeedCalibrationKey(sameGroup));

        slow.setNumberOfLanes(3);
        slow.getAttributes().putAttribute("municipalityType", "rural");
        slow.getAttributes().putAttribute("osm:way:highway", "primary");
        assertSame(residential, categorizer.getFreespeedCalibrationKey(slow));
    }

    @Test public void initiallyIneligibleLinksDoNotEnterCalibrationLater() {
        Network network = NetworkUtils.createNetwork();
        Link bike = road(network, "bike", 4);
        bike.setAllowedModes(Set.of("bike"));
        Link unknown = road(network, "unknown", 10);
        unknown.getAttributes().removeAttribute("osm:way:highway");
        var categorizer = new LinkCategorizer(network, new NetworkCalibrationConfigGroup());
        bike.setAllowedModes(Set.of("car"));
        unknown.getAttributes().putAttribute("osm:way:highway", "primary");
        assertNull(categorizer.getFreespeedCalibrationKey(bike));
        assertNull(categorizer.getFreespeedCalibrationKey(unknown));
    }

    private static Link road(Network network, String id, double speed) {
        var a = NetworkUtils.createAndAddNode(network, Id.createNodeId(id + "-a"), new Coord(0, 0));
        var b = NetworkUtils.createAndAddNode(network, Id.createNodeId(id + "-b"), new Coord(100, 0));
        Link link = NetworkUtils.createAndAddLink(network, Id.createLinkId(id), a, b, 100, speed, 1000, 1);
        link.setAllowedModes(Set.of("car"));
        link.getAttributes().putAttribute("osm:way:highway", "residential");
        link.getAttributes().putAttribute("municipalityType", "urban");
        return link;
    }
}
