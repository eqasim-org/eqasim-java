package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.RegionalSubscription;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Authority;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.NetworkOfDistances;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.ZonalRegistry;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Zone;
import org.junit.Test;

/**
 * A Z-Pass zone is covered by a subscription of another network only if it is the same zone (at a same stop): ZVV 110
 * and ZPassAwelle 110 at Zürich HB, not the zone 10 of Passepartout and the zone 10 of ZPassSchwyzZug far apart.
 */
public class TestZPassZoneCoverage {
    private static final Authority ZVV = new Authority("ZVV", 2, "Verbund");
    private static final Authority ZPASS_AWELLE = new Authority("ZPassAwelle", 1, "ZPass");
    private static final Authority PASSEPARTOUT = new Authority("Passepartout", 2, "Verbund");
    private static final Authority ZPASS_SZ = new Authority("ZPassSchwyzZug", 1, "ZPass");

    private final ZonalRegistry registry = new ZonalRegistry(List.of(ZVV, ZPASS_AWELLE, PASSEPARTOUT, ZPASS_SZ), List.of(
            new Zone(ZVV, "110", List.of("HB", "Stadelhofen")),
            new Zone(ZPASS_AWELLE, "110", List.of("HB")),
            new Zone(PASSEPARTOUT, "10", List.of("Luzern")),
            new Zone(ZPASS_SZ, "10", List.of("Zug"))));

    private final SwissPtRoutePredictor predictor = new SwissPtRoutePredictor(null, registry, new NetworkOfDistances());

    @Test
    public void sameZoneAcrossAZPass() {
        assertTrue(predictor.isZoneCovered("ZPassAwelle", "110", RegionalSubscription.parse("ZVV:110")));
        assertTrue(predictor.isZoneCovered("ZVV", "110", RegionalSubscription.parse("ZPassAwelle:110")));
        assertTrue(predictor.isZoneCovered("ZVV", "110", RegionalSubscription.parse("ZVV:110")));
    }

    @Test
    public void sameNumberInUnrelatedNetworks() {
        assertFalse(predictor.isZoneCovered("ZPassSchwyzZug", "10", RegionalSubscription.parse("Passepartout:10")));
        assertFalse(predictor.isZoneCovered("Passepartout", "10", RegionalSubscription.parse("ZPassSchwyzZug:all")));
    }
}
