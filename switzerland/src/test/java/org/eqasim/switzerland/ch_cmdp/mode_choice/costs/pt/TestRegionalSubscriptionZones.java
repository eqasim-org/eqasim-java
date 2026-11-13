package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eqasim.core.simulation.mode_choice.utilities.variables.PersonVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.RunComputeTransitPrices.CSVRequest;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Authority;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.NetworkOfDistances;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.PricingDescriptionReader;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.ZonalRegistry;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Zone;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPtRoutePredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtVariables;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * A regional subscription (Verbundabo) is valid in the zones of the RegionalSubscriptionProducts attribute of the
 * population: the zones a trip visits are compared with them, and only the zones outside the subscription are paid
 * (this replaces the free travel within ptRegionalRadius_km of home).
 *
 * The network of the tests: ZVV zones 110 (stops A, B, X), 121 (C), 122 (D) and 151 (F); X is also in zone 110 of the
 * Z-Pass ZPassAwelle, which shares the zone numbers of ZVV and Awelle, and Y is in zone 510 of ZPassAwelle and Awelle.
 */
public class TestRegionalSubscriptionZones {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static final Authority ZVV = new Authority("ZVV", 2, "Verbund");
    private static final Authority AWELLE = new Authority("Awelle", 2, "Verbund");
    private static final Authority ZPASS = new Authority("ZPassAwelle", 1, "ZPass");

    private final ZonalRegistry registry = new ZonalRegistry(List.of(ZVV, AWELLE, ZPASS), List.of(
            new Zone(ZVV, "110", List.of("A", "B", "X")),
            new Zone(ZVV, "121", List.of("C")),
            new Zone(ZVV, "122", List.of("D")),
            new Zone(ZVV, "151", List.of("F")),
            new Zone(ZPASS, "110", List.of("X")),
            new Zone(ZPASS, "510", List.of("Y")),
            new Zone(AWELLE, "510", List.of("Y"))));

    private final SwissPtRoutePredictor predictor = new SwissPtRoutePredictor(null, registry, new NetworkOfDistances());

    /** A leg along these stops, one km apart, in the networks shared by all of them (as SwissPtRoutePredictor). */
    private SwissPtLegVariables leg(String... stops) {
        List<String> ids = List.of(stops);
        List<Collection<Zone>> zones = new ArrayList<>();
        double[] distances = new double[stops.length];
        Set<Authority> common = null;
        for (int i = 0; i < stops.length; i++) {
            Collection<Zone> atStop = registry.getZones(stops[i]);
            zones.add(atStop);
            distances[i] = 1000.0 * i;
            Set<Authority> here = atStop.stream().map(Zone::getAuthority).collect(Collectors.toSet());
            if (common == null) common = new HashSet<>(here); else common.retainAll(here);
        }
        Map<String, List<Zone>> legZones = new LinkedHashMap<>();
        for (Collection<Zone> atStop : zones) {
            for (Zone zone : atStop) {
                if (common.contains(zone.getAuthority())) {
                    List<Zone> list = legZones.computeIfAbsent(zone.getAuthority().getId(), a -> new ArrayList<>());
                    if (!list.contains(zone)) list.add(zone);
                }
            }
        }
        return new SwissPtLegVariables(legZones, 0, 600, distances[stops.length - 1], -1, stops[0], stops[stops.length - 1],
                stops[0], stops[stops.length - 1], ids, zones, distances);
    }

    private static Set<String> zoneIds(SwissPtLegVariables leg, String network) {
        return leg.zones.getOrDefault(network, List.of()).stream().map(Zone::getZoneId).collect(Collectors.toCollection(TreeSet::new));
    }

    // ------------------------------------------------------------------ the attribute of the population

    @Test
    public void readsTheZonesOfThePopulation() {
        // values of the RegionalSubscriptionProducts attribute of the synthetic population (test5pct)
        RegionalSubscription zvv = RegionalSubscription.parse("ZVV:110,120,121,122,151,154,155");
        assertEquals(Set.of("ZVV"), zvv.zones().keySet());
        assertTrue(zvv.covers("ZVV", "121"));
        assertFalse(zvv.covers("ZVV", "130"));
        assertFalse(zvv.covers("ZPassAwelle", "121"));

        RegionalSubscription zpass = RegionalSubscription.parse("ZPassAwelle:151,534");
        assertTrue(zpass.covers("ZPassAwelle", "534"));
        assertTrue(RegionalSubscription.parse("Unireso:10").covers("Unireso", "10"));

        // several networks ("+", as written by eqasim-switzerland, or ";"), and a network without zones: all of them
        RegionalSubscription several = RegionalSubscription.parse("ZVV:110,121+Unireso:10");
        assertEquals(Set.of("ZVV", "Unireso"), several.zones().keySet());
        assertEquals(several.zones(), RegionalSubscription.parse(" ZVV:110, 121 ; Unireso:10 ").zones());
        assertTrue(RegionalSubscription.parse("Mobilis:all").covers("Mobilis", "anything"));
        assertTrue(RegionalSubscription.parse("Mobilis").covers("Mobilis", "anything"));

        assertSame(RegionalSubscription.NONE, RegionalSubscription.parse(""));
        assertSame(RegionalSubscription.NONE, RegionalSubscription.parse(null));
        assertEquals("ZVV:110,121+Unireso:10", several.toString());
    }

