// Exercises the actual JNI boundary, itinerary semantics, cache composition, and concurrent
// callers.
package org.eqasim.switzerland.ch_cmdp.rustRouting;

import static org.junit.Assert.*;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.*;

import org.eqasim.core.components.raptor.*;
import org.eqasim.switzerland.ch_cmdp.routing.pt_cache.*;
import org.junit.*;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.*;

import java.util.*;
import java.util.concurrent.*;

public class TestRustRouting {
    @Test
    public void nativeSettingsAndRuntimeCommentsSurviveConfigExport() throws Exception {
        var settings = new RustRoutingConfigGroup();
        settings.enabled = true;
        settings.libraryPath = "native/router.dll";
        settings.networkPartition = true;
        settings.transferBlocks = true;
        settings.localBoundSettlements = 3;
        settings.coordinateWorkBudget = 1234;
        settings.qualityWaitSeconds = 600;
        settings.qualityJourneySeconds = 7200;
        settings.qualityRadiusMeters = 1500;
        var runtimeSettings =
                new org.eqasim.switzerland.ch_cmdp.runtime.SimulationRuntimeConfigGroup();
        runtimeSettings.setTeleportBike(true);
        var config = ConfigUtils.createConfig(settings, runtimeSettings);
        var file = java.nio.file.Files.createTempFile("rust-config-roundtrip-", ".xml");
        try {
            ConfigUtils.writeConfig(config, file.toString());
            var restored =
                    ConfigUtils.loadConfig(
                            file.toString(),
                            new RustRoutingConfigGroup(),
                            new org.eqasim.switzerland.ch_cmdp.runtime
                                    .SimulationRuntimeConfigGroup());
            var nativeSettings = ConfigUtils.addOrGetModule(restored, RustRoutingConfigGroup.class);
            assertTrue(nativeSettings.enabled);
            assertTrue(nativeSettings.networkPartition);
            assertTrue(nativeSettings.transferBlocks);
            assertEquals(3,nativeSettings.localBoundSettlements);
            assertEquals(1234, nativeSettings.coordinateWorkBudget);
            assertEquals(600, nativeSettings.qualityWaitSeconds, 0);
            assertEquals(7200, nativeSettings.qualityJourneySeconds, 0);
            assertEquals(1500, nativeSettings.qualityRadiusMeters, 0);
            assertEquals("native/router.dll", nativeSettings.libraryPath);
            assertTrue(
                    ConfigUtils.addOrGetModule(
                                    restored,
                                    org.eqasim.switzerland.ch_cmdp.runtime
                                            .SimulationRuntimeConfigGroup.class)
                            .isTeleportBike());
        } finally {
            java.nio.file.Files.delete(file);
        }
    }

    private static void requireNative() {
        Assume.assumeTrue(
                "Pass -Drust.router.test.library to exercise JNI",
                System.getProperty("rust.router.test.library") != null);
    }

