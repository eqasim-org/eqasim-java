package org.eqasim.switzerland.ch_cmdp.trucks;

import static org.junit.Assert.*;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eqasim.core.components.travel_time.FloatTravelTimeModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.eqasim.core.components.travel_disutility.EqasimTravelDisutilityFactory;
import org.eqasim.switzerland.ch_cmdp.tolls.*;

public class TestTruckRouting {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private static Link link() {
        var network = NetworkUtils.createNetwork();
        var a = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"), new Coord(0, 0));
        var b = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"), new Coord(1000, 0));
        return NetworkUtils.createAndAddLink(network, Id.createLinkId("ab"), a, b, 1000, 10, 1000, 1);
    }

    @Test public void roadClassesUseFreeflowTimeRatherThanCongestedTime() {
        var link = link();
        link.getAttributes().putAttribute("toll", 7.0);
        var costs = new TruckTravelDisutilityFactory(0, 0, 4711, new Tolls(), new MarginalCostOfTolls(0, 3600, 4711), new borderCrossingLinks(List.of(link)))
                .createTravelDisutility((l, t, p, v) -> 300);
        for (String type : List.of("motorway", "motorway_link", "trunk", "trunk_link", "primary", "primary_link",
                "secondary", "secondary_link", "tertiary", "tertiary_link", "residential", "unclassified", "unknown")) {
            link.getAttributes().putAttribute("osm:way:highway", type);
            double penalty = type.startsWith("secondary") ? 10 : type.startsWith("tertiary") ? 15
                    : type.startsWith("motorway") || type.startsWith("trunk") || type.startsWith("primary") ? 0 : 20;
            assertEquals(type, 300 + penalty, costs.getLinkTravelDisutility(link, 0, null, null), 1e-12);
            assertEquals(type, 100 + penalty, costs.getLinkMinimumTravelDisutility(link), 1e-12);
        }
        link.getAttributes().removeAttribute("osm:way:highway");
        assertEquals(320, costs.getLinkTravelDisutility(link, 0, null, null), 1e-12);
        link.setFreespeed(20);
        assertEquals(310, costs.getLinkTravelDisutility(link, 0, null, null), 1e-12);
    }

    @Test public void truckTimesDelegateToLiveCarInstance() {
        double[] current = {42};
        var person = org.matsim.core.population.PopulationUtils.getFactory().createPerson(Id.createPersonId("truck"));
        var vehicle = org.matsim.vehicles.VehicleUtils.createVehicle(Id.createVehicleId("truck"), org.matsim.vehicles.VehicleUtils.createDefaultVehicleType());
        var link = link();
        var truck = new TruckTravelTime((l, t, p, v) -> {
            assertSame(link, l); assertSame(person, p); assertSame(vehicle, v); assertEquals(900, t, 0);
            return current[0];
        });
        assertEquals(42, truck.getLinkTravelTime(link, 900, person, vehicle), 0);
        current[0] = 75;
        assertEquals(75, truck.getLinkTravelTime(link, 900, person, vehicle), 0);
    }

    @Test public void independentFormulaIncludesDistanceAndReproducibleNoiseWithValidLowerBound() {
        var link = link();
        var person = org.matsim.core.population.PopulationUtils.getFactory().createPerson(Id.createPersonId("driver"));
        TravelTime time = (l, t, p, v) -> 300;
        var costs = new TruckTravelDisutilityFactory(2, .3, 4711, new Tolls(), new MarginalCostOfTolls(0, 3600, 4711), new borderCrossingLinks(List.of(link)))
                .createTravelDisutility(time);
        double multiplier = Math.max(1 + .3 * org.eqasim.core.tools.random.Normal.get("ab", "driver", 4711), .001);
        double expected = 320 * multiplier + 20;
        assertEquals(expected, costs.getLinkTravelDisutility(link, 900, person, null), 0);
        assertEquals(expected, costs.getLinkTravelDisutility(link, 900, person, null), 0);
        assertEquals(20.12, costs.getLinkMinimumTravelDisutility(link), 1e-12);
        assertTrue(costs.getLinkMinimumTravelDisutility(link) <= expected);
    }

