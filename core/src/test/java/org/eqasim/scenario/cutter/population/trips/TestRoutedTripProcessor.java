package org.eqasim.scenario.cutter.population.trips;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eqasim.core.components.transit.departure.DepartureFinder;
import org.eqasim.core.scenario.cutter.extent.LinkRegionClassifier;
import org.eqasim.core.scenario.cutter.extent.ShapeScenarioExtent;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.eqasim.core.scenario.cutter.outside.OutsideActivityAdapter;
import org.eqasim.core.scenario.cutter.population.DefaultMergeOutsideActivities;
import org.eqasim.core.scenario.cutter.population.PlanCutter;
import org.eqasim.core.scenario.cutter.population.PopulationCutterModule;
import org.eqasim.core.scenario.cutter.population.trips.RoutedTripProcessor;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.DefaultNetworkRouteCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.timing.LinkTimingRegistry;
import org.eqasim.core.scenario.cutter.transit.DefaultStopSequenceCrossingPointFinder;
import org.eqasim.core.scenario.cutter.transit.TransitScheduleCutter;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

public class TestRoutedTripProcessor {
    private final org.matsim.api.core.v01.Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
    private final ShapeScenarioExtent extent = new ShapeScenarioExtent(new GeometryFactory().createPolygon(new Coordinate[] {
            new Coordinate(0, -1000), new Coordinate(20000, -1000), new Coordinate(20000, 1000),
            new Coordinate(0, 1000), new Coordinate(0, -1000) }));

    private RoutedTripProcessor processor() {
        var network = scenario.getNetwork();
        LinkRegionClassifier.classify(network, extent);
        TravelTime tt = (l, t, p, v) -> l.getLength() / 10;
        var finder = new DefaultNetworkRouteCrossingPointFinder(network,
                Map.of("car", tt, "bike", tt, "car_passenger", tt), new LinkTimingRegistry());
        DepartureFinder departures = (route, access, egress, time) -> {
            var stop = route.getStops().stream().filter(s -> s.getStopFacility().getId().equals(access)).findFirst().orElseThrow();
            var departure = route.getDepartures().values().iterator().next();
            return new DepartureFinder.StopDeparture(departure, stop, 0);
        };
        return new RoutedTripProcessor(extent, network, finder, scenario.getTransitSchedule(), departures);
    }

    private void network(double... xs) {
        var n = scenario.getNetwork(); var f = n.getFactory();
        for (int i = 0; i < xs.length; i++) n.addNode(f.createNode(Id.createNodeId(i), new Coord(xs[i], 0)));
        for (int i = 0; i < xs.length - 1; i++) {
            var l = f.createLink(Id.createLinkId(i), n.getNodes().get(Id.createNodeId(i)), n.getNodes().get(Id.createNodeId(i + 1)));
            l.setLength(Math.abs(xs[i + 1] - xs[i])); l.setFreespeed(10);
            l.setAllowedModes(Set.of("car", "bike", "car_passenger")); n.addLink(l);
        }
    }

    private Activity activity(String type, double x, int link) {
        var a = PopulationUtils.createActivityFromCoordAndLinkId(type, new Coord(x, 0), Id.createLinkId(link));
        a.setEndTime(100); return a;
    }

    private Leg generic(String mode, String routingMode, double departure, double duration, double distance, int from, int to) {
        var l = PopulationUtils.createLeg(mode); l.setRoutingMode(routingMode);
        l.setDepartureTime(departure); l.setTravelTime(duration);
        var r = RouteUtils.createGenericRouteImpl(Id.createLinkId(from), Id.createLinkId(to));
        r.setDistance(distance); r.setTravelTime(duration); l.setRoute(r); return l;
    }

    private Leg networkLeg(String mode, int first, int last, double departure, double duration) {
        var l = generic(mode, mode, departure, duration, 0, first, last);
        var r = (NetworkRoute) new LinkNetworkRouteFactory().createRoute(Id.createLinkId(first), Id.createLinkId(last));
        var ids = new ArrayList<Id<org.matsim.api.core.v01.network.Link>>();
        for (int i = first + 1; i < last; i++) ids.add(Id.createLinkId(i));
        r.setLinkIds(Id.createLinkId(first), ids, Id.createLinkId(last));
        r.setVehicleId(Id.createVehicleId("original-vehicle")); r.setDistance(19800); r.setTravelTime(duration);
        l.setRoute(r); return l;
    }

    private List<PlanElement> cut(List<PlanElement> plan) {
        var processor = processor();
        var module = new PopulationCutterModule(extent, 1, 1, new LinkTimingRegistry());
        var dispatcher = module.provideModeAwareTripProcessor(scenario.getConfig().routing(), scenario.getConfig().transit(), extent, processor);
        var cutter = new PlanCutter(dispatcher, extent, new DefaultMergeOutsideActivities());
        return cutter.processPlan(Id.createPersonId("person"), plan);
    }

