package org.eqasim.switzerland.ch_cmdp.utils.pricing;

import static org.junit.Assert.*;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.OccupancyData;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.*;

public class TestRunComputeTransitPricesRouting {
    @Test
    public void pricingRouterSupportsBusToTramTransferWithModeSpecificCosts() {
        routeWithTransfer(true);
    }

    @Test
    public void pricingRouterStillSupportsDefaultTransferCosts() {
        routeWithTransfer(false);
    }

    private void routeWithTransfer(boolean modeSpecific) {
        var raptorConfig = new SwissRailRaptorConfigGroup();
        raptorConfig.setIntermodalLegOnlyHandling(SwissRailRaptorConfigGroup.IntermodalLegOnlyHandling.forbid);
        if (modeSpecific) {
            raptorConfig.addModeToModeTransferPenalty(
                    new SwissRailRaptorConfigGroup.ModeToModeTransferPenalty("bus", "tram", 0.2));
        }
        var config = ConfigUtils.createConfig(raptorConfig);
        config.transit().setUseTransit(true);
        config.transitRouter().setSearchRadius(100);
        config.transitRouter().setExtensionRadius(100);
        var scenario = ScenarioUtils.createScenario(config);
        var network = scenario.getNetwork();
        for (int i = 0; i < 4; i++) {
            NetworkUtils.createAndAddNode(network, Id.createNodeId(i), new Coord(i * 1000, 0));
        }
        var schedule = scenario.getTransitSchedule();
        var factory = schedule.getFactory();
        var stops = new ArrayList<TransitStopFacility>();
        for (int i = 0; i < 3; i++) {
            var link = NetworkUtils.createAndAddLink(network, Id.createLinkId(i),
                    network.getNodes().get(Id.createNodeId(i)), network.getNodes().get(Id.createNodeId(i + 1)),
                    1000, 10, 1000, 1);
            link.setAllowedModes(Set.of("car", "bus", "tram"));
            var stop = factory.createTransitStopFacility(Id.create("stop" + i, TransitStopFacility.class),
                    new Coord(i * 1000 + 500, 0), false);
            stop.setLinkId(link.getId());
            schedule.addStopFacility(stop);
            stops.add(stop);
        }
        for (int i = 0; i < 2; i++) {
            var route = factory.createTransitRoute(Id.create("route" + i, TransitRoute.class),
                    RouteUtils.createLinkNetworkRouteImpl(stops.get(i).getLinkId(), List.of(), stops.get(i + 1).getLinkId()),
                    List.of(factory.createTransitRouteStop(stops.get(i), 0, 0),
                            factory.createTransitRouteStop(stops.get(i + 1), 100, 100)),
                    i == 0 ? "bus" : "tram");
            route.addDeparture(factory.createDeparture(Id.create("departure" + i, Departure.class), 100 + i * 200));
            var line = factory.createTransitLine(Id.create("line" + i, TransitLine.class));
            line.addRoute(route);
            schedule.addTransitLine(line);
        }
        var data = SwissRailRaptorData.create(schedule, scenario.getTransitVehicles(),
                RaptorUtils.createStaticConfig(config), network, new OccupancyData());
        var request = DefaultRoutingRequest.withoutAttributes(
                FacilitiesUtils.wrapLinkAndCoord(network.getLinks().get(Id.createLinkId(0)), stops.get(0).getCoord()),
                FacilitiesUtils.wrapLinkAndCoord(network.getLinks().get(Id.createLinkId(2)), stops.get(2).getCoord()),
                0, null);
        // Every worker owns its own router, sharing only the immutable timetable.
        for (int worker = 0; worker < 2; worker++) {
            var result = RunComputeTransitPrices.createRouter(data, config).calcRoute(request);
            assertNotNull(result);
            var ptRoutes = result.stream().filter(Leg.class::isInstance).map(Leg.class::cast)
                    .map(Leg::getRoute).filter(TransitPassengerRoute.class::isInstance)
                    .map(TransitPassengerRoute.class::cast).toList();
            assertEquals("Must actually exercise the transfer between bus and tram", 2, ptRoutes.size());
            assertEquals("route0", ptRoutes.get(0).getRouteId().toString());
            assertEquals("route1", ptRoutes.get(1).getRouteId().toString());
            ptRoutes.forEach(route -> assertTrue(Double.isFinite(route.getTravelTime().seconds())));
        }
    }
}
