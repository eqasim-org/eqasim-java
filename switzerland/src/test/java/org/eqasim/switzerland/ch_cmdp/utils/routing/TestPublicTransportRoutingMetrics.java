package org.eqasim.switzerland.ch_cmdp.utils.routing;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class TestPublicTransportRoutingMetrics {
    @Test
    public void separatesWalkingWaitingAndAllTransitModesAcrossStageActivities() {
        var schedule = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getTransitSchedule();
        var factory = schedule.getFactory();
        var line = factory.createTransitLine(Id.create("line", TransitLine.class));
        schedule.addTransitLine(line);
        var population = PopulationUtils.getFactory();
        List<PlanElement> elements = new ArrayList<>();
        elements.add(population.createActivityFromCoord("pt interaction", new Coord(0, 0)));
        elements.add(walk(120, 100));
        var modes = List.of("rail", "subway", "bus", "tram", "ferry");
        for (int i = 0; i < modes.size(); i++) {
            elements.add(population.createActivityFromCoord("pt interaction", new Coord(0, 0)));
            var service = factory.createTransitRoute(Id.create(modes.get(i), TransitRoute.class), null, List.of(), modes.get(i));
            line.addRoute(service);
            var route = new DefaultTransitPassengerRoute(Id.createLinkId("from"), Id.createLinkId("to"),
                    Id.create("a", TransitStopFacility.class), Id.create("b", TransitStopFacility.class),
                    line.getId(), service.getId());
            double wait = i == 0 ? 30 : 10;
            route.setBoardingTime(100 + wait);
            route.setTravelTime(wait + (i + 1) * 60);
            route.setDistance(1000);
            Leg leg = population.createLeg("pt");
            leg.setDepartureTime(100); leg.setTravelTime(route.getTravelTime().seconds()); leg.setRoute(route);
            elements.add(leg);
            elements.add(population.createActivityFromCoord("pt interaction", new Coord(0, 0)));
            if (i < modes.size() - 1) elements.add(walk(15, 10));
        }
        elements.add(walk(180, 200));
        elements.add(population.createActivityFromCoord("pt interaction", new Coord(0, 0)));
        var result = new MultiModeRouter.RoutedTrip();
        result.populatePtMetrics(elements, schedule);
        assertEquals(2, result.accessTravelTime_min, 0);
        assertEquals(3, result.egressTravelTime_min, 0);
        assertEquals(1, result.transferTravelTime_min, 0);
        assertEquals(6, result.totalWalkTravelTime_min, 0);
        assertEquals(0.34, result.totalWalkDistance_km, 1e-12);
        assertEquals(100, result.accessDistance, 0);
        assertEquals(200, result.egressDistance, 0);
        assertEquals(0.5, result.initialWaitingTime_min, 0);
        assertEquals(40.0 / 60, result.transferWaitingTime_min, 1e-12);
        assertEquals(Integer.valueOf(4), result.numberOfTransfers);
        assertEquals(Integer.valueOf(0), result.isOnlyWalk);
        assertEquals(1, result.inVehicleTimeRail_min, 0);
        assertEquals(2, result.inVehicleTimeSubway_min, 0);
        assertEquals(3, result.inVehicleTimeBus_min, 0);
        assertEquals(4, result.inVehicleTimeTram_min, 0);
        assertEquals(5, result.inVehicleTimeOther_min, 0);
        assertEquals(15, result.inVehicleTimeTotal_min, 0);
        assertEquals(5, result.inVehicleDistanceTotal_km, 0);
        for (Double distance : List.of(result.inVehicleDistanceRail_km, result.inVehicleDistanceSubway_km,
                result.inVehicleDistanceBus_km, result.inVehicleDistanceTram_km, result.inVehicleDistanceOther_km))
            assertEquals(1, distance, 0);
    }

    private Leg walk(double seconds, double meters) {
        var factory = PopulationUtils.getFactory();
        Leg leg = factory.createLeg("transit_walk");
        Route route = factory.getRouteFactories().createRoute(Route.class, null, null);
        route.setDistance(meters); route.setTravelTime(seconds);
        leg.setRoute(route); leg.setTravelTime(seconds);
        return leg;
    }
}