    @Test public void accessAndEgressCutsKeepMainLegForAllModes() {
        network(100, 200, 19800, 19900);
        for (String mode : List.of("car", "bike", "car_passenger", "pt")) {
            for (boolean accessOutside : List.of(false, true)) for (boolean egressOutside : List.of(false, true)) {
                var access = generic("walk", mode, 100, 200, 200, 0, 0);
                var egress = generic("walk", mode, 2300, 200, 200, 2, 2);
                Leg main = mode.equals("car") || mode.equals("bike") ? networkLeg(mode, 0, 2, 300, 2000)
                        : generic(mode, mode, 300, 2000, 19800, 0, 2);
                var source = List.<PlanElement>of(activity(accessOutside ? "outside" : "home", accessOutside ? -100 : 50, 0),
                        access, activity(mode + " interaction", 100, 0), main, activity(mode + " interaction", 19900, 2),
                        egress, activity(egressOutside ? "outside" : "work", egressOutside ? 20100 : 19950, 2));
                // The common processor handles any generic mode, including car passenger and PT fallbacks.
                var cutter = new PlanCutter(processor(), extent, new DefaultMergeOutsideActivities());
                var result = cutter.processPlan(Id.createPersonId("person"), source);
                assertEquals(7, result.size()); assertSame(main, result.get(3));
                assertSame(main.getRoute(), ((Leg) result.get(3)).getRoute());
                assertEquals(300, ((Leg) result.get(3)).getDepartureTime().seconds(), 0);
                assertEquals(accessOutside ? 0 : 50, ((Activity) result.getFirst()).getCoord().getX(), 1e-8);
                assertEquals(egressOutside ? 20000 : 19950, ((Activity) result.getLast()).getCoord().getX(), 1e-8);
                assertEquals(accessOutside ? 100 : 200, ((Leg) result.get(1)).getTravelTime().seconds(), 1e-8);
                assertEquals(egressOutside ? 100 : 200, ((Leg) result.get(5)).getTravelTime().seconds(), 1e-8);
                assertEquals(result.size(), cutter.processPlan(Id.createPersonId("person"), result).size());
            }
        }
    }

