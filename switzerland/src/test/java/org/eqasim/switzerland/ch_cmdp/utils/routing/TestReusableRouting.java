package org.eqasim.switzerland.ch_cmdp.utils.routing;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eqasim.core.scenario.routing.PlanRouter;
import org.eqasim.core.scenario.routing.PopulationRouter;
import org.eqasim.core.scenario.routing.PopulationRoutingIO;
import org.eqasim.core.scenario.routing.ReusableRoute;
import org.eqasim.core.scenario.routing.RunPopulationRouting;
import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.MatsimVehicleWriter;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

public class TestReusableRouting {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void commandLineWorkflowExportsRoutesAndReusesAllModesForSinglePlanPersons() throws Exception {
        Scenario scenario = scenario();
        Path network = file("network.xml"), population = file("population.xml"), vehicles = file("vehicles.xml"), config = file("config.xml");
        new NetworkWriter(scenario.getNetwork()).write(network.toString());
        new PopulationWriter(scenario.getPopulation()).write(population.toString());
        var vehicleType = org.matsim.vehicles.VehicleUtils.createVehicleType(Id.create("car", org.matsim.vehicles.VehicleType.class));
        scenario.getVehicles().addVehicleType(vehicleType);
        scenario.getVehicles().addVehicle(org.matsim.vehicles.VehicleUtils.createVehicle(Id.createVehicleId("car"), vehicleType));
        new MatsimVehicleWriter(scenario.getVehicles()).writeFile(vehicles.toString());
        new ConfigWriter(scenario.getConfig()).write(config.toString());
        var runConfig = ConfigUtils.loadConfig(config.toString());
        runConfig.network().setInputFile(network.toString());
        runConfig.plans().setInputFile(population.toString());
        runConfig.vehicles().setVehiclesFile(vehicles.toString());
        runConfig.qsim().setVehiclesSource(org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource.fromVehiclesData);
        new ConfigWriter(runConfig).write(config.toString());
        Path trips = file("trips.csv"), routes = file("routes.csv"), output = file("routed.xml");
        RunPopulationRouting.main(new String[]{"--config-path", config.toString(), "--export-trips-path", trips.toString(),
                "--export-only", "true", "--eqasim-configurator", "org.eqasim.core.simulation.CoreEqasimConfigurator"});
        assertEquals(6, rows(trips).size());
        assertFalse(rows(trips).getFirst().containsKey("plan_index"));
        assertEquals("start-with-dashes", rows(trips).getFirst().get("origin_link_id"));
        for (boolean assignment : List.of(false, true)) {
            var settings = settings(network, config, assignment, true);
            try (var router = new MultiModeRouter(settings)) { assertEquals(6, router.route(trips, routes)); }
            var exported = rows(routes);
            for (var row : exported) {
                assertFalse(row.containsKey("plan_index"));
                assertFalse(row.get("route").isBlank());
                assertEquals("", row.get("links")); // Route export is independent of return-links.
                var itinerary = ReusableRoute.read(row.get("route"));
                double legTime = itinerary.parts().stream().filter(part -> part.kind().equals("leg"))
                        .mapToDouble(part -> part.travelTime()).sum();
                assertEquals(Double.parseDouble(row.get("travel_time")), legTime, 1e-9);
            }
            RunPopulationRouting.main(new String[]{"--config-path", config.toString(), "--routes-path", routes.toString(),
                    "--output-path", output.toString(), "--eqasim-configurator", "org.eqasim.core.simulation.CoreEqasimConfigurator"});
            Scenario restored = ScenarioUtils.createScenario(ConfigUtils.createConfig());
            new PopulationReader(restored).readFile(output.toString());
            for (var row : exported) {
                var person = restored.getPopulation().getPersons().get(Id.createPersonId(row.get("person_id")));
                var plan = person.getSelectedPlan();
                var trip = TripStructureUtils.getTrips(plan).get(Integer.parseInt(row.get("trip_index")));
                assertEquals(row.get("trip_id"), trip.getTripAttributes().getAttribute("trip_id").toString());
                assertNull(trip.getTripAttributes().getAttribute("plan_index"));
                var leg = trip.getLegsOnly().getFirst();
                // The standard MATSim population writer formats times to whole seconds.
                assertEquals(Double.parseDouble(row.get("travel_time")), leg.getTravelTime().seconds(), 1.0);
                if (leg.getRoute() instanceof NetworkRoute path) {
                    assertEquals("start-with-dashes", path.getStartLinkId().toString());
                    assertEquals(List.of(Id.createLinkId("middle-a"), Id.createLinkId("middle-b")), path.getLinkIds());
                    assertEquals("end-with-dashes", path.getEndLinkId().toString());
                }
            }
        }
    }

