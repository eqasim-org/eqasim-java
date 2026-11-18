package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eqasim.core.simulation.mode_choice.utilities.variables.PersonVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Authority;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.PricingDescriptionReader;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.ZonalReader;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Zone;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPtRoutePredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** The priority of the French local networks (network priority column of gtfs_zones.csv) and the Swiss stops. */
public class TestNetworkPriority {
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void readsTheOptionalPriorityColumn() throws Exception {
        File zones = temp.newFile("gtfs_zones.csv");
        Files.writeString(zones.toPath(), String.join("\n",
                "stop_id,stop_name,tarif network,local network,zones,network priority",
                "8501008,Genève,['Unireso'],['Genève'],Unireso:10,",
                "8503000,Zürich HB,\"['ZVV', 'ZPassAwelle']\",[nan],\"ZVV:110, ZPassAwelle:110\",",
                "FR:74281:ZE:42421:THONON,Thonon,['StartEvad'],[nan],StartEvad:A,3",
                "FR:74281:ZE:42421:THONON,Thonon,['Unireso'],[nan],Unireso:300,") + "\n");

        Map<String, Integer> priorities = new ZonalReader().readTarifNetworks(zones).stream()
                .collect(Collectors.toMap(Authority::getId, Authority::getPriority));
        assertEquals(Map.of("Unireso", 2, "ZVV", 2, "ZPassAwelle", 1, "StartEvad", 3), priorities);
    }

    @Test
    public void readsFilesWithoutThePriorityColumn() throws Exception {
        File zones = temp.newFile("old_gtfs_zones.csv");
        Files.writeString(zones.toPath(), "stop_id,stop_name,tarif network,local network,zones\n"
                + "8501008,Genève,['Unireso'],['Genève'],Unireso:10\n");
        Map<String, Integer> priorities = new ZonalReader().readTarifNetworks(zones).stream()
                .collect(Collectors.toMap(Authority::getId, Authority::getPriority));
        assertEquals(Map.of("Unireso", 2), priorities);
    }

    @Test
    public void sloidsAbroadAreNotSwiss() {
        assertTrue(SwissPtLegVariables.isSwissStop("ch:1:sloid:1008:2:3"));
        assertTrue(SwissPtLegVariables.isSwissStop("Parentch:1:sloid:1008"));
        assertTrue(SwissPtLegVariables.isSwissStop("ch:1:sloid:82700::51.link:stopMatch:1807368:2"));
        assertTrue(SwissPtLegVariables.isSwissStop("ch:1:sloid:924580:0:01"));
        assertFalse(SwissPtLegVariables.isSwissStop("ch:1:sloid:1400001"));
        assertFalse(SwissPtLegVariables.isSwissStop("ch:1:sloid:1103608_gen:missingSLOID_pf:1"));
        assertFalse(SwissPtLegVariables.isSwissStop("FR:74281:ZE:42421:THONON"));
    }

    @Test
    public void readsTheStopLocations() throws Exception {
        File zones = temp.newFile("located_gtfs_zones.csv");
        Files.writeString(zones.toPath(), String.join("\n",
                "stop_id,stop_name,tarif network,local network,zones,network priority,in switzerland",
                "8501008,Genève,['Unireso'],['Genève'],Unireso:10,,",
                "ch:1:sloid:1008:2:3,Genève,['Unireso'],['Genève'],Unireso:10,,true",
                "FR:74281:ZE:42421:THONON,Thonon,['StartEvad'],[nan],StartEvad:A,3,false",
                "ch:1:sloid:1101509,Hoppetenzell,[nan],[nan],,,false") + "\n");
        Map<String, Boolean> located = new ZonalReader().readStopLocations(zones);
        assertEquals(Map.of("ch:1:sloid:1008:2:3", true, "FR:74281:ZE:42421:THONON", false, "ch:1:sloid:1101509", false), located);

        File old = temp.newFile("unlocated_gtfs_zones.csv");
        Files.writeString(old.toPath(), "stop_id,stop_name,tarif network,local network,zones\n8501008,Genève,['Unireso'],['Genève'],Unireso:10\n");
        assertNull(new ZonalReader().readStopLocations(old));
    }