    // ------------------------------------------------------------------ the zones of the trip and of the subscription

    @Test
    public void legInsideTheSubscriptionIsFree() {
        assertNull(predictor.uncoveredPart(leg("A", "B"), RegionalSubscription.parse("ZVV:110")));
        assertNull(predictor.uncoveredPart(leg("A", "C", "D"), RegionalSubscription.parse("ZVV:110,121,122")));
    }

    @Test
    public void legLeavingTheSubscriptionPaysAnExtensionFromTheLastCoveredStop() {
        SwissPtLegVariables part = predictor.uncoveredPart(leg("A", "B", "C", "D"), RegionalSubscription.parse("ZVV:110"));

        assertNotNull(part);
        assertEquals(List.of("B", "C", "D"), part.stopIds);           // from B, the last stop in the subscription
        assertEquals(Set.of("121", "122"), zoneIds(part, "ZVV"));     // without zone 110 of the subscription
        assertEquals(2000.0, part.networkDistance, 1e-9);             // B to D, not A to D
    }

    @Test
    public void legEnteringTheSubscriptionPaysUntilTheFirstCoveredStop() {
        SwissPtLegVariables part = predictor.uncoveredPart(leg("D", "C", "B", "A"), RegionalSubscription.parse("ZVV:110"));

        assertEquals(List.of("D", "C", "B"), part.stopIds);
        assertEquals(Set.of("121", "122"), zoneIds(part, "ZVV"));
    }

    @Test
    public void legLeavingAndComingBackPaysTheWholeLegButTheCoveredZones() {
        SwissPtLegVariables part = predictor.uncoveredPart(leg("C", "A", "D"), RegionalSubscription.parse("ZVV:110"));

        assertEquals(List.of("C", "A", "D"), part.stopIds);
        assertEquals(Set.of("121", "122"), zoneIds(part, "ZVV"));
        assertEquals(2000.0, part.networkDistance, 1e-9);
    }

    @Test
    public void subscriptionOfOtherZonesCoversNothing() {
        SwissPtLegVariables whole = leg("A", "B");

        SwissPtLegVariables part = predictor.uncoveredPart(whole, RegionalSubscription.parse("ZVV:151"));
        assertEquals(zoneIds(whole, "ZVV"), zoneIds(part, "ZVV"));
        assertEquals(whole.networkDistance, part.networkDistance, 1e-9);

        // another network, and no subscription at all
        assertEquals(Set.of("110"), zoneIds(predictor.uncoveredPart(whole, RegionalSubscription.parse("Unireso:10")), "ZVV"));
        assertSame(whole, predictor.uncoveredPart(whole, RegionalSubscription.NONE));
    }

    @Test
    public void zPassAndTheNetworksItJoinsShareTheirZoneNumbers() {
        // X (ZVV 110, ZPassAwelle 110) to Y (ZPassAwelle and Awelle 510): a ZPassAwelle ride, of which ZVV 110 covers 110
        SwissPtLegVariables part = predictor.uncoveredPart(leg("X", "Y"), RegionalSubscription.parse("ZVV:110"));

        assertEquals(Set.of("ZPassAwelle"), part.zones.keySet());
        assertEquals(Set.of("510"), zoneIds(part, "ZPassAwelle"));
    }

    @Test
    public void tripKeepsOnlyTheLegsToPay() {
        SwissPtVariables trip = new SwissPtVariables();
        trip.addStage(leg("A", "B"));        // in the subscription
        trip.addStage(leg("B", "C", "D"));   // leaves it at B

        SwissPtVariables paid = predictor.uncovered(trip, RegionalSubscription.parse("ZVV:110"));

        assertEquals(1, paid.legVariables.size());
        assertEquals(Set.of("121", "122"), zoneIds(paid.legVariables.get(0), "ZVV"));
        assertSame(trip, predictor.uncovered(trip, RegionalSubscription.NONE));
    }

    // ------------------------------------------------------------------ the price

    private static CSVRequest request(boolean verbundAbo, String zones) {
        // home at the origin: the former rule (free within ptRegionalRadius_km of home) would make every trip free
        return new CSVRequest("trip", 0, 0, 3000, 0, 8 * 3600, 0, 0, false, false, verbundAbo, false, false, false, 30, zones);
    }

    private static SwissPtStageCostCalculator zvvFares() {
        ZoneBasedPricing zvv = new ZoneBasedPricing("ZVV");
        zvv.setFullPriceList(new double[] { 4.70, 4.70, 7.20, 9.40 });
        zvv.setHalbTaxPriceList(new double[] { 3.30, 3.30, 3.60, 4.70 });
        return new SwissPtStageCostCalculator(Map.of("ZVV", zvv, "None", new DefaultDistanceBasedCalculator(0.38, 4.08, -0.00022)));
    }

