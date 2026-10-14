package org.eqasim.switzerland.ch_cmdp.utils.routing;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.switzerland.ch_cmdp.config.SwissPTZonesConfigGroup;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.DefaultDistanceBasedCalculator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.SwissPtStageCostCalculator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtVariables;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.RunComputeTransitPrices;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.RunComputeTransitPrices.CSVRequest;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

public class TestMultiModeRouterCosts {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private static final String HEADER = "identifier,origin_x,origin_y,destination_x,destination_y,mode\n";

    @Test
    public void simpleWeissAndFreeModesUseTheSelectedModels() throws Exception {
        Path network = network();
        Config config = ConfigUtils.createConfig();
        config.routing().setNetworkModes(Set.of("car"));
        Path parameters = write("costs.yml", "carCost_CHF_km: 0.4\n");
        var eqasim = new EqasimConfigGroup();
        eqasim.setCostParametersPath(parameters.getFileName().toString());
        config.addModule(eqasim);
        Path configPath = writeConfig(config);
        Path input = write("modes.csv", HEADER + "car,0,0,2000,0,car\npassenger,0,0,2000,0,car_passenger\n"
                + "bike,0,0,2000,0,bike\nwalk,0,0,2000,0,walk\n");
        Path output = folder.getRoot().toPath().resolve("out.csv");
        var simple = settings(network, configPath, "--compute-cost", "true", "--car-cost-model", "simple");
        try (var router = new MultiModeRouter(simple)) {
            router.route(input, output);
            var results = rows(output);
            assertEquals(0.8, cost(results.get(0)), 1e-9);
            for (int i = 1; i < 4; i++) assertEquals(0, cost(results.get(i)), 0);
            assertEquals("car_passenger", results.get(1).get("mode"));
            assertEquals(results.get(0).get("travel_distance"), results.get(1).get("travel_distance"));
            Path distances = write("saved-distances.csv", "identifier,mode,travel_distance\n"
                    + "car,car,2000\npassenger,car_passenger,\nbike,bike,\nwalk,walk,\n");
            Path costs = folder.getRoot().toPath().resolve("saved-costs.csv");
            assertEquals(4, router.cost(distances, costs));
            var prices = rows(costs);
            assertEquals(Set.of("identifier", "mode", "cost"), prices.get(0).keySet());
            assertEquals(0.8, cost(prices.get(0)), 1e-9);
            for (int i = 1; i < 4; i++) assertEquals(0, cost(prices.get(i)), 0);
            byte[] preserved = Files.readAllBytes(costs);
            Files.writeString(distances, "identifier,mode,travel_distance\nbad,car,-1\n");
            assertThrows(IllegalArgumentException.class, () -> router.cost(distances, costs));
            assertArrayEquals(preserved, Files.readAllBytes(costs));
        }
        var weiss = settings(network, configPath, "--compute-cost", "true", "--car-cost-model", "weiss");
        try (var router = new MultiModeRouter(weiss)) {
            router.route(input, output);
            assertEquals(2 * Math.min(0.104 + 0.6 * Math.exp(-1.2 * Math.pow(2, 0.33)), 0.3),
                    cost(rows(output).get(0)), 1e-9);
        }
        var disabled = settings(network, configPath);
        try (var router = new MultiModeRouter(disabled)) {
            router.route(input, output);
            assertEquals("", rows(output).get(0).get("cost"));
            assertThrows(IllegalArgumentException.class, () -> router.cost(input, output));
            assertThrows(IllegalArgumentException.class, () -> router.route(input, output, "car", true));
        }
    }