    @Test
    public void aLocalRideIsPricedWithTheFewestZones() {
        // Évian (EVAD) to a stop in Thonon shared by EVA'D and STAR'T (A, EVAD): one zone, EVAD
        assertEquals(List.of("EVAD"), SwissPtRoutePredictor.smallestZoneCover(List.of(Set.of("EVAD"), Set.of("A", "EVAD"))));
        // between two shared stops: one zone, the first by its id
        assertEquals(List.of("A"), SwissPtRoutePredictor.smallestZoneCover(List.of(Set.of("A", "EVAD"), Set.of("A", "EVAD"))));
        // a shared stop to Douvaine (B): two zones, A rather than EVAD (held by as many stops, first by its id)
        assertEquals(List.of("A", "B"), SwissPtRoutePredictor.smallestZoneCover(List.of(Set.of("A", "EVAD"), Set.of("B"))));
        // the zone held by the most stops wins: A, EVAD then EVAD
        assertEquals(List.of("EVAD"), SwissPtRoutePredictor.smallestZoneCover(List.of(Set.of("A", "EVAD"), Set.of("EVAD"), Set.of("EVAD"))));
    }

    /** A leg in the zones of StartEvad, as the predictor makes it (inSwitzerland is true without stops). */
    private static SwissPtLegVariables startEvad(String... zones) {
        Authority network = new Authority("StartEvad", 3, "ZoneBased");
        List<Zone> list = new ArrayList<>();
        for (String zone : zones) list.add(new Zone(network, zone, List.of()));
        return new SwissPtLegVariables(Map.of("StartEvad", list), 0, 600, 5000, -1, "a", "b", "a", "b");
    }

    @Test
    public void frenchLocalNetworksHaveAgePricesAndNoHalfFare() throws Exception {
        File xml = temp.newFile("pricingDescription.xml");
        Files.writeString(xml.toPath(), String.join("\n",
                "<pricing>",
                "  <distanceBasedDefault><flatRatePrice value=\"4.08\"/><pricePerKM value=\"0.38\"/><power2Factor value=\"-0.00022\"/></distanceBasedDefault>",
                "  <sbb><minimumPrice value=\"3.0\"/><minimumPriceHalbtax value=\"2.20\"/>",
                "    <priceByKMByDistanceBin distance_min=\"0\" distance_max=\"2000\" priceByKM=\"0.4623\" first=\"true\" last=\"true\"/></sbb>",
                "  <authority>",
                "    <name value=\"StartEvad\"/>",
                "    <zonalBased value=\"true\"/>",
                "    <priceListByZone halbtax=\"false\" prices=\"1.11, 2.03\"/>",
                "    <priceListByZone age_range=\"6_15\" prices=\"1.02, 1.85\"/>",
                "    <priceListByZone age_range=\"65_150\" prices=\"1.02, 1.85\" zones=\"A, B\"/>",
                "  </authority>",
                "</pricing>") + "\n");
        PtStageCostCalculator network = PricingDescriptionReader.readPriceDescription(xml).calculator("StartEvad");

        // the half-fare card: no effect (no half-fare list)
        assertEquals(1.11, network.calculatePrice("StartEvad", List.of(startEvad("A")), false, new PersonVariables(30)), 1e-9);
        assertEquals(1.11, network.calculatePrice("StartEvad", List.of(startEvad("A")), true, new PersonVariables(30)), 1e-9);
        assertEquals(2.03, network.calculatePrice("StartEvad", List.of(startEvad("A", "B")), true, new PersonVariables(30)), 1e-9);
        // children, everywhere (also with the half fare of the children under 16)
        assertEquals(1.02, network.calculatePrice("StartEvad", List.of(startEvad("EVAD")), true, new PersonVariables(10)), 1e-9);
        assertEquals(1.85, network.calculatePrice("StartEvad", List.of(startEvad("A", "EVAD")), true, new PersonVariables(10)), 1e-9);
        // seniors, only within zones A and B
        assertEquals(1.02, network.calculatePrice("StartEvad", List.of(startEvad("A")), false, new PersonVariables(70)), 1e-9);
        assertEquals(1.85, network.calculatePrice("StartEvad", List.of(startEvad("A", "B")), false, new PersonVariables(70)), 1e-9);
        assertEquals(1.11, network.calculatePrice("StartEvad", List.of(startEvad("EVAD")), false, new PersonVariables(70)), 1e-9);
        assertEquals(2.03, network.calculatePrice("StartEvad", List.of(startEvad("A", "EVAD")), false, new PersonVariables(70)), 1e-9);
        assertEquals(1.11, network.calculatePrice("StartEvad", List.of(startEvad("A")), false, new PersonVariables(64)), 1e-9);
    }
}