    private static Scenario scenario() {
        Config config =
                ConfigUtils.createConfig(
                        new RustRoutingConfigGroup(),
                        new SwissRailRaptorConfigGroup(),
                        new EqasimRaptorConfigGroup());
        config.transit().setUseTransit(true);
        config.global().setNumberOfThreads(8);
        config.transitRouter().setSearchRadius(100);
        config.transitRouter().setExtensionRadius(100);
        config.transitRouter().setMaxBeelineWalkConnectionDistance(300);
        config.transitRouter().setAdditionalTransferTime(5);
        config.routing().getOrCreateModeRoutingParams("walk").setTeleportedModeSpeed(1.3);
        config.routing().getOrCreateModeRoutingParams("walk").setBeelineDistanceFactor(1.3);
        var raptor = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
        raptor.addParameterSet(
                new SwissRailRaptorConfigGroup.ModeToModeTransferPenalty("bus", "rail", .25));
        var settings = ConfigUtils.addOrGetModule(config, RustRoutingConfigGroup.class);
        settings.enabled = true;
        settings.libraryPath = System.getProperty("rust.router.test.library", "");
        settings.networkPartition = true;
        settings.transferBlocks = true;
        settings.localBoundSettlements = 3;
        Scenario scenario = ScenarioUtils.createScenario(config);
        var network = scenario.getNetwork();
        var factory = network.getFactory();
        for (int i = 0; i < 4; i++)
            network.addNode(factory.createNode(Id.createNodeId(i), new Coord(i * 10000, 0)));
        for (int i = 0; i < 3; i++) {
            var link =
                    factory.createLink(
                            Id.createLinkId("l" + i),
                            network.getNodes().get(Id.createNodeId(i)),
                            network.getNodes().get(Id.createNodeId(i + 1)));
            link.setLength(1000);
            link.setFreespeed(10);
            link.setCapacity(1000);
            link.setNumberOfLanes(1);
            network.addLink(link);
            var back =
                    factory.createLink(
                            Id.createLinkId("back" + i), link.getToNode(), link.getFromNode());
            back.setLength(1000);
            back.setFreespeed(10);
            back.setCapacity(1000);
            back.setNumberOfLanes(1);
            network.addLink(back);
        }
        var schedule = scenario.getTransitSchedule();
        var sf = schedule.getFactory();
        for (int i = 0; i < 3; i++) {
            var stop =
                    sf.createTransitStopFacility(
                            Id.create("s" + i, TransitStopFacility.class),
                            new Coord(i * 10000, 0),
                            false);
            stop.setLinkId(Id.createLinkId("l" + i));
            schedule.addStopFacility(stop);
        }
        addRoute(scenario, "bus", 0, 1, 100, 10);
        addRoute(scenario, "rail", 1, 2, 800, 0);
        return scenario;
    }

    private static void addRoute(
            Scenario scenario, String mode, int from, int to, double departure, double dwell) {
        var schedule = scenario.getTransitSchedule();
        var sf = schedule.getFactory();
        var first = schedule.getFacilities().get(Id.create("s" + from, TransitStopFacility.class));
        var last = schedule.getFacilities().get(Id.create("s" + to, TransitStopFacility.class));
        var path = RouteUtils.createLinkNetworkRouteImpl(first.getLinkId(), last.getLinkId());
        path.setLinkIds(first.getLinkId(), List.of(), last.getLinkId());
        var route =
                sf.createTransitRoute(
                        Id.create(mode, TransitRoute.class),
                        path,
                        List.of(
                                sf.createTransitRouteStop(first, 0, dwell),
                                sf.createTransitRouteStop(last, 600, 0)),
                        mode);
        route.addDeparture(sf.createDeparture(Id.create(mode, Departure.class), departure));
        var line = sf.createTransitLine(Id.create(mode, TransitLine.class));
        line.addRoute(route);
        schedule.addTransitLine(line);
    }

    private static RoutingRequest request(double departure) {
        return request(departure, 20000);
    }

    private static RoutingRequest request(double departure, double destinationX) {
        // Request facilities are independent of the schedule and can be used by parallel workers.
        var factory = FacilitiesUtils.createActivityFacilities().getFactory();
        var origin =
                factory.createActivityFacility(
                        Id.create("from", org.matsim.facilities.ActivityFacility.class),
                        new Coord(0, 0),
                        Id.createLinkId("l0"));
        var destination =
                factory.createActivityFacility(
                        Id.create("to", org.matsim.facilities.ActivityFacility.class),
                        new Coord(destinationX, 0),
                        Id.createLinkId("l2"));
        return DefaultRoutingRequest.of(
                origin,
                destination,
                departure,
                PopulationUtils.getFactory().createPerson(Id.createPersonId("person")),
                null);
    }

