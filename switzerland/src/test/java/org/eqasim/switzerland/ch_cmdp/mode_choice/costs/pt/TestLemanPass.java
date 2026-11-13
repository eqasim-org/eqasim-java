package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eqasim.core.simulation.mode_choice.utilities.variables.PersonVariables;
import org.eqasim.switzerland.ch.mode_choice.SwissModeChoiceModule;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Authority;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.LemanPassReader;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Zone;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;
import org.junit.Test;

/**
 * The Léman Pass single tickets, on the files of the 2025 tpg fare table written by eqasim-switzerland, and the
 * half-fare card abroad. The expected prices are those of the Python implementation (utils/gtfs_router/lemanpass.py).
 */
public class TestLemanPass {
    private static final Authority UNIRESO = new Authority("Unireso", 2, "ZoneBased");
    private static final Authority SBB = new Authority("SBB", 0, "SBB");
    private static final PersonVariables ADULT = new PersonVariables(30);

    // stops of lemanpass_stations.csv, and a few bus stops (French ids, not Swiss)
    private static final String ANNEMASSE = "8774549";
    private static final String GENEVE = "8501008";
    private static final String EAUX_VIVES = "8516273";
    private static final String LANCY_BACHET = "8517142";
    private static final String THONON = "8774564";
    private static final String COPPET = "8501023";

    private static final LemanPass LEMAN_PASS = read("lemanpass_products.csv", "lemanpass_stations.csv");

    private static URL resource(String name) {
        return TestLemanPass.class.getResource(name);
    }

