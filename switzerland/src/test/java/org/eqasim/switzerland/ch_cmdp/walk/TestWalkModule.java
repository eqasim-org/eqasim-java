package org.eqasim.switzerland.ch_cmdp.walk;

import static org.junit.Assert.*;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.*;
import java.util.*;
import org.junit.Test;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.pt.transitSchedule.api.*;

public class TestWalkModule {
    @Test public void defaultPreservesTeleportationAndCustomReceivesDirectedGeometry() {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        var network = scenario.getNetwork();
        var a = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"), new Coord(0, 0, 400));
        var b = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"), new Coord(100, 0, 410));
        var link = NetworkUtils.createAndAddLink(network, Id.createLinkId("ab"), a, b, 100, 10, 1000, 1);
        var params = scenario.getConfig().routing().getOrCreateModeRoutingParams("walk");
        params.setBeelineDistanceFactor(1.3); params.setTeleportedModeSpeed(1.3);
        var request = DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLinkAndCoord(link, new Coord(1, 0)),
                FacilitiesUtils.wrapLinkAndCoord(link, new Coord(99, 0)), 123, null);
        var baseline = DefaultRoutingModules.createTeleportationRouter("walk", scenario, params);
        var defaultRouter = new WalkRoutingModule(baseline, new WalkTravelTimeCalculator(scenario, new TeleportationWalkTravelTime()));
        var original = (Leg) baseline.calcRoute(request).getFirst();
        var unchanged = (Leg) defaultRouter.calcRoute(request).getFirst();
        assertEquals(original.getTravelTime().seconds(), unchanged.getTravelTime().seconds(), 0);
        assertEquals(original.getRoute().getDistance(), unchanged.getRoute().getDistance(), 0);
        var calculator = new WalkTravelTimeCalculator(scenario, context -> {
            assertSame(a, context.originNode()); assertSame(b, context.destinationNode());
            assertTrue(context.hasElevation()); assertEquals(10, context.nodesElevationChange(), 0);
            assertEquals(100, context.nodesDistance(), 0);
            assertEquals(98, context.euclideanDistance(), 0);
            return context.euclideanDistance() * 2;
        });
        var result = (Leg) new WalkRoutingModule(baseline, calculator).calcRoute(request).getFirst();
        assertEquals(196, result.getTravelTime().seconds(), 0);
        assertEquals(196, result.getRoute().getTravelTime().seconds(), 0);
        assertEquals(original.getRoute().getDistance(), result.getRoute().getDistance(), 0);
        var slope = new WalkTravelTimeCalculator(scenario, c -> 100 + c.nodesElevationChange()/c.nodesDistance());
        assertEquals(99.9, slope.calculate(b.getCoord(), a.getCoord(), null, 0, 0), 1e-12);
    }

    @Test public void handlesMissingElevation() {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        var calculator = new WalkTravelTimeCalculator(scenario, c -> {
            assertNull(c.originNode()); assertFalse(c.hasElevation()); assertEquals(0, c.nodesElevationChange(), 0);
            return c.defaultTravelTime();
        });
        assertEquals(12, calculator.calculate(new Coord(0, 0), new Coord(1, 0), null, 0, 12), 0);
        NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId(0), new Coord(0, 0));
        var sameNode = new WalkTravelTimeCalculator(scenario, c -> {
            assertSame(c.originNode(), c.destinationNode()); assertFalse(c.hasElevation());
            assertEquals(0, c.nodesElevationChange(), 0); return 1;
        });
        assertEquals(1, sameNode.calculate(new Coord(0, 0), new Coord(1, 0), null, 0, 0), 0);
    }

    @Test public void ptUsesDefaultStopFinderAndKeepsAllInternalWalkingTimes() throws Exception {
        var config = ConfigUtils.createConfig(new SwissRailRaptorConfigGroup());
        config.transit().setUseTransit(true);
        config.routing().getOrCreateModeRoutingParams("walk").setTeleportedModeSpeed(1.0);
        config.routing().getOrCreateModeRoutingParams("walk").setBeelineDistanceFactor(1.0);
        config.transitRouter().setSearchRadius(150); config.transitRouter().setExtensionRadius(0);
        config.transitRouter().setMaxBeelineWalkConnectionDistance(300);
        var scenario = ScenarioUtils.createScenario(config);
        var schedule = scenario.getTransitSchedule(); var factory = schedule.getFactory();
        double[] xs = {0, 10000, 10200, 20000};
        var stops = new ArrayList<TransitStopFacility>();
        for (int i = 0; i < xs.length; i++) {
            var a = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId("a"+i), new Coord(xs[i], 0));
            var b = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId("b"+i), new Coord(xs[i]+1, 0));
            var link = NetworkUtils.createAndAddLink(scenario.getNetwork(), Id.createLinkId(i), a, b, 1, 10, 1000, 1);
            var stop = factory.createTransitStopFacility(Id.create("s"+i, TransitStopFacility.class), a.getCoord(), false);
            stop.setLinkId(link.getId()); schedule.addStopFacility(stop); stops.add(stop);
        }
        for (int i = 0; i < 2; i++) {
            var a = stops.get(i*2); var b = stops.get(i*2+1);
            var route = factory.createTransitRoute(Id.create("r"+i, TransitRoute.class),
                    RouteUtils.createLinkNetworkRouteImpl(a.getLinkId(), List.of(), b.getLinkId()),
                    List.of(factory.createTransitRouteStop(a, 0, 0), factory.createTransitRouteStop(b, 600, 600)), "bus");
            route.addDeparture(factory.createDeparture(Id.create("d"+i, Departure.class), 1000+i*1000));
            var line = factory.createTransitLine(Id.create("line"+i, TransitLine.class));
            line.addRoute(route); schedule.addTransitLine(line);
        }
        var data = SwissRailRaptorData.create(schedule, scenario.getTransitVehicles(), RaptorUtils.createStaticConfig(config), scenario.getNetwork(), null);
        var delegate = new DefaultRaptorStopFinder(new DefaultRaptorIntermodalAccessEgress(), null);
        var request = DefaultRoutingRequest.withoutAttributes(
                FacilitiesUtils.wrapLinkAndCoord(scenario.getNetwork().getLinks().get(Id.createLinkId(0)), new Coord(-100, 0)),
                FacilitiesUtils.wrapLinkAndCoord(scenario.getNetwork().getLinks().get(Id.createLinkId(3)), new Coord(20100, 0)), 0, null);
        var normal = new SwissRailRaptor.Builder(data, config).with(delegate).build().calcRoute(request);
        config.controller().setOutputDirectory(java.nio.file.Files.createTempDirectory("unchanged-pt-walking").toString());
        var controller = new org.matsim.core.controler.Controler(scenario);
        controller.addOverridingModule(new SwissRailRaptorModule());
        controller.addOverridingModule(new WalkModule(ForbiddenPtWalkTime.class));
        var injector = controller.getInjector();
        assertTrue(injector.getInstance(RaptorStopFinder.class) instanceof DefaultRaptorStopFinder);
        var changed = injector.getInstance(org.matsim.pt.router.TransitRouter.class).calcRoute(request);
        assertNotNull(normal); assertNotNull(changed);
        var before = normal.stream().filter(Leg.class::isInstance).map(Leg.class::cast).toList();
        var after = changed.stream().filter(Leg.class::isInstance).map(Leg.class::cast).toList();
        assertEquals(5, before.size()); assertEquals(5, after.size());
        assertTrue(before.getFirst().getTravelTime().seconds() > 0);
        for (int i = 0; i < before.size(); i++) {
            assertEquals(before.get(i).getTravelTime().seconds(), after.get(i).getTravelTime().seconds(), 0);
            assertEquals(before.get(i).getRoute().getDistance(), after.get(i).getRoute().getDistance(), 0);
        }
    }

    public static final class ForbiddenPtWalkTime implements WalkTravelTime {
        @Override public double calculate(WalkTravelTimeContext context) {
            throw new AssertionError("PT must not call the custom walking model");
        }
    }

    public static final class DoubleWalkingTime implements WalkTravelTime {
        @Override public double calculate(WalkTravelTimeContext c) { return c.defaultTravelTime()*2; }
    }

    @Test public void installedModuleRoutesCarAccessAndEgressThroughExpression() throws Exception {
        var config = ConfigUtils.createConfig();
        config.routing().getOrCreateModeRoutingParams("walk").setTeleportedModeSpeed(1.0);
        config.routing().getOrCreateModeRoutingParams("walk").setBeelineDistanceFactor(1.0);
        config.routing().setAccessEgressType(org.matsim.core.config.groups.RoutingConfigGroup.AccessEgressType.walkConstantTimeToLink);
        config.controller().setOutputDirectory(java.nio.file.Files.createTempDirectory("walk-module-test").toString());
        var scenario = ScenarioUtils.createScenario(config);
        var a = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId(0), new Coord(0, 0));
        var b = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId(1), new Coord(1000, 0));
        var link = NetworkUtils.createAndAddLink(scenario.getNetwork(), Id.createLinkId(0), a, b, 1000, 10, 1000, 1);
        link.setAllowedModes(Set.of("car"));
        NetworkUtils.createAndAddLink(scenario.getNetwork(), Id.createLinkId("back"), b, a, 1000, 10, 1000, 1).setAllowedModes(Set.of("car"));
        var controller = new org.matsim.core.controler.Controler(scenario);
        controller.addOverridingModule(new WalkModule(DoubleWalkingTime.class));
        var injector = controller.getInjector();
        assertSame(injector.getInstance(WalkTravelTime.class), injector.getInstance(WalkTravelTime.class));
        var facilities = FacilitiesUtils.createActivityFacilities().getFactory();
        var from = facilities.createActivityFacility(Id.create("from", org.matsim.facilities.ActivityFacility.class), new Coord(10, 100), link.getId());
        var to = facilities.createActivityFacility(Id.create("to", org.matsim.facilities.ActivityFacility.class), new Coord(900, 100), link.getId());
        var person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("walker"));
        var type = scenario.getVehicles().getFactory().createVehicleType(Id.create("car", org.matsim.vehicles.VehicleType.class));
        scenario.getVehicles().addVehicleType(type);
        var vehicle = scenario.getVehicles().getFactory().createVehicle(Id.createVehicleId("walker:car"), type);
        scenario.getVehicles().addVehicle(vehicle);
        org.matsim.vehicles.VehicleUtils.insertVehicleIdsIntoAttributes(person, Map.of("car", vehicle.getId()));
        var result = injector.getInstance(TripRouter.class).calcRoute("car", from, to, 0, person,
                person.getAttributes());
        var walks = result.stream().filter(Leg.class::isInstance).map(Leg.class::cast)
                .filter(l -> "walk".equals(l.getMode())).toList();
        assertEquals(2, walks.size());
        for (var walk : walks) assertEquals(200, walk.getTravelTime().seconds(), 0);
    }
}