    @Test
    public void configSelectsCostsButCliWinsIncludingExplicitFalse() throws Exception {
        Path network = network();
        Config config = ConfigUtils.createConfig();
        var eqasim = new EqasimConfigGroup();
        eqasim.setCostModel("car", "SwissWeissCarCostModel");
        config.addModule(eqasim);
        var routerConfig = new org.matsim.core.config.ConfigGroup("multiModeRouter");
        routerConfig.addParam("computeCost", "true");
        config.addModule(routerConfig);
        Path configPath = writeConfig(config);
        var fromConfig = settings(network, configPath);
        fromConfig.applyConfigDefaults(MultiModeRouter.loadConfig(fromConfig));
        assertTrue(fromConfig.computeCost);
        assertEquals("weiss", fromConfig.carCostModel);
        var explicit = settings(network, configPath, "--compute-cost", "false", "--car-cost-model", "simple");
        explicit.applyConfigDefaults(MultiModeRouter.loadConfig(explicit));
        assertFalse(explicit.computeCost);
        assertEquals("simple", explicit.carCostModel);
        routerConfig.addParam("carCostModel", "simple");
        configPath = writeConfig(config);
        var moduleOverride = settings(network, configPath);
        moduleOverride.applyConfigDefaults(MultiModeRouter.loadConfig(moduleOverride));
        assertEquals("simple", moduleOverride.carCostModel);
    }

    @Test
    public void ptPricesUseLoadedFareInputsAndOptionalTravelerColumns() throws Exception {
        Path network = network();
        Config config = transitConfig();
        Path zones = write("zones.csv", "stop_id,tarif network,zones\nstart,[],\nend,[],\n");
        Path distances = write("distances.csv", "origin_id,destination_id,distance\n");
        Path pricing = write("pricing.xml", """
                <pricing><distanceBasedDefault>
                  <flatRatePrice value="4"/><pricePerKM value="2"/><power2Factor value="0"/>
                </distanceBasedDefault></pricing>
                """);
        var ptZones = new SwissPTZonesConfigGroup();
        ptZones.setZonePath(zones.getFileName().toString());
        ptZones.setSBBDistancesPath(distances.getFileName().toString());
        ptZones.setPricingDescriptionPath(pricing.getFileName().toString());
        config.addModule(ptZones);
        Path configPath = writeConfig(config);
        String header = "identifier,origin_x,origin_y,destination_x,destination_y,mode,hasGA,hasHalbtaxSubscription,hasVerbundAbo,homeX,homeY,age\n";
        Path input = write("pt.csv", header + "full,500,0,1500,0,pt,,,,,,\n"
                + "half,500,0,1500,0,pt,false,true,false,,,30\n"
                + "ga,500,0,1500,0,pt,true,false,false,,,30\n"
                + "regional,500,0,1500,0,pt,false,false,true,0,0,30\n"
                + "child,500,0,1500,0,pt,false,false,false,,,5\n"
                + "walkonly,500,0,500,0,pt,false,false,false,,,30\n");
        Path output = folder.getRoot().toPath().resolve("pt-out.csv");
        var settings = settings(network, configPath, "--compute-cost", "true", "--batch-size", "1");
        try (var router = new MultiModeRouter(settings)) {
            router.route(input, output);
            var results = rows(output);
            assertEquals(6, cost(results.get(0)), 0);
            assertEquals(3, cost(results.get(1)), 0);
            for (int i = 2; i < results.size(); i++) assertEquals(0, cost(results.get(i)), 0);
            byte[] first = Files.readAllBytes(output);
            Path routesOnly = folder.getRoot().toPath().resolve("pt-routes-only.csv");
            router.route(input, routesOnly, null, false);
            var savedRoutes = rows(routesOnly);
            for (var saved : savedRoutes) {
                assertEquals("", saved.get("cost"));
                assertFalse(saved.get("pt_cost_data").isBlank());
            }
            // Keep only saved itinerary data: a new routing search would require the missing coordinates.
            Path savedInput = folder.newFile("pt-saved.csv").toPath();
            var schema = CsvSchema.builder().addColumn("identifier").addColumn("mode")
                    .addColumn("pt_cost_data").build().withHeader();
            try (var writer = new CsvMapper().writer(schema).writeValues(savedInput.toFile())) {
                for (var saved : savedRoutes) {
                    writer.write(Map.of("identifier", saved.get("identifier"), "mode", "pt",
                            "pt_cost_data", saved.get("pt_cost_data")));
                }
            }
            Path costs = folder.getRoot().toPath().resolve("pt-costs-only.csv");
            assertEquals(savedRoutes.size(), router.cost(savedInput, costs));
            var separate = rows(costs);
            for (int i = 0; i < results.size(); i++) {
                assertEquals(cost(results.get(i)), cost(separate.get(i)), 0);
            }
            router.cost(input, costs, "pt"); // Raw OD inputs are also supported.
            var raw = rows(costs);
            for (int i = 0; i < results.size(); i++) assertEquals(cost(results.get(i)), cost(raw.get(i)), 0);
            Files.delete(zones); Files.delete(distances); Files.delete(pricing); Files.delete(configPath);
            router.cost(savedInput, costs);
            assertEquals(6, cost(rows(costs).get(0)), 0);
            router.route(input, output);
            assertArrayEquals(first, Files.readAllBytes(output));
        }
    }

