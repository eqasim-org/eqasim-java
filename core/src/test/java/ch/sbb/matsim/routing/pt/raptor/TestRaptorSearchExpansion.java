package ch.sbb.matsim.routing.pt.raptor;

import java.util.*;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.junit.Test;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import static org.junit.Assert.*;

public class TestRaptorSearchExpansion {
    @Test public void nearbyUnsuitableStopsNoLongerHideUsefulStations() { verify(false); }
    @Test public void rangeQueryAlsoExpands() { verify(true); }

    private void verify(boolean range) {
        var fixture = new Fixture(400, range);
        var result = fixture.route();
        assertTrue(hasTransit(result));
        assertEquals(List.of(100.0, 200.0, 400.0), fixture.radii);
        assertEquals(100, fixture.parameters.getSearchRadius(), 0);
        fixture.radii.clear();
        assertTrue(hasTransit(fixture.route())); // Shared parameters did not retain the expanded search.
        assertEquals(List.of(100.0, 200.0, 400.0), fixture.radii);
    }

    @Test public void successfulInitialQueryDoesNotExpand() {
        var fixture = new Fixture(80, false);
        assertTrue(hasTransit(fixture.route())); assertEquals(List.of(100.0), fixture.radii);
    }

    @Test public void failureStopsAfterFiveAttemptsBeforeWalkingFallback() {
        var fixture = new Fixture(4000, false);
        assertNull(fixture.route());
        assertEquals(List.of(100.0, 200.0, 400.0, 800.0, 1600.0), fixture.radii);
        var fallback = new SwissRailRaptorRoutingModule(fixture.router, fixture.scenario.getTransitSchedule(),
                fixture.scenario.getNetwork(), request -> List.of(PopulationUtilsHelper.walk()));
        fixture.radii.clear();
        var result = fallback.calcRoute(DefaultRoutingRequest.withoutAttributes(fixture.from, fixture.to, 0, null));
        assertEquals(1, result.size()); assertEquals("walk", ((Leg) result.getFirst()).getMode());
        assertEquals(5, fixture.radii.size());
    }

    @Test public void routeListApiExpandsToo() {
        var fixture = new Fixture(400, true);
        var routes = fixture.router.calcRoutes(fixture.from, fixture.to, 0, 0, 1000, null, new AttributesImpl());
        assertTrue(routes.stream().anyMatch(route -> route.parts.stream().anyMatch(part -> part.line != null)));
        assertEquals(3, fixture.radii.size());
    }

    @Test public void existingNearestStopExtensionIsIncludedInNextRadius() {
        var config = new SwissRailRaptorConfigGroup(); var parameters = new RaptorParameters(config);
        parameters.setSearchRadius(100); parameters.setExtensionRadius(50);
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        var stop = scenario.getTransitSchedule().getFactory().createTransitStopFacility(
                Id.create("distant", TransitStopFacility.class), new Coord(1000, 0), false);
        var facility = FacilitiesUtils.wrapLinkAndCoord(null, new Coord(0, 0));
        var radii = new ArrayList<Double>();
        RaptorSearchExpansion.search(facility, facility, parameters, (p, direction) -> {
            if (direction == RaptorStopFinder.Direction.ACCESS) radii.add(p.getSearchRadius());
            return List.of(new InitialStop(stop, 0, 0, 1000, "walk"));
        }, (a, b) -> false, Boolean::booleanValue);
        assertEquals(5, radii.size()); assertEquals(2000, radii.get(1), 0);
        assertEquals(100, parameters.getSearchRadius(), 0);
    }

    private static boolean hasTransit(List<? extends PlanElement> elements) {
        return elements != null && elements.stream().anyMatch(element -> element instanceof Leg leg
                && leg.getRoute() instanceof TransitPassengerRoute);
    }

    private static final class PopulationUtilsHelper {
        static Leg walk() { return org.matsim.core.population.PopulationUtils.createLeg("walk"); }
    }

