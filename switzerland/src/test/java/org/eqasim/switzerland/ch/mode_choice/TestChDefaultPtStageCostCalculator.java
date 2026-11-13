package org.eqasim.switzerland.ch.mode_choice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.eqasim.core.simulation.mode_choice.utilities.variables.PersonVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.PtStageCostCalculator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.SwissPtStageCostCalculator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.ZoneBasedPricing;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Authority;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Zone;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;
import org.junit.Test;

/**
 * Without a pricingDescriptionPath in the config, the ch scenario reads the pricing description bundled with eqasim
 * (the one of eqasim-switzerland), as ch_cmdp and the Python fare model do.
 */
public class TestChDefaultPtStageCostCalculator {
    private static final PersonVariables ADULT = new PersonVariables(40);

    /** The former ch distance-based price: half-fare coefficients (0.19, 2.04, -0.00011), doubled without Halbtax. */
    private static double formerChDistancePrice(double distance_km, boolean halbtax) {
        double price = 2.04 + 0.19 * distance_km - 0.00011 * distance_km * distance_km;
        if (!halbtax) {
            price = price * 2;
        }
        price = distance_km < 69 ? Math.ceil(price * 5.0) / 5.0 : Math.ceil(price);
        return Math.round(price * 100.0) / 100.0;
    }

    private static List<SwissPtLegVariables> leg(double distance_m) {
        return List.of(new SwissPtLegVariables(Map.of(), 0, 0, distance_m, distance_m, "a", "b", "a", "b"));
    }

    private static SwissPtLegVariables zones(String network, String... ids) {
        Authority authority = new Authority(network, 2, "ZoneBased");
        return new SwissPtLegVariables(Map.of(network, java.util.Arrays.stream(ids).map(id -> new Zone(authority, id, List.of())).toList()),
                0, 0, 5000, -1, "a", "b", "a", "b");
    }

    @Test
    public void distanceBasedPriceIsUnchanged() {
        // the default of the pricing description (0.38, 4.08, -0.00022) is the former ch price; CTJU, which has no
        // prices of its own, is priced by it too
        SwissPtStageCostCalculator table = SwissModeChoiceModule.createDefaultPtStageCostCalculator();
        PtStageCostCalculator calculator = table.priceCalculators.get("None");
        assertNull(table.priceCalculators.get("CTJU"));

        for (double km : new double[] { 0.5, 3, 12.7, 42, 68.9, 69, 150, 320 }) {
            for (boolean halbtax : new boolean[] { false, true }) {
                assertEquals(km + " km, halbtax=" + halbtax, formerChDistancePrice(km, halbtax),
                        calculator.calculatePrice("None", leg(km * 1000), halbtax, ADULT), 0.0);
            }
        }
    }

    @Test
    public void bundledFaresHaveEveryNetwork() {
        Map<String, PtStageCostCalculator> calculators = SwissModeChoiceModule.createDefaultPtStageCostCalculator().priceCalculators;

        for (String network : new String[] { "ZVV", "Unireso", "ZPassOstwind", "ZPassAwelle", "ZPassSchwyzZug", "TNW",
                "OndeVerte", "Frimobil", "Passepartout", "ZVB", "Libero", "Ostwind", "Arcobaleno", "TVSZ", "Davos",
                "EngadinMobil", "Transreno", "Klosters", "SionPT", "Awelle", "Mobilis", "SBB", "None" }) {
            assertTrue(network, calculators.containsKey(network));
        }

        // the current ZVV fares (zone 110 counts twice), Transreno, and Unireso around its centre zone 10
        assertEquals(4.70, calculators.get("ZVV").calculatePrice("ZVV", List.of(zones("ZVV", "121")), false, ADULT), 1e-9);
        assertEquals(7.20, calculators.get("ZVV").calculatePrice("ZVV", List.of(zones("ZVV", "110", "121")), false, ADULT), 1e-9);
        assertEquals(3.00, calculators.get("Transreno").calculatePrice("Transreno", List.of(zones("Transreno", "1")), false, ADULT), 1e-9);
        assertTrue(calculators.get("Unireso") instanceof ZoneBasedPricing);
        assertEquals(3.00, calculators.get("Unireso").calculatePrice("Unireso", List.of(zones("Unireso", "10")), false, ADULT), 1e-9);
        assertEquals(2.00, calculators.get("Unireso").calculatePrice("Unireso", List.of(zones("Unireso", "10")), true, ADULT), 1e-9);
        assertEquals(0.00, calculators.get("Unireso").calculatePrice("Unireso", List.of(zones("Unireso", "10")), true, new PersonVariables(15)), 1e-9);
    }
}
