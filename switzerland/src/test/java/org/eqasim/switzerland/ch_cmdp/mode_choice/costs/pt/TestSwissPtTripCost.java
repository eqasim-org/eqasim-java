package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Authority;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.NetworkOfDistances;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.ZonalRegistry;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Zone;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPtRoutePredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;
import org.junit.Test;

/**
 * The GA holds in Switzerland only (SwissPtTripCost.withGeneralSubscription), and is no free travel by itself.
 *
 * The network of the tests: Unireso zone 10 at the Swiss stops GVA (Genève) and EAUX (Genève-Eaux-Vives), zone 210 at
 * the French stops ANNE (Annemasse) and AMBI (Ambilly); the 'in switzerland' column of gtfs_zones.csv locates them.
 */
public class TestSwissPtTripCost {
    private static final Authority UNIRESO = new Authority("Unireso", 2, "Verbund");

    private final ZonalRegistry registry = registry();
    private final SwissPtRoutePredictor predictor = new SwissPtRoutePredictor(null, registry, new NetworkOfDistances());

    private static ZonalRegistry registry() {
        ZonalRegistry registry = new ZonalRegistry(List.of(UNIRESO), List.of(
                new Zone(UNIRESO, "10", List.of("GVA", "EAUX")),
                new Zone(UNIRESO, "210", List.of("ANNE", "AMBI"))));
        registry.setStopLocations(Map.of("GVA", true, "EAUX", true, "ANNE", false, "AMBI", false));
        return registry;
    }

    /** A leg along these stops (MATSim stop ids: the feed stop and its link), one km apart. */
    private SwissPtLegVariables leg(String... stops) {
        List<String> ids = new ArrayList<>();
        List<Collection<Zone>> zones = new ArrayList<>();
        Map<String, List<Zone>> legZones = new LinkedHashMap<>();
        double[] distances = new double[stops.length];
        for (int i = 0; i < stops.length; i++) {
            ids.add(stops[i] + ".link:" + i);
            Collection<Zone> atStop = registry.getZones(stops[i]);
            zones.add(atStop);
            distances[i] = 1000.0 * i;
            for (Zone zone : atStop) {
                List<Zone> list = legZones.computeIfAbsent(zone.getAuthority().getId(), a -> new ArrayList<>());
                if (!list.contains(zone)) list.add(zone);
            }
        }
        SwissPtLegVariables leg = new SwissPtLegVariables(legZones, 0, 600, distances[stops.length - 1], -1,
                ids.get(0), ids.get(stops.length - 1), stops[0], stops[stops.length - 1], ids, zones, distances);
        leg.locate(predictor::isSwissStop);
        return leg;
    }

    private static Set<String> zoneIds(SwissPtLegVariables leg) {
        return leg.zones.getOrDefault("Unireso", List.of()).stream().map(Zone::getZoneId)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Test
    public void testStopsAreLocatedWithTheColumnThroughTheirFeedStop() {
        assertTrue(predictor.isSwissStop("EAUX.link:442882"));
        assertFalse(predictor.isSwissStop("ANNE.link:1"));
        assertTrue(predictor.isSwissStop("8501008.link:12"));   // not in the column: by its id
        assertEquals("ch:1:sloid:1950", SwissPtRoutePredictor.feedStopId("ch:1:sloid:1950.link:442882"));
    }

    @Test
    public void testGeneralSubscriptionCoversOnlyTheLegsInSwitzerland() {
        SwissPtLegVariables swiss = leg("GVA", "EAUX");
        SwissPtLegVariables french = leg("ANNE", "AMBI");
        List<SwissPtLegVariables> rest = SwissPtTripCost.withGeneralSubscription(Arrays.asList(swiss, french, null),
                predictor);
        assertNull(rest.get(0));
        assertNotNull(rest.get(1));
        assertEquals(Set.of("210"), zoneIds(rest.get(1)));
        assertEquals(1000.0, rest.get(1).networkDistance, 1e-9);
        assertNull(rest.get(2));   // already covered (a regional subscription) stays covered
    }

    @Test
    public void testGeneralSubscriptionPaysOnlyThePartOfALegAbroad() {
        // Genève - Eaux-Vives - Annemasse - Ambilly: paid from Eaux-Vives, the last Swiss stop, in zone 210 only
        SwissPtLegVariables part = SwissPtTripCost.withGeneralSubscription(
                List.of(leg("GVA", "EAUX", "ANNE", "AMBI")), predictor).get(0);
        assertNotNull(part);
        assertEquals("EAUX.link:1", part.stopIds.get(0));
        assertEquals(Set.of("210"), zoneIds(part));
        assertEquals(2000.0, part.networkDistance, 1e-9);

        // the other way round: paid until Eaux-Vives, the first Swiss stop
        part = SwissPtTripCost.withGeneralSubscription(List.of(leg("AMBI", "ANNE", "EAUX", "GVA")), predictor).get(0);
        assertEquals("EAUX.link:2", part.stopIds.get(part.stopIds.size() - 1));
        assertEquals(Set.of("210"), zoneIds(part));
    }

    @Test
    public void testGeneralSubscriptionIsNotFreeTravel() {
        assertFalse(SwissPtTripCost.hasFreeTravel(40, false, false, () -> 8 * 3600));
        assertTrue(SwissPtTripCost.hasFreeTravel(5, false, false, () -> 8 * 3600));
        assertTrue(SwissPtTripCost.hasFreeTravel(20, false, true, () -> 20 * 3600));
    }
}