    @Test public void installedModuleSharesCarCollectorEvenWithSeparateModesAndPreservesCarCosts() throws Exception {
        var settings = new org.eqasim.core.components.config.EqasimConfigGroup();
        settings.setRoutingDistanceUtility(2);
        settings.setSigmaRoutingRandomness(0);
        var config = ConfigUtils.createConfig(settings);
        config.controller().setOutputDirectory(temporary.newFolder().toString());
        config.routing().setNetworkModes(List.of("car", "truck"));
        config.travelTimeCalculator().setSeparateModes(true);
        config.travelTimeCalculator().setAnalyzedModes(Set.of("car", "truck"));
        var scenario = ScenarioUtils.createScenario(config);
        var link = link();
        link.setAllowedModes(Set.of("car", "truck"));
        link.getAttributes().putAttribute("osm:way:highway", "secondary");
        scenario.getNetwork().addNode(link.getFromNode()); scenario.getNetwork().addNode(link.getToNode());
        scenario.getNetwork().addLink(link);
        var controller = new Controler(scenario);
        controller.addOverridingModule(new FloatTravelTimeModule());
        var penaltyCalls = new java.util.concurrent.atomic.AtomicInteger();
        var baseFactory = new EqasimTravelDisutilityFactory((l, p, t, base) -> { penaltyCalls.incrementAndGet(); return 50; }, 2);
        var carFactory = new EqasimTollsTravelDisutilityFactory(baseFactory, new Tolls(), new MarginalCostOfTolls(0, 3600, 4711), new borderCrossingLinks(scenario.getNetwork()));
        link.getAttributes().putAttribute("toll", 7.0);
        var vehicle = org.matsim.vehicles.VehicleUtils.createVehicle(Id.createVehicleId("truck"), org.matsim.vehicles.VehicleUtils.createDefaultVehicleType());
        controller.addOverridingModule(new AbstractModule() {
            @Override public void install() {
                addTravelDisutilityFactoryBinding("car").toInstance(carFactory);
                bind(Tolls.class).toInstance(new Tolls());
                bind(MarginalCostOfTolls.class).toInstance(new MarginalCostOfTolls(0, 3600, 4711));
            }
        });
        controller.addOverridingModule(new TruckRoutingModule());
        var injector = controller.getInjector();
        var times = injector.getInstance(Key.get(new TypeLiteral<Map<String, TravelTime>>() {}));
        var costs = injector.getInstance(Key.get(new TypeLiteral<Map<String, TravelDisutilityFactory>>() {}));
        assertEquals(times.get("car").getLinkTravelTime(link, 0, null, null), times.get("truck").getLinkTravelTime(link, 0, null, null), 0);
        assertEquals(1, injector.getInstance(FloatTravelTimeModule.Registry.class).size());
        double travelTime = times.get("car").getLinkTravelTime(link, 0, null, vehicle);
        // Truck distance=20, road-class surcharge=10, tolls disabled. Car distance=200, RoutingPenalty=50, toll=7.
        assertEquals(travelTime + 30, costs.get("truck").createTravelDisutility(times.get("truck")).getLinkTravelDisutility(link, 0, null, vehicle), 1e-12);
        assertEquals(0, penaltyCalls.get());
        assertEquals(travelTime + 257, costs.get("car").createTravelDisutility(times.get("car")).getLinkTravelDisutility(link, 0, null, vehicle), 1e-12);
        assertEquals(1, penaltyCalls.get());
    }