    @Test
    public void limitedCoordinateSearchMarksFeasibleLegsAndResolvesMissingWitnessExactly() {
        requireNative();
        var scenario = scenario();
        var settings = ConfigUtils.addOrGetModule(scenario.getConfig(), RustRoutingConfigGroup.class);
        settings.coordinateWorkBudget = 1;
        settings.qualityWaitSeconds = 0;
        settings.qualityJourneySeconds = 0;
        settings.qualityRadiusMeters = 0;
        try (var runtime = new RustRoutingRuntime(scenario, settings)) {
            runtime.prepared();
            assertTrue(NativePtRouter.hasCoordinatePolicy);
            var router = new RustPtRoutingModule(runtime, null);
            var direct = router.transitLegs(request(90, 10000));
            assertNotNull(direct);
            assertTrue(direct.stream().anyMatch(leg -> Boolean.TRUE.equals(
                    leg.getAttributes().getAttribute("rustRoutingBudgetLimited"))));
            // One work unit cannot reach the second boarding: resolve rather than return no PT.
            var connecting = router.transitLegs(request(90));
            assertNotNull(connecting);
            assertFalse(connecting.stream().anyMatch(leg -> Boolean.TRUE.equals(
                    leg.getAttributes().getAttribute("rustRoutingBudgetLimited"))));
        }
    }

    @Test
    public void preservesDwellWaitingTransfersDistancesAndIndependentConcurrentRoutes()
            throws Exception {
        requireNative();
        var scenario = scenario();
        var settings =
                ConfigUtils.addOrGetModule(scenario.getConfig(), RustRoutingConfigGroup.class);
        try (var runtime = new RustRoutingRuntime(scenario, settings)) {
            var router =
                    new RustPtRoutingModule(
                            runtime, ignored -> List.of(PopulationUtils.createLeg("walk")));
            var trip = router.calcRoute(request(90));
            var rides =
                    trip.stream()
                            .filter(
                                    p ->
                                            p instanceof Leg l
                                                    && l.getRoute()
                                                            instanceof TransitPassengerRoute)
                            .map(p -> (Leg) p)
                            .toList();
            assertEquals(2, rides.size());
            assertEquals("pt", rides.getFirst().getMode());
            assertEquals("pt", rides.getLast().getMode());
            assertEquals(90, rides.getFirst().getDepartureTime().seconds(), 0);
            assertEquals(610, rides.getFirst().getTravelTime().seconds(), .001);
            assertEquals(
                    100,
                    ((TransitPassengerRoute) rides.getFirst().getRoute())
                            .getBoardingTime()
                            .seconds(),
                    .001);
            assertEquals(1000, rides.getFirst().getRoute().getDistance(), 0);
            assertEquals(705, rides.getLast().getDepartureTime().seconds(), .001);
            assertEquals(695, rides.getLast().getTravelTime().seconds(), .001);
            assertEquals(1000, rides.getLast().getRoute().getDistance(), 0);
            double expectedCost = (1200 * 7 + 105 * 6 + 5 * 7) / 3600. + 1.25;
            assertEquals(
                    expectedCost,
                    (Double) rides.getFirst().getAttributes().getAttribute("totalRouteCost"),
                    1e-9);
            var pool = Executors.newFixedThreadPool(8);
            try {
                List<Callable<List<? extends PlanElement>>> tasks = new ArrayList<>();
                for (int i = 0; i < 120; i++) tasks.add(() -> router.calcRoute(request(90)));
                for (var result : pool.invokeAll(tasks)) {
                    var elements = result.get();
                    assertEquals(trip.size(), elements.size());
                    assertNotSame(trip.getFirst(), elements.getFirst());
                    assertEquals(
                            1400,
                            elements.stream()
                                            .filter(
                                                    p ->
                                                            p instanceof Leg l
                                                                    && l.getRoute()
                                                                            instanceof
                                                                            TransitPassengerRoute)
                                            .map(p -> (Leg) p)
                                            .reduce((a, b) -> b)
                                            .orElseThrow()
                                            .getDepartureTime()
                                            .seconds()
                                    + 695,
                            .001);
                }
            } finally {
                pool.shutdownNow();
            }
            assertNull(router.transitLegs(request(2000)));
            assertEquals("walk", ((Leg) router.calcRoute(request(2000)).getFirst()).getMode());
            long handle = runtime.prepared().handle();
            runtime.close();
            runtime.close();
            assertThrows(
                    IllegalStateException.class,
                    () ->
                            NativePtRouter.route(
                                    handle,
                                    90000,
                                    4,
                                    10000000,
                                    new int[] {0, 0},
                                    new int[] {2, 0}));
        }
    }