    private static LemanPass read(String products, String stations) {
        try {
            return LemanPassReader.read(resource(products), resource(stations));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ------------------------------------------------------------------ legs

    /** A leg through some stops, each "id" or "id@zone" (a Unireso zone), with the networks common to all its stops. */
    private static SwissPtLegVariables leg(String mode, String... stops) {
        List<String> ids = new ArrayList<>();
        List<Collection<Zone>> zones = new ArrayList<>();
        for (String stop : stops) {
            String[] parts = stop.split("@");
            ids.add(parts[0]);
            List<Zone> stopZones = new ArrayList<>();
            if (parts.length > 1) {
                stopZones.add(new Zone(UNIRESO, parts[1], List.of(parts[0])));
            }
            zones.add(stopZones);
        }

        Map<String, List<Zone>> common = new LinkedHashMap<>();
        if (zones.stream().allMatch(z -> !z.isEmpty())) {
            List<Zone> unireso = new ArrayList<>();
            for (Collection<Zone> z : zones) {
                for (Zone zone : z) {
                    if (unireso.stream().noneMatch(u -> u.getZoneId().equals(zone.getZoneId()))) unireso.add(zone);
                }
            }
            common.put("Unireso", unireso);
        }

        double[] distances = new double[ids.size()];
        for (int i = 0; i < distances.length; i++) distances[i] = 2000.0 * i;
        SwissPtLegVariables leg = new SwissPtLegVariables(common, 0, 0, distances[distances.length - 1], -1,
                ids.get(0), ids.get(ids.size() - 1), ids.get(0), ids.get(ids.size() - 1), ids, zones, distances);
        leg.mode = mode;
        return leg;
    }

    private static Set<String> places(String... places) {
        Set<String> result = new java.util.HashSet<>();
        for (String place : places) {
            result.add(place.matches("\\d+") ? LemanPass.zonePlace(place) : LemanPass.stationPlace(place));
        }
        return result;
    }

    private static void assertCheapest(String label, double price, Set<String> first, Set<String> last, Set<String> through,
            List<String> profiles, int travelClass) {
        LemanPass.Match match = LEMAN_PASS.cheapest(first, last, through, profiles, travelClass);
        assertNotNull(match);
        assertEquals(label, match.product().label());
        assertEquals(label, price, match.price(), 1e-9);
    }

    // ------------------------------------------------------------------ the files

    @Test
    public void filesAreRead() {
        assertEquals(1102, LEMAN_PASS.products().size());
        assertEquals("Annemasse (LEX)", LEMAN_PASS.station(ANNEMASSE).name());
        assertEquals("FR", LEMAN_PASS.station("StopPoint:OCETrain:::TER-87745497").country());
        assertEquals("10", LEMAN_PASS.station("8501008:0:3").zone());

        // the stops of a schedule whose ids differ from those of the file: by their station
        assertEquals("Genève (LEX)", LEMAN_PASS.station("ch:1:sloid:1008:2:3").name());    // SLOID, the file has 8501008
        assertEquals("Vernier (LEX)", LEMAN_PASS.station("ch:1:sloid:1007:1:1").name());   // the file has only 8501007:0:1
        assertEquals("Bellegarde (LEX)", LEMAN_PASS.station("8774500").name());            // the file has only TER-87745000
        assertNull(LEMAN_PASS.station("StopPoint:OCECar:::TER-OCE87745000"));
        assertNull(LEMAN_PASS.station("8501120"));                                          // Lausanne

        LemanPass.Product product = LEMAN_PASS.products().stream().filter(p -> p.label().equals("Zone 10 + Zone 210")).findFirst().get();
        assertEquals(Set.of("zone:10", "zone:210"), product.pieces());
        assertTrue(product.endB().contains("station:Annemasse (LEX)"));
        assertTrue(product.cover().contains("station:Genève (LEX)"));
        assertEquals(4.70, product.price("adult", 2), 0);
        assertEquals(3.70, product.price("reduced", 2), 0);
        assertTrue(Double.isNaN(product.price("child", 2)));

        // only the products of zones only have a cover
        assertEquals(30, LEMAN_PASS.products().stream().filter(p -> p.cover() != null).count());
    }

    @Test
    public void withoutTheFilesThereIsNoLemanPass() throws Exception {
        assertNull(read("lemanpass_products_empty.csv", "lemanpass_stations_empty.csv"));

        SwissPtPricingConfigGroup group = new SwissPtPricingConfigGroup();
        URL context = resource("lemanpass_products.csv");
        assertNull(LemanPassReader.read(group, context));
        group.setLemanPassProductsPath("");
        group.setLemanPassStationsPath(" ");
        assertNull(LemanPassReader.read(group, context));

        group.setLemanPassProductsPath("lemanpass_products.csv");
        assertThrows(IllegalArgumentException.class, () -> LemanPassReader.read(group, context));
        group.setLemanPassStationsPath("lemanpass_stations.csv");
        assertEquals(1102, LemanPassReader.read(group, context).products().size());   // relative to the config
    }

    // ------------------------------------------------------------------ matching (section 5 of the handoff)

    @Test
    public void cheapestProductsAreThoseOfPython() {
        List<String> adult = LemanPass.profiles(30, false);
        List<String> child = LemanPass.profiles(10, false);
        List<String> halbtax = LemanPass.profiles(30, true);

        // train Annemasse -> Genève
        Set<String> annemasse = places("Annemasse (LEX)");
        Set<String> geneve = places("Genève (LEX)");
        assertCheapest("Zone 10 + Zone 210", 4.70, annemasse, geneve, Set.of(), adult, 2);
        assertCheapest("Zone 10 + Zone 210", 8.40, annemasse, geneve, Set.of(), adult, 1);
        assertCheapest("Annemasse (LEX) + Genève (LEX)", 3.90, annemasse, geneve, Set.of(), child, 2);
        assertCheapest("Zone 10 + Zone 210", 3.70, annemasse, geneve, Set.of(), halbtax, 2);

        // the same, then a tram in Zone 10
        Set<String> geneveAndTram = places("Genève (LEX)", "10");
        assertCheapest("Zone 10 + Zone 210", 4.70, annemasse, geneveAndTram, Set.of(), adult, 2);
        assertCheapest("Zone 10 + Zone 210", 8.40, annemasse, geneveAndTram, Set.of(), adult, 1);
        assertCheapest("Annemasse (LEX) + Zone 10", 4.20, annemasse, geneveAndTram, Set.of(), child, 2);
        assertCheapest("Zone 10 + Zone 210", 3.70, annemasse, geneveAndTram, Set.of(), halbtax, 2);

        // a bus in Zone 300, then the train Thonon -> Genève-Eaux-Vives
        Set<String> thonon = places("Thonon-les-Bains (LEX)", "300");
        Set<String> eauxVives = places("Genève-Eaux-Vives (LEX)");
        String thononProduct = "Zone 10 + Thonon-les-Bains (LEX) + Zone 300";
        assertCheapest(thononProduct, 15.70, thonon, eauxVives, Set.of(), adult, 2);
        assertCheapest(thononProduct, 25.50, thonon, eauxVives, Set.of(), adult, 1);
        assertCheapest(thononProduct, 9.20, thonon, eauxVives, Set.of(), child, 2);
        assertCheapest(thononProduct, 15.70, thonon, eauxVives, Set.of(), halbtax, 2);

        // the train Genève -> Lancy-Bachet, then the bus to Saint-Julien: the cover of Zone 10 + Zone 230
        Set<String> lancy = places("Lancy-Bachet (LEX)", "230");
        assertCheapest("Zone 10 + Zone 230", 4.60, geneve, lancy, Set.of(), adult, 2);
        assertCheapest("Zone 10 + Zone 230", 7.80, geneve, lancy, Set.of(), adult, 1);
        assertCheapest("Zone 10 + Zone 230", 4.60, geneve, lancy, Set.of(), child, 2);
        assertCheapest("Zone 10 + Zone 230", 3.30, geneve, lancy, Set.of(), halbtax, 2);

        // buses only, Zone 230 -> Zone 10
        Set<String> zones = places("230", "10");
        assertCheapest("Zone 10 + Zone 230", 4.60, places("230"), places("10"), zones, adult, 2);
        assertCheapest("Zone 10 + Zone 230", 7.80, places("230"), places("10"), zones, adult, 1);
        assertCheapest("Zone 10 + Zone 230", 4.60, places("230"), places("10"), zones, child, 2);
        assertCheapest("Zone 10 + Zone 230", 3.30, places("230"), places("10"), zones, halbtax, 2);
    }

    @Test
    public void profilesOfTheTravellers() {
        assertEquals(List.of("adult"), LemanPass.profiles(5, false));
        assertEquals(List.of("adult", "child"), LemanPass.profiles(6, false));
        assertEquals(List.of("adult", "child"), LemanPass.profiles(15, false));
        assertEquals(List.of("adult", "youth", "reduced"), LemanPass.profiles(16, true));
        assertEquals(List.of("adult", "youth"), LemanPass.profiles(25, false));
        assertEquals(List.of("adult", "reduced"), LemanPass.profiles(26, true));
    }

    // ------------------------------------------------------------------ the candidate tickets of a journey

    @Test
    public void ticketsOfJourneys() {
        // the train Annemasse -> Genève
        List<LemanPass.Ticket> tickets = LEMAN_PASS.singleTickets(List.of(leg("rail", ANNEMASSE + "@210", GENEVE + "@10")), 30, false, 2);
        assertEquals(1, tickets.size());
        assertEquals("Zone 10 + Zone 210", tickets.get(0).product().label());
        assertEquals(0, tickets.get(0).start());
        assertEquals(1, tickets.get(0).end());
        // the same with the SLOID of Genève, as in the schedules of 2026
        tickets = LEMAN_PASS.singleTickets(List.of(leg("rail", ANNEMASSE + "@210", "ch:1:sloid:1008:2:3@10")), 30, false, 2);
        assertEquals("Zone 10 + Zone 210", tickets.get(0).product().label());

        // the same, then a tram in Zone 10: a ticket with the tram, and one without
        var train = leg("rail", ANNEMASSE + "@210", GENEVE + "@10");
        var tram = leg("tram", "tramA@10", "tramB@10");
        tickets = LEMAN_PASS.singleTickets(List.of(train, tram), 10, false, 2);
        assertEquals(2, tickets.size());
        assertEquals("Annemasse (LEX) + Zone 10", tickets.get(0).product().label());
        assertEquals(4.20, tickets.get(0).price(), 1e-9);
        assertEquals(2, tickets.get(0).end());
        assertEquals("Annemasse (LEX) + Genève (LEX)", tickets.get(1).product().label());
        assertEquals(1, tickets.get(1).end());

        // a bus in Zone 300 to Thonon, then the train Thonon -> Genève-Eaux-Vives
        var bus = leg("bus", "FR:evian@300", THONON + "@300");
        var thononTrain = leg("rail", THONON + "@300", EAUX_VIVES + "@10");
        tickets = LEMAN_PASS.singleTickets(List.of(bus, thononTrain), 30, false, 2);
        assertEquals("Zone 10 + Thonon-les-Bains (LEX) + Zone 300", tickets.get(0).product().label());
        assertEquals(15.70, tickets.get(0).price(), 1e-9);

        // the train Genève -> Lancy-Bachet, then the bus to Saint-Julien
        var lancyTrain = leg("rail", GENEVE + "@10", LANCY_BACHET + "@10");
        var stJulien = leg("bus", "lancy@10", "FR:stjulien@230");
        tickets = LEMAN_PASS.singleTickets(List.of(lancyTrain, stJulien), 30, true, 2);
        assertEquals("Zone 10 + Zone 230", tickets.get(0).product().label());
        assertEquals(3.30, tickets.get(0).price(), 1e-9);

        // buses only, Zone 230 -> Zone 10
        tickets = LEMAN_PASS.singleTickets(List.of(leg("bus", "FR:stjulien@230", "border@230"),
                leg("bus", "border@10", "cornavin@10")), 30, false, 2);
        assertEquals(1, tickets.size());
        assertEquals("Zone 10 + Zone 230", tickets.get(0).product().label());
        assertEquals(4.60, tickets.get(0).price(), 1e-9);

        // the train Genève -> Coppet: not abroad
        assertTrue(LEMAN_PASS.singleTickets(List.of(leg("rail", GENEVE + "@10", COPPET)), 30, false, 2).isEmpty());
        // a bus leaving the area: no ticket
        assertTrue(LEMAN_PASS.singleTickets(List.of(leg("bus", "FR:stjulien@230", "FR:outside")), 30, false, 2).isEmpty());
        // a bus (not a train) between two Léman Pass stations, alone in the area: zones only
        assertEquals("Zone 10 + Zone 210", LEMAN_PASS.singleTickets(List.of(leg("bus", ANNEMASSE + "@210", GENEVE + "@10")), 30, false, 2)
                .get(0).product().label());
    }

    // ------------------------------------------------------------------ the price of a journey

    private static SwissPtStageCostCalculator fares(boolean lemanPass) {
        SwissPtStageCostCalculator fares = SwissModeChoiceModule.createDefaultPtStageCostCalculator();
        fares.setLemanPass(lemanPass ? LEMAN_PASS : null);
        return fares;
    }

    @Test
    public void lemanPassIsKeptWhenCheaper() {
        // Saint-Julien -> Genève by bus with the half-fare card: Léman Pass Zone 10 + Zone 230 at 3.30, rather than the
        // Unireso half fare 3.60 (centre zone 2.00 + 1.60)
        List<SwissPtLegVariables> stJulien = List.of(leg("bus", "FR:stjulien@230", "8587057@10"));
        assertEquals(3.60, fares(false).price(stJulien, true, ADULT), 1e-9);
        assertEquals(3.30, fares(true).price(stJulien, true, ADULT), 1e-9);
        // without the half-fare card, Unireso (4.60) and the Léman Pass (4.60) cost the same
        assertEquals(4.60, fares(true).price(stJulien, false, ADULT), 1e-9);

        // Annemasse -> Genève: Unireso Zone 10 + Zone 210 (4.60, free under 19) without the Léman Pass. With it, Zone 210
        // is a zone of the Léman Pass, not of Unireso: the Léman Pass price, even if dearer, and the child price
        List<SwissPtLegVariables> annemasse = List.of(leg("rail", ANNEMASSE + "@210", GENEVE + "@10"));
        assertEquals(4.60, fares(false).price(annemasse, false, ADULT), 1e-9);
        assertEquals(0.00, fares(false).price(annemasse, false, new PersonVariables(10)), 1e-9);
        assertEquals(4.70, fares(true).price(annemasse, false, ADULT), 1e-9);
        assertEquals(3.70, fares(true).price(annemasse, true, ADULT), 1e-9);
        assertEquals(3.90, fares(true).price(annemasse, false, new PersonVariables(10)), 1e-9);

        // the train Thonon -> Genève-Eaux-Vives: the Léman Pass (11.50) rather than Unireso Zone 300 + Zone 10 (4.60)
        List<SwissPtLegVariables> thonon = List.of(leg("rail", THONON + "@300", EAUX_VIVES + "@10"));
        assertEquals(4.60, fares(false).price(thonon, false, ADULT), 1e-9);
        assertEquals(11.50, fares(true).price(thonon, false, ADULT), 1e-9);

        // within Zone 10, Unireso as before
        List<SwissPtLegVariables> geneva = List.of(leg("tram", "8587057@10", "8592000@10"));
        assertEquals(3.00, fares(true).price(geneva, false, ADULT), 1e-9);
        assertEquals(0.00, fares(true).price(geneva, false, new PersonVariables(10)), 1e-9);

        // a Unireso ride abroad that no Léman Pass product fits keeps the Unireso price
        List<SwissPtLegVariables> noProduct = List.of(leg("bus", "FR:x@380", "FR:y@400"));
        assertEquals(fares(false).price(noProduct, false, ADULT), fares(true).price(noProduct, false, ADULT), 1e-9);
    }

    @Test
    public void lemanPassIsBoughtWholeAndTheOtherLegsAsUsual() {
        // with the half-fare card: an SBB train Lausanne -> Genève, the train Genève -> Lancy-Bachet and the bus to
        // Saint-Julien. Usual: SBB + Unireso Zone 10 + Zone 230 (3.60); or SBB + Léman Pass Zone 10 + Zone 230 (3.30)
        var sbbTrain = leg("rail", "8501120", GENEVE);
        sbbTrain.zones = Map.of("SBB", List.of(new Zone(SBB, "SBB", List.of())));
        sbbTrain.sbbDistance = 60;
        var lancyTrain = leg("rail", GENEVE + "@10", LANCY_BACHET + "@10");
        var stJulien = leg("bus", "8587001@10", "FR:stjulien@230");
        List<SwissPtLegVariables> journey = List.of(sbbTrain, lancyTrain, stJulien);

        SwissPtStageCostCalculator fares = fares(true);
        double sbb = fares.ticketsPrice(List.of(sbbTrain), true, ADULT);
        assertEquals(PtFareRounding.roundToCents(3.60 + sbb), fares(false).price(journey, true, ADULT), 1e-9);
        assertEquals(PtFareRounding.roundToCents(3.30 + sbb), fares.price(journey, true, ADULT), 1e-9);

        // a Unireso subscription covering the two last legs: they are free, and the Léman Pass does not help
        List<SwissPtLegVariables> paid = new ArrayList<>();
        paid.add(sbbTrain);
        paid.add(null);
        paid.add(null);
        assertEquals(sbb, fares.price(journey, paid, true, ADULT), 1e-9);
    }

    // ------------------------------------------------------------------ the half-fare card abroad (section 4)

    @Test
    public void halfFareCardHoldsOnlyOnSwissFares() {
        assertTrue(SwissPtLegVariables.isSwissStop("8501008"));
        assertTrue(SwissPtLegVariables.isSwissStop("8501008:0:3"));
        assertTrue(SwissPtLegVariables.isSwissStop("Parent8501008"));
        assertTrue(SwissPtLegVariables.isSwissStop("ch:1:sloid:3000:2:3"));
        assertTrue(SwissPtLegVariables.isSwissStop("StopPoint:OCETrain:::TER-85162735"));
        assertTrue(!SwissPtLegVariables.isSwissStop("8774549"));
        assertTrue(!SwissPtLegVariables.isSwissStop("StopPoint:OCETrain:::TER-87745497"));
        assertTrue(!SwissPtLegVariables.isSwissStop("FR:12345"));
        assertTrue(!SwissPtLegVariables.isSwissStop("85010080"));

        SwissPtStageCostCalculator fares = fares(false);

        // a bus in France in no network: no half fare with the card, half fare under 16
        var france = leg("bus", "FR:a", "FR:b");
        assertEquals(fares.price(List.of(france), false, ADULT), fares.price(List.of(france), true, ADULT), 0);
        assertEquals(fares.price(List.of(france), true, ADULT) / 2, fares.price(List.of(france), false, new PersonVariables(12)), 0.2);
        // in Switzerland, half fare
        var swiss = leg("bus", "8500001", "8500002");
        assertTrue(fares.price(List.of(swiss), true, ADULT) < fares.price(List.of(swiss), false, ADULT));
        // into France, in no network: a stop abroad, no half fare
        var across = leg("bus", "8500001", "FR:b");
        assertEquals(fares.price(List.of(across), false, ADULT), fares.price(List.of(across), true, ADULT), 0);

        // a Unireso ticket all in France (Zone 210 -> Zone 230, priced by distance without the centre zone): no half fare
        var uniresoFrance = leg("bus", "FR:annemasse@210", "FR:stjulien@230");
        assertEquals(fares.price(List.of(uniresoFrance), false, ADULT), fares.price(List.of(uniresoFrance), true, ADULT), 0);
        // a Unireso ticket into Switzerland: half fare
        var uniresoGeneva = leg("bus", "FR:annemasse@210", "8587057@10");
        assertEquals(3.60, fares.price(List.of(uniresoGeneva), true, ADULT), 1e-9);

        // SBB: always half fare
        var sbb = leg("rail", "8774549", "8501008");
        sbb.zones = Map.of("SBB", List.of(new Zone(SBB, "SBB", List.of())));
        sbb.sbbDistance = 10;
        assertTrue(fares.price(List.of(sbb), true, ADULT) < fares.price(List.of(sbb), false, ADULT));

        // the legs without their stops are in Switzerland
        Map<String, List<Zone>> none = new HashMap<>();
        var unknown = new SwissPtLegVariables(none, 0, 0, 5000, -1, "FR:a", "FR:b", "a", "b");
        assertTrue(fares.price(List.of(unknown), true, ADULT) < fares.price(List.of(unknown), false, ADULT));
    }
}