    @Test public void calibrationPlanRouterUsesDifferentCarAndTruckCosts() throws Exception {
        var settings = new org.eqasim.core.components.config.EqasimConfigGroup();
        settings.setRoutingDistanceUtility(0); settings.setSigmaRoutingRandomness(0);
        var config = ConfigUtils.createConfig(settings);
        config.controller().setOutputDirectory(temporary.newFolder().toString());
        config.routing().setNetworkModes(List.of("car", "truck"));
        config.routing().setAccessEgressType(org.matsim.core.config.groups.RoutingConfigGroup.AccessEgressType.none);
        config.controller().setRoutingAlgorithmType(org.matsim.core.config.groups.ControllerConfigGroup.RoutingAlgorithmType.SpeedyALT);
        var scenario = ScenarioUtils.createScenario(config); var network = scenario.getNetwork();
        for (int i = 0; i < 4; i++) NetworkUtils.createAndAddNode(network, Id.createNodeId(i), new Coord(i*1000, 0));
        String[] ids = {"start", "primary", "secondary", "end", "return"};
        int[] from = {0, 1, 1, 2, 3}, to = {1, 2, 2, 3, 0};
        double[] lengths = {100, 1000, 1010, 100, 1200};
        for (int i = 0; i < ids.length; i++) {
            var link = NetworkUtils.createAndAddLink(network, Id.createLinkId(ids[i]), network.getNodes().get(Id.createNodeId(from[i])),
                    network.getNodes().get(Id.createNodeId(to[i])), lengths[i], 10, 1000, 1);
            link.setAllowedModes(Set.of("car", "truck"));
            link.getAttributes().putAttribute("osm:way:highway", ids[i].equals("secondary") ? "secondary" : "primary");
        }
        var controller = new Controler(scenario);
        controller.addOverridingModule(new FloatTravelTimeModule());
        controller.addOverridingModule(new org.eqasim.core.components.routing.speedy.MemoryEfficientRoutingModule());
        controller.addOverridingModule(new AbstractModule() {
            @Override public void install() {
                bind(EqasimTravelDisutilityFactory.class).toInstance(new EqasimTravelDisutilityFactory(
                        (link, person, time, cost) -> link.getId().toString().equals("primary") ? 50 : 0, 0));
            }
        });
        controller.addOverridingModule(new TollsModule()); controller.addOverridingModule(new TruckRoutingModule());
        var provider = controller.getInjector().getProvider(org.matsim.core.router.TripRouter.class);
        // This is exactly the provider/PlanRouter chain used by demand-calibration workers.
        for (String mode : List.of("car", "truck")) {
            var factory = scenario.getPopulation().getFactory(); var person = factory.createPerson(Id.createPersonId(mode));
            var type = scenario.getVehicles().getFactory().createVehicleType(Id.createVehicleTypeId(mode));
            scenario.getVehicles().addVehicleType(type);
            var vehicle = scenario.getVehicles().getFactory().createVehicle(Id.createVehicleId(mode), type);
            scenario.getVehicles().addVehicle(vehicle);
            org.matsim.vehicles.VehicleUtils.insertVehicleIdsIntoAttributes(person, Map.of(mode, vehicle.getId()));
            var plan = factory.createPlan(); plan.setPerson(person);
            var origin = factory.createActivityFromLinkId("home", Id.createLinkId("start")); origin.setEndTime(100);
            plan.addActivity(origin); plan.addLeg(factory.createLeg(mode));
            plan.addActivity(factory.createActivityFromLinkId("work", Id.createLinkId("end")));
            new org.eqasim.core.scenario.routing.PlanRouter(scenario.getActivityFacilities(), provider.get()).run(plan, true, Set.of(mode));
            var leg = org.matsim.core.router.TripStructureUtils.getTrips(plan).getFirst().getLegsOnly().getFirst();
            assertEquals(mode, leg.getMode());
            var route = (org.matsim.core.population.routes.NetworkRoute) leg.getRoute();
            assertEquals(List.of(Id.createLinkId(mode.equals("car") ? "secondary" : "primary")), route.getLinkIds());
        }
    }
}