    @Test
    public void coordinateBoundaryMatchesExplicitEndpointsAndBoundsFailedSearchExpansion() {
        requireNative();
        var scenario = scenario();
        var settings = ConfigUtils.addOrGetModule(scenario.getConfig(), RustRoutingConfigGroup.class);
        try (var runtime = new RustRoutingRuntime(scenario, settings)) {
            var ready = runtime.prepared();
            double[] expected = NativePtRouter.route(ready.handle(), 90000, settings.maxBoardings,
                    NativePtRouter.millis(settings.maxDurationSeconds), new int[] {0, 0}, new int[] {2, 0});
            double[] actual = NativePtRouter.routeCoordinates(ready.handle(), 90000, settings.maxBoardings,
                    NativePtRouter.millis(settings.maxDurationSeconds), 0, 0, 20000, 0);
            assertEquals(1, actual[0], 0);
            assertArrayEquals(expected, Arrays.copyOfRange(actual, 1, actual.length), 0);
            double[] missed = NativePtRouter.routeCoordinates(ready.handle(), 2000000, settings.maxBoardings,
                    NativePtRouter.millis(settings.maxDurationSeconds), 0, 0, 20000, 0);
            assertEquals(1, missed.length);
            assertEquals(5, missed[0], 0);
        }
    }

    @Test
    public void coordinatesUseEffectiveRaptorWalkingSpeedAndStopAccessEgressTimes() throws Exception {
        requireNative();
        var scenario = scenario();
        // TransitRouterConfig prefers transit_walk, then non_network_walk, then walk.
        scenario.getConfig().routing().getOrCreateModeRoutingParams("walk").setTeleportedModeSpeed(0.7);
        var walk = scenario.getConfig().routing().getOrCreateModeRoutingParams("transit_walk");
        walk.setTeleportedModeSpeed(2.6);
        walk.setBeelineDistanceFactor(1.3);
        var stops = scenario.getTransitSchedule().getFacilities();
        stops.get(Id.create("s0", TransitStopFacility.class)).getAttributes().putAttribute("accessTime", 7.25);
        stops.get(Id.create("s2", TransitStopFacility.class)).getAttributes().putAttribute("egressTime", 3.5);
        var settings = ConfigUtils.addOrGetModule(scenario.getConfig(), RustRoutingConfigGroup.class);
        try (var runtime = new RustRoutingRuntime(scenario, settings)) {
            var ready = runtime.prepared();
            assertTrue(NativePtRouter.hasCoordinates);
            assertEquals(2.0, ready.snapshot().parameters.getBeelineWalkSpeed(), 1e-12);
            var exported = new com.fasterxml.jackson.databind.ObjectMapper().readTree(ready.snapshot().costsJson());
            assertEquals(ready.snapshot().parameters.getBeelineWalkSpeed(),
                    exported.get("_coordinates").get("speed").asDouble(), 0);
            double[] expected = NativePtRouter.route(ready.handle(), 80000, settings.maxBoardings,
                    NativePtRouter.millis(settings.maxDurationSeconds), new int[] {0, 9250}, new int[] {2, 6500});
            double[] actual = NativePtRouter.routeCoordinates(ready.handle(), 80000, settings.maxBoardings,
                    NativePtRouter.millis(settings.maxDurationSeconds), 3, 0, 20005, 0);
            assertTrue(expected.length > 0);
            assertArrayEquals(expected, Arrays.copyOfRange(actual, 1, actual.length), 0);
            assertEquals(9250, actual[3], 0);
            assertEquals(6500, actual[5], 0);
        }
    }

