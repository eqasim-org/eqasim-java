package org.eqasim.core.components.network_calibration;

import java.util.Objects;
import java.util.Set;
import org.eqasim.core.components.network_calibration.cost_calibration.PenaltyGroupKey;
import org.eqasim.core.components.network_calibration.cost_calibration.PenaltyManager;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;
import static org.junit.Assert.*;

public class TestPenaltyKeyReuse {
    @Test public void hashMatchesPreviousVarargsIncludingOverflow() {
        for (int category = 1; category <= 5; category++) for (boolean urban : new boolean[]{false, true}) {
            for (int region : new int[]{0, 1, 127, 128, 10000, Integer.MAX_VALUE}) {
                var key = new PenaltyGroupKey(category, urban, region);
                assertEquals(Objects.hash(category, urban, region), key.hashCode());
                assertEquals(key, new PenaltyGroupKey(category, urban, region));
            }
        }
    }

    @Test public void reusedKeysFollowLiveLinkCategoriesAndLivePenaltyUpdates() {
        var network = NetworkUtils.createNetwork();
        var a = network.getFactory().createNode(Id.createNodeId("a"), new Coord(0, 0));
        var b = network.getFactory().createNode(Id.createNodeId("b"), new Coord(1, 0));
        network.addNode(a); network.addNode(b);
        var link = network.getFactory().createLink(Id.createLinkId("a-b"), a, b);
        network.addLink(link);
        link.setAllowedModes(Set.of("car"));
        link.getAttributes().putAttribute("osm:way:highway", "residential");
        link.getAttributes().putAttribute("municipalityType", " UrbanCore ");
        var config = new NetworkCalibrationConfigGroup();
        var categorizer = new LinkCategorizer(network, config);
        link.setNumberOfLanes(config.getCategoryFivePromotionLaneThreshold());
        link.setFreespeed(config.getCategoryFivePromotionSpeedThreshold() / 3.6);
        var original = categorizer.getPenaltyGroupKey(link);
        assertEquals(new PenaltyGroupKey(5, true, 0), original);
        assertSame(original, categorizer.getPenaltyGroupKey(link));
        var penalties = new PenaltyManager(config, config.getCostCalibrationConfigGroup());
        assertEquals(0, penalties.getPenalty(original), 0);
        penalties.setPenalty(original, 0.25);
        assertEquals(0.25, penalties.getPenalty(categorizer.getPenaltyGroupKey(link)), 0);
        penalties.setPenalty(new PenaltyGroupKey(5, true, 0), 0.125);
        assertEquals(0.125, penalties.getPenalty(categorizer.getPenaltyGroupKey(link)), 0);
        link.setFreespeed(Math.nextUp(link.getFreespeed()));
        var promoted = categorizer.getPenaltyGroupKey(link);
        assertEquals(new PenaltyGroupKey(4, true, 0), promoted);
        assertEquals(0, penalties.getPenalty(promoted), 0);
        link.setFreespeed(1); link.setNumberOfLanes(Math.nextUp(link.getNumberOfLanes()));
        assertSame(promoted, categorizer.getPenaltyGroupKey(link));
        link.setNumberOfLanes(0);
        assertSame(original, categorizer.getPenaltyGroupKey(link));
        link.getAttributes().putAttribute("municipalityType", "rural");
        assertEquals(new PenaltyGroupKey(5, false, 0), categorizer.getPenaltyGroupKey(link));
        link.getAttributes().putAttribute("municipalityType", "outside");
        assertNull(categorizer.getPenaltyGroupKey(link));
        link.getAttributes().putAttribute("municipalityType", "urban");
        link.setAllowedModes(Set.of("bike"));
        assertNull(categorizer.getPenaltyGroupKey(link));
        link.setAllowedModes(Set.of("car"));
        link.getAttributes().putAttribute("osm:way:highway", "motorway");
        assertEquals(new PenaltyGroupKey(1, true, 0), categorizer.getPenaltyGroupKey(link));
        var extension = new LinkCategorizer(network, config) {
            @Override public int getPenaltiesSpecialRegionId(Link ignored) { return 1000; }
        };
        assertEquals(new PenaltyGroupKey(1, true, 1000), extension.getPenaltyGroupKey(link));
    }
}