    @Test
    public void pricingIsNotLoadedWhenDisabledAndIncompletePricingFailsWhenEnabled() throws Exception {
        Path network = network();
        Path configPath = writeConfig(transitConfig());
        try (var router = new MultiModeRouter(settings(network, configPath))) {
            assertTrue(router.getAvailableModes().contains("pt"));
        }
        var enabled = settings(network, configPath, "--compute-cost", "true");
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new MultiModeRouter(enabled))
                .getMessage().contains("ptZones"));
        var invalid = settings(network, null, "--compute-cost", "true", "--car-cost-model", "invalid");
        assertThrows(IllegalArgumentException.class, () -> new MultiModeRouter(invalid));
    }

    @Test
    public void sharedFareRulesRespectCapsChildDiscountAndRegionalKilometers() {
        var vars = new SwissPtVariables();
        vars.addStage(new SwissPtLegVariables(Map.of(), 0, 100, 100_000, -1, "start", "end", "start", "end"));
        var calculators = new SwissPtStageCostCalculator(Map.of("None", new DefaultDistanceBasedCalculator(2, 4, 0)));
        assertEquals(60, RunComputeTransitPrices.calculatePrices(request(30, false), vars, calculators, 10).price(), 0);
        assertEquals(35, RunComputeTransitPrices.calculatePrices(request(30, true), vars, calculators, 10).price(), 0);
        assertEquals(35, RunComputeTransitPrices.calculatePrices(request(15, false), vars, calculators, 10).price(), 0);
        assertEquals(0, RunComputeTransitPrices.calculatePrices(request(5, false), vars, calculators, 10).price(), 0);
        assertEquals(1.5, RunComputeTransitPrices.calculateHomeDistance_km(request(30, false)), 0);
    }

    @Test
    public void serverOffersCostEndpointAndPerRequestRoutingCosts() throws Exception {
        Path network = network();
        var settings = settings(network, null, "--compute-cost", "true");
        Path input = write("http-trip.csv", HEADER + "trip,0,0,2000,0,car\n");
        Path output = folder.getRoot().toPath().resolve("http-route.csv");
        Path costs = folder.getRoot().toPath().resolve("http-costs.csv");
        try (var server = new MultiModeRouterServer(new MultiModeRouter(settings), "127.0.0.1", 0)) {
            server.start();
            var client = java.net.http.HttpClient.newHttpClient();
            var json = new com.fasterxml.jackson.databind.ObjectMapper();
            String base = "http://127.0.0.1:" + server.getPort();
            var route = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/route"))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of(
                            "inputCsv", input.toString(), "outputCsv", output.toString(), "computeCost", false))))
                    .build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(route.body(), 200, route.statusCode());
            assertEquals("", rows(output).get(0).get("cost"));
            var costResponse = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/cost"))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of(
                            "inputCsv", output.toString(), "outputCsv", costs.toString()))))
                    .build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(costResponse.body(), 200, costResponse.statusCode());
            assertEquals(0.52, cost(rows(costs).get(0)), 1e-9);
            assertEquals(1, server.cost(output.toString(), costs.toString(), "car"));
            var health = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/health"))
                    .GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            assertTrue(json.readTree(health.body()).get("costReady").asBoolean());
        }
    }

    private static CSVRequest request(int age, boolean halfFare) {
        return new CSVRequest("trip", 500, 0, 1500, 0, 0, 0, 0, false, halfFare, false, false, false, false, age);
    }

    private MultiModeRouter.Settings settings(Path network, Path config, String... options) throws Exception {
        List<String> args = new ArrayList<>(List.of("--network-path", network.toString(), "--port", "8080", "--threads", "3"));
        if (config != null) args.addAll(List.of("--config-path", config.toString()));
        args.addAll(List.of(options));
        return MultiModeRouterServer.readSettings(MultiModeRouterServer.commandLine(args.toArray(String[]::new)));
    }

    private Config transitConfig() throws Exception {
        Config config = ConfigUtils.createConfig();
        ConfigUtils.addOrGetModule(config, ch.sbb.matsim.config.SwissRailRaptorConfigGroup.class)
                .setIntermodalLegOnlyHandling(ch.sbb.matsim.config.SwissRailRaptorConfigGroup.IntermodalLegOnlyHandling.allow);
        config.transit().setUseTransit(true);
        config.transitRouter().setSearchRadius(100);
        config.transitRouter().setExtensionRadius(100);
        var schedule = ScenarioUtils.createScenario(config).getTransitSchedule();
        var factory = schedule.getFactory();
        var start = factory.createTransitStopFacility(Id.create("start", TransitStopFacility.class), new Coord(500, 0), false);
        var end = factory.createTransitStopFacility(Id.create("end", TransitStopFacility.class), new Coord(1500, 0), false);
        start.setName("start"); end.setName("end");
        start.setLinkId(Id.createLinkId("first")); end.setLinkId(Id.createLinkId("second"));
        schedule.addStopFacility(start); schedule.addStopFacility(end);
        var route = factory.createTransitRoute(Id.create("route", org.matsim.pt.transitSchedule.api.TransitRoute.class),
                RouteUtils.createLinkNetworkRouteImpl(start.getLinkId(), List.of(), end.getLinkId()),
                List.of(factory.createTransitRouteStop(start, 0, 0), factory.createTransitRouteStop(end, 100, 100)), "bus");
        route.addDeparture(factory.createDeparture(Id.create("dep", org.matsim.pt.transitSchedule.api.Departure.class), 100));
        var line = factory.createTransitLine(Id.create("line", org.matsim.pt.transitSchedule.api.TransitLine.class));
        line.addRoute(route); schedule.addTransitLine(line);
        Path path = folder.newFile().toPath();
        new TransitScheduleWriter(schedule).writeFile(path.toString());
        config.transit().setTransitScheduleFile(path.toString());
        return config;
    }

    private Path network() throws Exception {
        Network network = NetworkUtils.createNetwork();
        var factory = network.getFactory();
        var a = factory.createNode(Id.createNodeId("a"), new Coord(0, 0));
        var b = factory.createNode(Id.createNodeId("b"), new Coord(1000, 0));
        var c = factory.createNode(Id.createNodeId("c"), new Coord(2000, 0));
        List.of(a, b, c).forEach(network::addNode);
        for (var link : List.of(factory.createLink(Id.createLinkId("first"), a, b), factory.createLink(Id.createLinkId("second"), b, c))) {
            link.setLength(1000); link.setFreespeed(10); link.setCapacity(3600); link.setNumberOfLanes(1);
            link.setAllowedModes(Set.of("car", "walk", "bike")); network.addLink(link);
        }
        Path path = folder.newFile().toPath(); new NetworkWriter(network).write(path.toString()); return path;
    }

    private Path writeConfig(Config config) throws Exception {
        Path path = folder.newFile().toPath(); new ConfigWriter(config).write(path.toString()); return path;
    }

    private Path write(String name, String content) throws Exception {
        Path path = folder.newFile(name).toPath(); Files.writeString(path, content); return path;
    }

    private static List<Map<String, String>> rows(Path path) throws Exception {
        CsvMapper mapper = new CsvMapper();
        try (MappingIterator<Map<String, String>> iterator = mapper.readerFor(
                mapper.getTypeFactory().constructMapType(Map.class, String.class, String.class))
                .with(CsvSchema.emptySchema().withHeader()).readValues(path.toFile())) {
            return iterator.readAll();
        }
    }

    private static double cost(Map<String, String> row) { return Double.parseDouble(row.get("cost")); }
}