    @Test
    public void matchedTripsSkipRoutingEvenWhenReplacingExistingRoutesAndOthersStillRoute() throws Exception {
        Scenario scenario = scenario();
        PopulationRoutingIO.enrichTrips(scenario);
        Path network = file("partial-network.xml"), config = file("partial-config.xml"), trips = file("partial-trips.csv"), output = file("partial-routes.csv");
        new NetworkWriter(scenario.getNetwork()).write(network.toString());
        new ConfigWriter(scenario.getConfig()).write(config.toString());
        PopulationRoutingIO.exportTrips(scenario, trips, Set.of("car"));
        try (var router = new MultiModeRouter(settings(network, config, false, true))) { router.route(trips, output); }
        var exported = rows(output);
        exported.getFirst().remove("trip_id"); // person_id + trip_index is sufficient.
        writeRows(output, List.of(exported.getFirst()));
        var reused = PopulationRoutingIO.importRoutes(scenario, output, Set.of("car"));
        assertTrue(PopulationRoutingIO.needsRouting(scenario, Set.of("car"), reused));
        TripRouter routing = mock(TripRouter.class);
        when(routing.calcRoute(anyString(), any(), any(), anyDouble(), any(), any())).thenAnswer(call -> {
            var leg = scenario.getPopulation().getFactory().createLeg("car");
            leg.setTravelTime(321);
            return List.of(leg);
        });
        new PopulationRouter(2, 1, true, Set.of("car"),
                () -> new PlanRouter(scenario.getActivityFacilities(), routing), reused).run(scenario.getPopulation());
        verify(routing, times(1)).calcRoute(eq("car"), any(), any(), anyDouble(), any(), any());
        writeRows(output, List.of(exported.getFirst(), exported.getFirst()));
        assertTrue(PopulationRoutingIO.importRoutes(scenario, output, Set.of("car")).isEmpty());
    }

    @Test
    public void disabledExportIsBlankAndInvalidMatchesFallBackToRouting() throws Exception {
        Scenario scenario = scenario();
        PopulationRoutingIO.enrichTrips(scenario);
        Path network = file("validation-network.xml"), config = file("validation-config.xml"), trips = file("validation-trips.csv"), output = file("validation-routes.csv");
        new NetworkWriter(scenario.getNetwork()).write(network.toString());
        new ConfigWriter(scenario.getConfig()).write(config.toString());
        PopulationRoutingIO.exportTrips(scenario, trips, Set.of("car"));
        try (var router = new MultiModeRouter(settings(network, config, false, false))) { router.route(trips, output); }
        assertTrue(rows(output).stream().allMatch(row -> row.get("route").isBlank()));
        assertTrue(PopulationRoutingIO.importRoutes(scenario, output, Set.of()).isEmpty());
        try (var router = new MultiModeRouter(settings(network, config, false, true))) { router.route(trips, output); }
        var row = new java.util.LinkedHashMap<>(rows(output).getFirst());
        row.put("trip_id", "wrong-trip"); writeRows(output, List.of(row));
        assertTrue(PopulationRoutingIO.importRoutes(scenario, output, Set.of()).isEmpty());
        row.put("trip_id", "custom,id-\""); row.remove("trip_index");
        writeRows(output, List.of(row)); // Match by custom trip_id without any positional trip key.
        assertEquals(1, PopulationRoutingIO.importRoutes(scenario, output, Set.of()).size());
        row.put("route", "{\"version\":99}"); writeRows(output, List.of(row));
        assertTrue(PopulationRoutingIO.importRoutes(scenario, output, Set.of()).isEmpty());
    }