    @Test public void productionDispatchAndOutsideAdapterPreserveBoundaryAndRoutesAcrossSecondCut() {
        network(100, 200, 19800, 19900);
        var main = networkLeg("car", 0, 2, 300, 2000);
        var result = cut(List.of(activity("outside", -100, 0), generic("walk", "car", 100, 200, 200, 0, 0),
                activity("car interaction", 100, 0), main, activity("car interaction", 19900, 2),
                generic("walk", "car", 2300, 200, 200, 2, 2), activity("outside", 20100, 2)));
        var person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("person"));
        var plan = PopulationUtils.createPlan(person); plan.getPlanElements().addAll(result); person.addPlan(plan);
        scenario.getPopulation().addPerson(person);
        new OutsideActivityAdapter(new RoadNetwork(scenario.getNetwork(), Set.of("car")))
                .run(scenario.getPopulation(), scenario.getActivityFacilities());
        assertEquals(0, ((Activity) result.getFirst()).getCoord().getX(), 0);
        assertEquals(20000, ((Activity) result.getLast()).getCoord().getX(), 0);
        var second = cut(result);
        assertEquals(7, second.size()); assertSame(main, second.get(3));
        assertEquals(((Leg) result.get(1)).getTravelTime(), ((Leg) second.get(1)).getTravelTime());
        assertEquals(((Leg) result.get(5)).getTravelTime(), ((Leg) second.get(5)).getTravelTime());
        assertNotNull(((Leg) second.get(1)).getRoute().getStartLinkId());
    }

    @Test public void networkRouteIsTrimmedWithoutReroutingAndKeepsVehicle() {
        network(-200, -100, 100, 1000, 20100, 20200);
        var source = networkLeg("car", 0, 4, 100, 2240);
        var result = cut(List.of(activity("outside", -150, 0), source, activity("outside", 20150, 4)));
        assertEquals(3, result.size()); var retained = (Leg) result.get(1);
        var route = (NetworkRoute) retained.getRoute();
        assertEquals(Id.createLinkId(1), route.getStartLinkId());
        assertEquals(List.of(Id.createLinkId(2)), route.getLinkIds());
        assertEquals(Id.createLinkId(3), route.getEndLinkId());
        assertEquals(Id.createVehicleId("original-vehicle"), route.getVehicleId());
        assertEquals(Id.createLinkId(0), source.getRoute().getStartLinkId());
        var second = cut(result);
        assertSame(retained, second.get(1));
        assertEquals(retained.getDepartureTime(), ((Leg) second.get(1)).getDepartureTime());
    }

    @Test public void transitStopsAndDepartureSurvivePopulationAndScheduleCut() {
        network(-200, -100, 100, 1000, 20100, 20200);
        var schedule = scenario.getTransitSchedule(); var f = schedule.getFactory();
        var stops = new ArrayList<TransitRouteStop>();
        double[] xs = {-150, -50, 550, 10550, 20150};
        for (int i = 0; i < xs.length; i++) {
            var stop = f.createTransitStopFacility(Id.create("s" + i, org.matsim.pt.transitSchedule.api.TransitStopFacility.class), new Coord(xs[i], 0), false);
            stop.setLinkId(Id.createLinkId(i)); schedule.addStopFacility(stop);
            stops.add(f.createTransitRouteStop(stop, i * 100.0, i * 100.0 + 10));
        }
        var service = f.createTransitRoute(Id.create("service", org.matsim.pt.transitSchedule.api.TransitRoute.class),
                (NetworkRoute) networkLeg("car", 0, 4, 0, 400).getRoute(), stops, "bus");
        var departure = f.createDeparture(Id.create("departure", org.matsim.pt.transitSchedule.api.Departure.class), 1000);
        service.addDeparture(departure);
        var line = f.createTransitLine(Id.create("line", org.matsim.pt.transitSchedule.api.TransitLine.class)); line.addRoute(service); schedule.addTransitLine(line);
        var passenger = new DefaultTransitPassengerRoute(stops.getFirst().getStopFacility(), line, service, stops.getLast().getStopFacility());
        passenger.setBoardingTime(1010); passenger.setDistance(20000); passenger.setTravelTime(500);
        var pt = generic("pt", "pt", 1000, 500, 20000, 0, 4); pt.setRoute(passenger);
        var cutter = new PlanCutter(processor(), extent, new DefaultMergeOutsideActivities());
        var result = cutter.processPlan(Id.createPersonId("person"), List.of(activity("outside", -150, 0), pt, activity("outside", 20150, 4)));
        assertEquals(7, result.size());
        var retained = (TransitPassengerRoute) ((Leg) result.get(3)).getRoute();
        assertEquals(stops.get(1).getStopFacility().getId(), retained.getAccessStopId());
        assertEquals(stops.get(4).getStopFacility().getId(), retained.getEgressStopId());
        assertEquals(1110, retained.getBoardingTime().seconds(), 0);
        new TransitScheduleCutter(extent, new DefaultStopSequenceCrossingPointFinder(extent)).run(schedule);
        var cutService = schedule.getTransitLines().get(retained.getLineId()).getRoutes().get(retained.getRouteId());
        assertTrue(cutService.getStops().stream().anyMatch(s -> s.getStopFacility().getId().equals(retained.getAccessStopId())));
        assertEquals(1110, cutService.getDepartures().get(departure.getId()).getDepartureTime()
                + cutService.getStops().getFirst().getDepartureOffset().seconds(), 0);

        // A real scheduled passenger route must remain intact when only its access/egress walks cross.
        var onboardRoute = new DefaultTransitPassengerRoute(stops.get(2).getStopFacility(), line, service,
                stops.get(3).getStopFacility());
        onboardRoute.setBoardingTime(1210); onboardRoute.setDistance(10000); onboardRoute.setTravelTime(100);
        var onboard = generic("pt", "pt", 1200, 100, 10000, 2, 3); onboard.setRoute(onboardRoute);
        var access = generic("walk", "pt", 1000, 200, 650, 0, 2);
        var egress = generic("walk", "pt", 1300, 100, 9550, 3, 4);
        var accessCut = new PlanCutter(processor(), extent, new DefaultMergeOutsideActivities())
                .processPlan(Id.createPersonId("person"), List.of(activity("outside", -100, 0), access,
                        activity("pt interaction", 550, 2), onboard, activity("pt interaction", 10550, 3),
                        egress, activity("outside", 20100, 4)));
        assertEquals(7, accessCut.size()); assertSame(onboard, accessCut.get(3));
        assertSame(onboardRoute, ((Leg) accessCut.get(3)).getRoute());
        assertEquals(1210, onboardRoute.getBoardingTime().seconds(), 0);
        assertEquals(1200, ((Leg) accessCut.get(1)).getDepartureTime().seconds()
                + ((Leg) accessCut.get(1)).getTravelTime().seconds(), 1e-8);
        assertEquals(0, ((Activity) accessCut.getFirst()).getCoord().getX(), 1e-8);
        assertEquals(20000, ((Activity) accessCut.getLast()).getCoord().getX(), 1e-8);
    }

    @Test public void genuineWalkOnlyRetainedPartDoesNotBecomeACarTrip() {
        network(-300, -200, -100, 100);
        var result = new PlanCutter(processor(), extent, new DefaultMergeOutsideActivities())
                .processPlan(Id.createPersonId("person"), List.of(activity("outside", -250, 0),
                        networkLeg("car", 0, 1, 100, 200), activity("car interaction", -100, 1),
                        generic("walk", "car", 300, 200, 200, 1, 2), activity("work", 100, 2)));
        assertEquals(3, result.size());
        assertEquals("walk", ((Leg) result.get(1)).getMode());
        assertEquals("walk", ((Leg) result.get(1)).getRoutingMode());
        assertEquals(100, ((Leg) result.get(1)).getRoute().getDistance(), 1e-8);
    }
}