    @Test
    public void composesWithControllerAndDiskCache() throws Exception {
        requireNative();
        var scenario = scenario();
        var cache = ConfigUtils.addOrGetModule(scenario.getConfig(), PtRouteCacheConfigGroup.class);
        cache.enabled = true;
        scenario.getConfig()
                .controller()
                .setOutputDirectory(
                        java.nio.file.Files.createTempDirectory("rust-routing-injector-")
                                .toString());
        try (var runtime =
                new RustRoutingRuntime(
                        scenario,
                        ConfigUtils.addOrGetModule(
                                scenario.getConfig(), RustRoutingConfigGroup.class))) {
            var controller = new Controler(scenario);
            controller.addOverridingModule(new SwissRailRaptorModule());
            controller.addOverridingModule(new EqasimRaptorModule());
            controller.addOverridingModule(new RustRoutingModule(runtime));
            controller.addOverridingModule(new PtRouteCacheModule());
            var injector = controller.getInjector();
            try (var store = injector.getInstance(PtRouteCache.class)) {
                var router = injector.getInstance(TripRouter.class);
                var request = request(90);
                var first =
                        router.calcRoute(
                                "pt",
                                request.getFromFacility(),
                                request.getToFacility(),
                                90,
                                request.getPerson(),
                                request.getAttributes());
                var second =
                        router.calcRoute(
                                "pt",
                                request.getFromFacility(),
                                request.getToFacility(),
                                90,
                                request.getPerson(),
                                request.getAttributes());
                assertEquals(first.size(), second.size());
                assertNotSame(first.getFirst(), second.getFirst());
                assertTrue(
                        first.stream()
                                .anyMatch(
                                        p ->
                                                p instanceof Leg l
                                                        && l.getRoute()
                                                                instanceof TransitPassengerRoute));
                assertEquals(1, store.statistics().misses());
                assertEquals(1, store.statistics().memoryHits());
            }
        }
    }

