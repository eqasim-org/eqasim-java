package org.eqasim.scenario.cutter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.transit.DefaultStopSequenceCrossingPointFinder;
import org.eqasim.core.scenario.cutter.transit.TransitScheduleCutter;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import static org.junit.Assert.*;

public class TestTransitScheduleCutter {
    private final ScenarioExtent extent = new ScenarioExtent() {
        public boolean isInside(Coord coord) { return coord.getY() == 1; }
        public List<Coord> computeEuclideanIntersections(Coord from, Coord to) { return List.of(); }
        public Coord getInteriorPoint() { return new Coord(0, 1); }
    };

    private void check(boolean[] inside, int first, int last) {
        var schedule = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getTransitSchedule();
        var factory = schedule.getFactory();
        var stops = new ArrayList<TransitRouteStop>();
        for (int i = 0; i < inside.length; i++) {
            var stop = factory.createTransitStopFacility(Id.create("stop" + i,
                    org.matsim.pt.transitSchedule.api.TransitStopFacility.class),
                    new Coord(i, inside[i] ? 1 : 0), false);
            stop.setLinkId(Id.createLinkId(i));
            schedule.addStopFacility(stop);
            stops.add(factory.createTransitRouteStop(stop, i * 60.0, i * 60.0));
        }
        NetworkRoute networkRoute = (NetworkRoute) new LinkNetworkRouteFactory()
                .createRoute(Id.createLinkId(0), Id.createLinkId(inside.length - 1));
        networkRoute.setLinkIds(Id.createLinkId(0), IntStream.range(1, inside.length - 1)
                .mapToObj(Id::createLinkId).toList(), Id.createLinkId(inside.length - 1));
        var route = factory.createTransitRoute(Id.create("route",
                org.matsim.pt.transitSchedule.api.TransitRoute.class), networkRoute, stops, "pt");
        var departure = factory.createDeparture(Id.create("departure",
                org.matsim.pt.transitSchedule.api.Departure.class), 3600);
        departure.setVehicleId(Id.createVehicleId("bus"));
        route.addDeparture(departure);
        var line = factory.createTransitLine(Id.create("line",
                org.matsim.pt.transitSchedule.api.TransitLine.class));
        line.addRoute(route);
        schedule.addTransitLine(line);

        new TransitScheduleCutter(extent, new DefaultStopSequenceCrossingPointFinder(extent)).run(schedule);

        if (first < 0) {
            assertTrue(schedule.getTransitLines().isEmpty());
            assertTrue(schedule.getFacilities().isEmpty());
            return;
        }
        var reduced = schedule.getTransitLines().get(line.getId()).getRoutes().get(route.getId());
        assertEquals(stops.subList(first, last + 1), reduced.getStops());
        assertEquals(last - first + 1, schedule.getFacilities().size());
        assertEquals(Id.createLinkId(first), reduced.getRoute().getStartLinkId());
        assertEquals(Id.createLinkId(last), reduced.getRoute().getEndLinkId());
        assertEquals(IntStream.range(first + 1, last).mapToObj(Id::createLinkId).toList(),
                reduced.getRoute().getLinkIds());
        assertEquals(departure.getVehicleId(), reduced.getDepartures().get(departure.getId()).getVehicleId());
    }

    @Test public void retainsAdjacentOutsideStops() {
        check(new boolean[]{false, false, true, true, false, false}, 1, 4);
    }

    @Test public void retainsFullSequenceAcrossRepeatedCrossings() {
        check(new boolean[]{false, false, true, false, false, true, false, false}, 1, 6);
    }

    @Test public void retainsSingleInsideStopAndBothOutsideNeighbours() {
        check(new boolean[]{false, false, true, false, false}, 1, 3);
    }

    @Test public void handlesRoutesStartingOrEndingInside() {
        check(new boolean[]{true, true, false, false}, 0, 2);
        check(new boolean[]{false, false, true, true}, 1, 3);
    }

    @Test public void handlesEntirelyInsideAndOutsideRoutes() {
        check(new boolean[]{true, true, true}, 0, 2);
        check(new boolean[]{false, false, false}, -1, -1);
    }
}