    private static final class Fixture {
        final Scenario scenario;
        final SwissRailRaptor router;
        final RaptorParameters parameters;
        final Facility from, to;
        final List<Double> radii = new ArrayList<>();
        Fixture(double usefulDistance, boolean range) {
            var raptorConfig = new SwissRailRaptorConfigGroup();
            raptorConfig.setUseRangeQuery(range);
            var rangeSettings = new SwissRailRaptorConfigGroup.RangeQuerySettingsParameterSet();
            rangeSettings.setMaxEarlierDeparture(0); rangeSettings.setMaxLaterDeparture(1000);
            raptorConfig.addRangeQuerySettings(rangeSettings);
            raptorConfig.setIntermodalLegOnlyHandling(SwissRailRaptorConfigGroup.IntermodalLegOnlyHandling.forbid);
            var config = ConfigUtils.createConfig(raptorConfig);
            config.transitRouter().setSearchRadius(100); config.transitRouter().setExtensionRadius(100);
            config.transitRouter().setMaxBeelineWalkConnectionDistance(50);
            scenario = ScenarioUtils.createScenario(config);
            var schedule = scenario.getTransitSchedule(); var factory = schedule.getFactory();
            double[] xs = {20, -20, 9980, 10020, usefulDistance, 10000 - usefulDistance};
            var stops = new ArrayList<TransitStopFacility>();
            for (int i = 0; i < xs.length; i++) {
                var a = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId("a" + i), new Coord(xs[i], 0));
                var b = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId("b" + i), new Coord(xs[i] + 1, 0));
                var link = NetworkUtils.createAndAddLink(scenario.getNetwork(), Id.createLinkId(i), a, b, 1, 10, 1000, 1);
                var stop = factory.createTransitStopFacility(Id.create("s" + i, TransitStopFacility.class), a.getCoord(), false);
                stop.setLinkId(link.getId()); schedule.addStopFacility(stop); stops.add(stop);
            }
            for (int i = 0; i < 3; i++) {
                var a = stops.get(i * 2); var b = stops.get(i * 2 + 1);
                var route = factory.createTransitRoute(Id.create("r" + i, TransitRoute.class),
                        RouteUtils.createLinkNetworkRouteImpl(a.getLinkId(), List.of(), b.getLinkId()),
                        List.of(factory.createTransitRouteStop(a, 0, 0), factory.createTransitRouteStop(b, 100, 100)), "bus");
                route.addDeparture(factory.createDeparture(Id.create("d" + i, Departure.class), i == 2 ? 800 : 1));
                var line = factory.createTransitLine(Id.create("l" + i, TransitLine.class)); line.addRoute(route); schedule.addTransitLine(line);
            }
            parameters = RaptorUtils.createParameters(config);
            var data = SwissRailRaptorData.create(schedule, scenario.getTransitVehicles(), RaptorUtils.createStaticConfig(config), scenario.getNetwork(), null);
            var original = new DefaultRaptorStopFinder(new DefaultRaptorIntermodalAccessEgress(), null);
            RaptorStopFinder counting = (a, b, person, time, attributes, p, d, direction) -> {
                if (direction == RaptorStopFinder.Direction.ACCESS) radii.add(p.getSearchRadius());
                return original.findStops(a, b, person, time, attributes, p, d, direction);
            };
            router = new SwissRailRaptor.Builder(data, config).with((RaptorParametersForPerson) person -> parameters).with(counting).build();
            from = FacilitiesUtils.wrapLinkAndCoord(scenario.getNetwork().getLinks().get(Id.createLinkId(0)), new Coord(0, 0));
            to = FacilitiesUtils.wrapLinkAndCoord(scenario.getNetwork().getLinks().get(Id.createLinkId(2)), new Coord(10000, 0));
        }
        List<? extends PlanElement> route() {
            return router.calcRoute(DefaultRoutingRequest.withoutAttributes(from, to, 0, null));
        }
    }
}
