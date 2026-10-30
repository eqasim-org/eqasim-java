package org.eqasim.core.components.network_calibration;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.flow.FlowConfigGroup;
import org.eqasim.core.components.flow.FlowModule;
import org.eqasim.core.components.network_calibration.freespeed_calibration.TripsHandler;
import org.eqasim.core.components.routing.speedy.MemoryEfficientRoutingModule;
import org.eqasim.core.components.routing.speedy.MemoryEfficientSpeedyALTFactory;
import org.eqasim.core.components.travel_time.FloatTravelTimeModule;
import org.eqasim.core.scenario.routing.PlanRouter;
import org.eqasim.core.simulation.policies.routing.RoutingPenalty;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleUtils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/** Covers the bindings shared by freespeed, cost and subpopulation calibration. */
public class TestNetworkCalibrationRouting {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void calibrationProvidersAndParallelPlanRoutersInheritOptimizedSharedTimes() throws Exception {
        var calibration = new NetworkCalibrationConfigGroup();
        calibration.setActivate(true);
        calibration.setObjective("penalty,freespeed,agent,subpopulations");
        calibration.setCalibrate("freespeed");
        calibration.setCorrectCapacities(false);
        var freespeed = calibration.getFreeSpeedCalibrationConfigGroup();
        freespeed.setWarmupIterations(10); // Run calibration passes explicitly below.
        freespeed.setMinTripDistance(1);
        freespeed.setMinTripTravelTime(1);
        var observedTrips = temporary.newFile("observed.csv").toPath();
        Files.writeString(observedTrips,
                "identifier,departure_x,departure_y,arrival_x,arrival_y,departure_time,travel_time,traveled_distance\n"
                + "1,0,0,5000,0,100,800,4000\n2,0,0,5000,0,100,800,4000\n");
        freespeed.setObservedTripsFile(observedTrips.toString());
        var eqasim = new EqasimConfigGroup();
        eqasim.setRoutingDistanceUtility(0);
        eqasim.setSigmaRoutingRandomness(0);
        var config = ConfigUtils.createConfig(eqasim, calibration, new FlowConfigGroup());
        config.controller().setRoutingAlgorithmType(RoutingAlgorithmType.SpeedyALT);
        config.controller().setOutputDirectory(temporary.newFolder("simulation").getAbsolutePath());
        config.controller().setLastIteration(0);
        config.global().setNumberOfThreads(2);
        config.qsim().setNumberOfThreads(1);
        config.qsim().setEndTime(1);
        config.routing().setNetworkModes(List.of("car", "truck", "car_passenger", "bike"));
        config.routing().removeModeRoutingParams("bike");
        config.travelTimeCalculator().setSeparateModes(false);
        config.travelTimeCalculator().setFilterModes(false);
        config.travelTimeCalculator().setTraveltimeBinSize(900);
        config.travelTimeCalculator().setMaxTime(3600);
        var scenario = ScenarioUtils.createScenario(config);
        var network = scenario.getNetwork();
        var nodes = new ArrayList<org.matsim.api.core.v01.network.Node>();
        for (int i = 0; i < 6; i++) nodes.add(NetworkUtils.createAndAddNode(network,
                Id.createNodeId(i), new Coord(i * 1000, i == 3 ? 1000 : 0)));
        for (int[] edge : List.of(new int[]{0, 1}, new int[]{1, 2}, new int[]{2, 4}, new int[]{2, 1},
                new int[]{1, 3}, new int[]{3, 4}, new int[]{4, 5}, new int[]{5, 0}, new int[]{5, 2})) {
            Link link = NetworkUtils.createAndAddLink(network, Id.createLinkId(edge[0] + "_" + edge[1]),
                    nodes.get(edge[0]), nodes.get(edge[1]), 1000, 10, 3600, 1);
            link.setAllowedModes(Set.of("car", "truck", "car_passenger", "bike"));
            link.getAttributes().putAttribute("osm:way:highway", "primary");
            link.getAttributes().putAttribute("municipalityType", "urban");
        }
        var restrictions = new DisallowedNextLinks();
        for (String mode : List.of("car", "truck", "car_passenger")) {
            restrictions.addDisallowedLinkSequence(mode, List.of(Id.createLinkId("2_4")));
        }
        NetworkUtils.setDisallowedNextLinks(network.getLinks().get(Id.createLinkId("1_2")), restrictions);
        var vehicleTypes = new java.util.HashMap<String, org.matsim.vehicles.VehicleType>();
        for (String mode : List.of("car", "truck", "car_passenger")) {
            var type = VehicleUtils.createVehicleType(Id.create(mode, org.matsim.vehicles.VehicleType.class));
            type.setNetworkMode(mode);
            scenario.getVehicles().addVehicleType(type);
            vehicleTypes.put(mode, type);
        }
        for (int i = 0; i < 12; i++) {
            String mode = List.of("car", "truck", "car_passenger").get(i % 3);
            scenario.getVehicles().addVehicle(VehicleUtils.createVehicle(
                    Id.createVehicleId(i), vehicleTypes.get(mode)));
        }
        TravelTime bike = (link, time, person, vehicle) -> 321;
        var controller = new Controler(scenario);
        // Same override order as RunSimulation: floats, normal modules, optimized router.
        controller.addOverridingModule(new FloatTravelTimeModule());
        controller.addOverridingModule(new FlowModule());
        controller.addOverridingModule(new NetworkCalibrationModule());
        controller.addOverridingModule(new AbstractModule() {
            @Override public void install() {
                bind(TripListConverter.class).toInstance(mock(TripListConverter.class));
                bind(RoutingPenalty.class).toInstance((link, person, time, cost) -> 0);
                addTravelTimeBinding("bike").toInstance(bike);
            }
        });
        controller.addOverridingModule(new MemoryEfficientRoutingModule());
        controller.run();
        var injector = controller.getInjector();
        var factory = injector.getInstance(LeastCostPathCalculatorFactory.class);
        assertTrue(factory instanceof MemoryEfficientSpeedyALTFactory);
        assertSame(factory, injector.getInstance(LeastCostPathCalculatorFactory.class));
        var registry = injector.getInstance(FloatTravelTimeModule.Registry.class);
        var times = injector.getInstance(Key.get(new TypeLiteral<Map<String, TravelTime>>() {}));
        var collector = registry.get("car");
        assertSame(collector.getLinkTravelTimes(), times.get("car"));
        assertSame(times.get("car"), times.get("truck"));
        assertSame(times.get("car"), times.get("car_passenger"));
        assertSame(bike, times.get("bike"));
        var trips = injector.getInstance(TripsHandler.class);
        assertSame(trips, injector.getInstance(TripsHandler.class));
        assertEquals(1, registry.size());
        // Exercise the real calibration provider as well as the standalone
        // handler test: retaining a filtered copy as its source would go stale.
        assertEquals(800, trips.routeTrips().values().stream().mapToDouble(s -> s.simulatedTime).sum(), 1e-7);
        network.getLinks().values().forEach(link -> link.setFreespeed(5));
        assertEquals(1600, trips.routeTrips().values().stream().mapToDouble(s -> s.simulatedTime).sum(), 1e-7);
        network.getLinks().values().forEach(link -> link.setFreespeed(10));
        for (var link : network.getLinks().values()) {
            collector.handleEvent(new LinkEnterEvent(100, Id.createVehicleId("sample"), link.getId()));
            collector.handleEvent(new LinkLeaveEvent(300, Id.createVehicleId("sample"), link.getId()));
        }
        collector.prepareForRouting();
        var before = collector.getStorageStatistics();
        var provider = injector.getProvider(TripRouter.class);
        try (var workers = Executors.newFixedThreadPool(2)) {
            // Subpopulation calibrators use a private TripRouter/PlanRouter per
            // worker. Exercise that exact provider chain for every road mode.
            var jobs = new ArrayList<java.util.concurrent.Callable<Void>>();
            for (int i = 0; i < 12; i++) {
                String mode = List.of("car", "truck", "car_passenger").get(i % 3);
                int personId = i;
                jobs.add(() -> {
                    var populationFactory = scenario.getPopulation().getFactory();
                    var person = populationFactory.createPerson(Id.createPersonId(personId));
                    VehicleUtils.insertVehicleIdsIntoAttributes(person,
                            Map.of(mode, Id.createVehicleId(personId)));
                    var plan = populationFactory.createPlan();
                    plan.setPerson(person);
                    var home = populationFactory.createActivityFromLinkId("home", Id.createLinkId("0_1"));
                    home.setEndTime(100);
                    plan.addActivity(home);
                    var leg = populationFactory.createLeg(mode);
                    leg.setTravelTime(0);
                    TripStructureUtils.setRoutingMode(leg, mode);
                    plan.addLeg(leg);
                    plan.addActivity(populationFactory.createActivityFromLinkId("work", Id.createLinkId("4_5")));
                    new PlanRouter(scenario.getActivityFacilities(), provider.get()).run(plan, true, Set.of(mode));
                    var routed = TripStructureUtils.getLegs(plan).stream()
                            .filter(l -> l.getRoute() instanceof NetworkRoute).findFirst().orElseThrow();
                    var route = (NetworkRoute) routed.getRoute();
                    assertEquals(List.of(Id.createLinkId("1_3"), Id.createLinkId("3_4")), route.getLinkIds());
                    assertTrue(routed.getTravelTime().seconds() >= 400);
                    return null;
                });
            }
            for (var result : workers.invokeAll(jobs)) result.get();
        }
        assertEquals("Routing must not allocate or modify shared travel-time rows", before, collector.getStorageStatistics());
        assertEquals(0, before.observationCountBytes());
        assertFalse(((MemoryEfficientSpeedyALTFactory) factory).getStatistics().isEmpty());
    }
}