    @Test
    public void rejectsUnsupportedConfigAndInvalidChronology() {
        var scenario = scenario();
        var settings =
                ConfigUtils.addOrGetModule(scenario.getConfig(), RustRoutingConfigGroup.class);
        settings.maxBoardings = 0;
        assertThrows(
                IllegalArgumentException.class,
                () -> RustRoutingModule.validate(scenario.getConfig(), settings));
        settings.maxBoardings = 64;
        settings.qualityWaitSeconds = Double.NaN;
        assertThrows(IllegalArgumentException.class,
                () -> RustRoutingModule.validate(scenario.getConfig(), settings));
        settings.qualityWaitSeconds = 1800;
        ConfigUtils.addOrGetModule(scenario.getConfig(), SwissRailRaptorConfigGroup.class)
                .setUseRangeQuery(true);
        assertThrows(
                IllegalArgumentException.class,
                () -> RustRoutingModule.validate(scenario.getConfig(), settings));
        ConfigUtils.addOrGetModule(scenario.getConfig(), SwissRailRaptorConfigGroup.class)
                .setUseRangeQuery(false);
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    addRoute(scenario, "invalid", 0, 1, 100, -1);
                    var snapshot = new RustTimetableSnapshot(scenario);
                    var file = java.nio.file.Files.createTempFile("invalid-rust-test-", ".bin");
                    try {
                        snapshot.write(file);
                    } finally {
                        java.nio.file.Files.delete(file);
                    }
                });
    }

    @Test
    public void genevaObservedTripsThroughNativeAdapterWhenRequested() throws Exception {
        requireNative();
        String configPath = System.getProperty("rust.router.test.geneva.config");
        String benchmark = System.getProperty("rust.router.test.geneva.benchmark");
        Assume.assumeTrue(configPath != null && benchmark != null);
        var config =
                ConfigUtils.loadConfig(
                        configPath,
                        new RustRoutingConfigGroup(),
                        new EqasimRaptorConfigGroup(),
                        new SwissRailRaptorConfigGroup());
        var scenario = ScenarioUtils.createScenario(config);
        new org.matsim.core.network.io.MatsimNetworkReader(scenario.getNetwork())
                .readFile(
                        ConfigGroup.getInputFileURL(
                                        config.getContext(), config.network().getInputFile())
                                .toString());
        var base = java.nio.file.Path.of(benchmark);
        new org.matsim.pt.transitSchedule.api.TransitScheduleReader(scenario)
                .readFile(base.resolve("benchmark_schedule.xml.gz").toString());
        var settings = ConfigUtils.addOrGetModule(config, RustRoutingConfigGroup.class);
        settings.enabled = true;
        settings.libraryPath = System.getProperty("rust.router.test.library");
        // This historical reference exercises exact legacy candidate selection.
        settings.coordinateWorkBudget = 0;
        settings.qualityWaitSeconds = 0;
        settings.qualityJourneySeconds = 0;
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var requests = java.nio.file.Files.readAllLines(base.resolve("requests.jsonl"));
        var reference = java.nio.file.Files.readAllLines(base.resolve("rust_results.jsonl"));
        assertEquals(1000, requests.size());
        try (var runtime = new RustRoutingRuntime(scenario, settings)) {
            runtime.prepared();
            var router = new RustPtRoutingModule(runtime, null);
            var pool = Executors.newFixedThreadPool(8);
            try {
                List<Callable<Void>> tasks = new ArrayList<>();
                for (int i = 0; i < requests.size(); i++) {
                    var input = mapper.readTree(requests.get(i));
                    var expected = mapper.readTree(reference.get(i)).get("journey");
                    tasks.add(
                            () -> {
                                var factory =
                                        FacilitiesUtils.createActivityFacilities().getFactory();
                                var origin = input.get("origin");
                                var destination = input.get("destination");
                                var link =
                                        scenario.getNetwork().getLinks().keySet().iterator().next();
                                var from =
                                        factory.createActivityFacility(
                                                Id.create(
                                                        "from",
                                                        org.matsim.facilities.ActivityFacility
                                                                .class),
                                                new Coord(
                                                        origin.get(0).asDouble(),
                                                        origin.get(1).asDouble()),
                                                link);
                                var to =
                                        factory.createActivityFacility(
                                                Id.create(
                                                        "to",
                                                        org.matsim.facilities.ActivityFacility
                                                                .class),
                                                new Coord(
                                                        destination.get(0).asDouble(),
                                                        destination.get(1).asDouble()),
                                                link);
                                double departure =
                                        org.matsim.core.utils.misc.Time.parseTime(
                                                input.get("departure").asText());
                                var request =
                                        DefaultRoutingRequest.withoutAttributes(
                                                from, to, departure, null);
                                var result = router.transitLegs(request);
                                assertNotNull("No native route for " + input.get("id"), result);
                                var rides =
                                        result.stream()
                                                .filter(
                                                        e ->
                                                                e instanceof Leg l
                                                                        && l.getRoute()
                                                                                instanceof
                                                                                TransitPassengerRoute)
                                                .map(e -> (Leg) e)
                                                .toList();
                                assertFalse(rides.isEmpty());
                                double cost =
                                        (Double)
                                                rides.getFirst()
                                                        .getAttributes()
                                                        .getAttribute("totalRouteCost");
                                // Larger boarding limit may improve a path; it must never worsen
                                // the reference optimum.
                                assertTrue(
                                        "Cost differs for "
                                                + input.get("id")
                                                + ": "
                                                + cost
                                                + " > "
                                                + expected.get("generalized_cost"),
                                        cost <= expected.get("generalized_cost").asDouble() + 1e-8);
                                for (var ride : rides) {
                                    assertTrue(Double.isFinite(ride.getRoute().getDistance()));
                                    assertTrue(ride.getRoute().getDistance() >= 0);
                                    assertTrue(ride.getTravelTime().seconds() >= 0);
                                }
                                return null;
                            });
                }
                for (var future : pool.invokeAll(tasks)) future.get();
            } finally {
                pool.shutdownNow();
            }
        }
    }
}