    @Test
    public void personAndTripIndexMatchMultipleTripsWithoutTripIdOrIdentifierOrdering() throws Exception {
        Scenario scenario = scenario();
        var person = scenario.getPopulation().getPersons().get(Id.createPersonId("car"));
        var plan = person.getSelectedPlan();
        var last = (org.matsim.api.core.v01.population.Activity) plan.getPlanElements().getLast();
        last.setEndTime(300);
        var factory = scenario.getPopulation().getFactory();
        var leg = factory.createLeg("car"); TripStructureUtils.setRoutingMode(leg, "car"); plan.addLeg(leg);
        var end = factory.createActivityFromCoord("home", new Coord(350, 0));
        end.setLinkId(Id.createLinkId("end-with-dashes")); plan.addActivity(end);
        Path network = file("indexed-network.xml"), config = file("indexed-config.xml"), trips = file("indexed-trips.csv"), output = file("indexed-routes.csv");
        new NetworkWriter(scenario.getNetwork()).write(network.toString());
        new ConfigWriter(scenario.getConfig()).write(config.toString());
        PopulationRoutingIO.exportTrips(scenario, trips, Set.of("car"));
        var inputs = rows(trips);
        for (int i = 0; i < inputs.size(); i++) {
            inputs.get(i).remove("trip_id");
            inputs.get(i).put("identifier", "unrelated-row-label-" + i);
        }
        java.util.Collections.reverse(inputs); writeRows(trips, inputs);
        try (var router = new MultiModeRouter(settings(network, config, false, true))) { router.route(trips, output); }
        var reused = PopulationRoutingIO.importRoutes(scenario, output, Set.of("car"));
        assertFalse(PopulationRoutingIO.needsRouting(scenario, Set.of("car"), reused));
        var imported = TripStructureUtils.getTrips(plan);
        assertEquals(2, imported.size());
        assertEquals(400, imported.get(0).getLegsOnly().getFirst().getRoute().getDistance(), 0);
        assertEquals(100, imported.get(1).getLegsOnly().getFirst().getRoute().getDistance(), 0);
        assertEquals("end-with-dashes", imported.get(1).getLegsOnly().getFirst().getRoute().getStartLinkId().toString());
    }

    private Scenario scenario() {
        var config = ConfigUtils.createConfig();
        config.routing().setNetworkModes(Set.of("car", "bike"));
        var vdf = new VDFConfigGroup(); vdf.setBprFactor(1); vdf.setBprExponent(2); config.addModule(vdf);
        Scenario scenario = ScenarioUtils.createScenario(config);
        var net = scenario.getNetwork(); var factory = net.getFactory();
        var nodes = new ArrayList<org.matsim.api.core.v01.network.Node>();
        for (int i = 0; i < 5; i++) { var node = factory.createNode(Id.createNodeId(i), new Coord(i * 100, 0)); net.addNode(node); nodes.add(node); }
        var ids = List.of("start-with-dashes", "middle-a", "middle-b", "end-with-dashes");
        for (int i = 0; i < 4; i++) {
            var link = factory.createLink(Id.createLinkId(ids.get(i)), nodes.get(i), nodes.get(i + 1));
            link.setLength(100); link.setFreespeed(10); link.setCapacity(20); link.setNumberOfLanes(1);
            link.setAllowedModes(Set.of("car", "truck", "walk", "bike")); net.addLink(link);
        }
        var population = scenario.getPopulation(); var pf = population.getFactory();
        for (String personId : List.of("car", "car-2", "truck", "car_passenger", "walk", "bike")) {
            String mode = personId.equals("car-2") ? "car" : personId;
            var person = pf.createPerson(Id.createPersonId(personId)); population.addPerson(person);
            {
                var plan = pf.createPlan(); person.addPlan(plan);
                var from = pf.createActivityFromCoord("home", new Coord(50, 0));
                from.setLinkId(Id.createLinkId(ids.getFirst())); from.setEndTime(personId.equals("car-2") ? 200 : 100);
                from.getAttributes().putAttribute("trip_id", "custom,id-\"");
                var to = pf.createActivityFromCoord("work", new Coord(350, 0)); to.setLinkId(Id.createLinkId(ids.getLast()));
                plan.addActivity(from); var leg = pf.createLeg(mode); TripStructureUtils.setRoutingMode(leg, mode); plan.addLeg(leg); plan.addActivity(to);
            }
        }
        return scenario;
    }