    @Test
    public void tripIsPricedOnTheZonesOutsideTheSubscription() {
        SwissPtVariables trip = new SwissPtVariables();
        trip.addStage(leg("A", "B", "C", "D"));   // ZVV 110, 121, 122

        double withoutSubscription = RunComputeTransitPrices.calculatePrices(request(false, ""), trip, zvvFares(), predictor).price();
        double zone110 = RunComputeTransitPrices.calculatePrices(request(true, "ZVV:110"), trip, zvvFares(), predictor).price();
        double allZones = RunComputeTransitPrices.calculatePrices(request(true, "ZVV:110,121,122"), trip, zvvFares(), predictor).price();
        double otherZones = RunComputeTransitPrices.calculatePrices(request(true, "ZVV:151"), trip, zvvFares(), predictor).price();

        assertEquals(7.20, withoutSubscription, 1e-9);   // 3 zones
        assertEquals(4.70, zone110, 1e-9);               // 121 and 122 only
        assertEquals(0.0, allZones, 1e-9);
        assertEquals(7.20, otherZones, 1e-9);            // close to home, but outside the subscription

        // the zones count only with a Verbundabo, and a Verbundabo without zones covers nothing
        assertEquals(7.20, RunComputeTransitPrices.calculatePrices(request(false, "ZVV:110,121,122"), trip, zvvFares(), predictor).price(), 1e-9);
        assertEquals(7.20, RunComputeTransitPrices.calculatePrices(request(true, ""), trip, zvvFares(), predictor).price(), 1e-9);
    }

    // ------------------------------------------------------------------ the fares of pricingDescription.xml

    private static final String PRICING = """
            <pricing>
                <distanceBasedDefault>
                    <flatRatePrice value="4.08"/><pricePerKM value="0.38"/><power2Factor value="-0.00022"/>
                </distanceBasedDefault>
                <authority>
                    <name value="Unireso"/>
                    <zonalBased value="centerbased"/>
                    <centerZoneID zone="10"/>
                    <priceCenterZone halbtax="false" price="3.00"/>
                    <priceCenterZone halbtax="true" price="2.00"/>
                    <priceAdditionalZone price="1.60"/>
                    <targetedPricing discount="100" age_range="0_18"/>
                </authority>
                <authority>
                    <name value="ZVV"/>
                    <zonalBased value="true"/>
                    <priceListByZone halbtax="false" prices="4.70, 4.70, 7.20, 9.40"/>
                    <priceListByZone halbtax="true" prices="3.30, 3.30, 3.60, 4.70"/>
                    <zonesCountingAsTwo zones="110, 120"/>
                </authority>
            </pricing>
            """;

    private static SwissPtLegVariables unireso(String... zones) {
        Authority unireso = new Authority("Unireso", 2, "Verbund");
        List<Zone> list = new ArrayList<>();
        for (String zone : zones) list.add(new Zone(unireso, zone, List.of()));
        return new SwissPtLegVariables(Map.of("Unireso", list), 0, 600, 5000, -1, "a", "b", "a", "b");
    }

    @Test
    public void uniresoHasItsOwnHalfFarePrices() throws Exception {
        File xml = folder.newFile("pricingDescription.xml");
        Files.writeString(xml.toPath(), PRICING);
        SwissPtStageCostCalculator fares = PricingDescriptionReader.readPriceDescription(xml);
        PtStageCostCalculator unireso = fares.priceCalculators.get("Unireso");
        PersonVariables adult = new PersonVariables(30);

        assertEquals(3.00, unireso.calculatePrice("Unireso", List.of(unireso("10")), false, adult), 1e-9);
        assertEquals(2.00, unireso.calculatePrice("Unireso", List.of(unireso("10")), true, adult), 1e-9);
        assertEquals(4.60, unireso.calculatePrice("Unireso", List.of(unireso("10", "21")), false, adult), 1e-9);
        assertEquals(3.60, unireso.calculatePrice("Unireso", List.of(unireso("10", "21")), true, adult), 1e-9);
        // under 19: free (targetedPricing); outside the centre zone: the default price by distance (5 km)
        assertEquals(0.0, unireso.calculatePrice("Unireso", List.of(unireso("10")), false, new PersonVariables(12)), 1e-9);
        assertEquals(6.0, unireso.calculatePrice("Unireso", List.of(unireso("21")), false, adult), 1e-9);

        // the list-based network after it is read as before (zone 110 counts twice)
        List<Zone> zones = List.of(new Zone(ZVV, "110", List.of()));
        SwissPtLegVariables zvvLeg = new SwissPtLegVariables(Map.of("ZVV", zones), 0, 600, 1000, -1, "a", "b", "a", "b");
        assertEquals(4.70, fares.priceCalculators.get("ZVV").calculatePrice("ZVV", List.of(zvvLeg), false, adult), 1e-9);
    }
}