    @Test
    public void absentNanMalformedAndIncompatibleRoutesAreReroutedWhileValidTripsAreReused() throws Exception {
        Scenario source = scenario();
        Path network = file("fallback-network.xml"), config = file("fallback-config.xml"), trips = file("fallback-trips.csv"), output = file("fallback-routes.csv");
        new NetworkWriter(source.getNetwork()).write(network.toString());
        new ConfigWriter(source.getConfig()).write(config.toString());
        PopulationRoutingIO.exportTrips(source, trips, Set.of());
        try (var router = new MultiModeRouter(settings(network, config, false, true))) { router.route(trips, output); }
        var originals = rows(output);
        for (String problem : List.of("absent", "missing-value", "empty", "nan", "null", "empty-array", "malformed", "version",
                "nan-time", "nan-distance", "trip-key", "coordinates", "unknown-link", "disconnected", "endpoint", "nan-leg-time",
                "wrong-leg-mode", "generic-car")) {
            List<Map<String, String>> inputs = new ArrayList<>();
            for (var original : originals) inputs.add(new java.util.LinkedHashMap<>(original));
            var row = inputs.stream().filter(v -> v.get("person_id").equals("car")).findFirst().orElseThrow();
            switch (problem) {
                case "absent" -> inputs.remove(row);
                case "missing-value" -> row.remove("route");
                case "empty" -> row.put("route", "  ");
                case "nan" -> row.put("route", "NaN");
                case "null" -> row.put("route", "null");
                case "empty-array" -> row.put("route", "[]");
                case "malformed" -> row.put("route", "invalid-json");
                case "version" -> row.put("route", "{\"version\":99}");
                case "nan-time" -> row.put("travel_time", "NaN");
                case "nan-distance" -> row.put("travel_distance", "NaN");
                case "trip-key" -> row.put("trip_id", "wrong-id");
                case "coordinates" -> row.put("origin_x", "999");
                default -> {
                    var json = new com.fasterxml.jackson.databind.ObjectMapper();
                    var root = json.readTree(row.get("route"));
                    var route = (com.fasterxml.jackson.databind.node.ObjectNode) root.path("parts").get(0).path("route");
                    switch (problem) {
                        case "unknown-link" -> ((com.fasterxml.jackson.databind.node.ArrayNode) route.path("links"))
                                .set(1, json.getNodeFactory().textNode("does-not-exist"));
                        case "disconnected" -> route.set("links", json.valueToTree(List.of("start-with-dashes", "end-with-dashes")));
                        case "endpoint" -> {
                            route.put("startLink", "middle-a");
                            route.set("links", json.valueToTree(List.of("middle-a", "middle-b", "end-with-dashes")));
                        }
                        case "nan-leg-time" -> ((com.fasterxml.jackson.databind.node.ObjectNode) root.path("parts").get(0)).put("travelTime", "NaN");
                        case "wrong-leg-mode" -> ((com.fasterxml.jackson.databind.node.ObjectNode) root.path("parts").get(0)).put("mode", "bike");
                        case "generic-car" -> route.put("type", "generic");
                        default -> throw new AssertionError(problem);
                    }
                    row.put("route", json.writeValueAsString(root));
                }
            }
            writeRows(output, inputs);
            Scenario population = scenario(); PopulationRoutingIO.enrichTrips(population);
            var reused = PopulationRoutingIO.importRoutes(population, output, Set.of());
            assertEquals(problem, 5, reused.values().stream().mapToInt(Set::size).sum());
            assertTrue(problem, PopulationRoutingIO.needsRouting(population, Set.of(), reused));
            var car = population.getPopulation().getPersons().get(Id.createPersonId("car"));
            var trip = TripStructureUtils.getTrips(car.getSelectedPlan()).getFirst();
            assertEquals(problem, Id.createLinkId("start-with-dashes"), trip.getOriginActivity().getLinkId());
            TripRouter routing = mock(TripRouter.class);
            when(routing.calcRoute(anyString(), any(), any(), anyDouble(), any(), any())).thenAnswer(call -> {
                var leg = population.getPopulation().getFactory().createLeg(call.getArgument(0)); leg.setTravelTime(321); return List.of(leg);
            });
            new PopulationRouter(2, 1, true, Set.of(), () -> new PlanRouter(population.getActivityFacilities(), routing), reused)
                    .run(population.getPopulation());
            verify(routing, times(1)).calcRoute(eq("car"), any(), any(), anyDouble(), any(), any());
        }
    }

    private MultiModeRouter.Settings settings(Path network, Path config, boolean assignment, boolean export) throws Exception {
        return MultiModeRouterServer.readSettings(MultiModeRouterServer.commandLine(new String[]{"--network-path", network.toString(),
                "--config-path", config.toString(), "--port", "8080", "--threads", "2", "--chunk-size", "2",
                "--export-routes", Boolean.toString(export), "--marouter", Boolean.toString(assignment)}));
    }
    private Path file(String name) { return folder.getRoot().toPath().resolve(name); }
    private static List<Map<String, String>> rows(Path path) throws Exception {
        var csv = new CsvMapper();
        try (MappingIterator<Map<String, String>> rows = csv.readerFor(csv.getTypeFactory().constructMapType(Map.class, String.class, String.class))
                .with(CsvSchema.emptySchema().withHeader()).readValues(path.toFile())) { return rows.readAll(); }
    }
    private static void writeRows(Path path, List<Map<String, String>> rows) throws Exception {
        var schema = CsvSchema.builder();
        var columns = new java.util.LinkedHashSet<String>(); rows.forEach(row -> columns.addAll(row.keySet())); columns.forEach(schema::addColumn);
        try (var writer = new CsvMapper().writer(schema.build().withHeader()).writeValues(path.toFile())) { writer.writeAll(rows); }
    }
}
