// Regression tests for charging static mode-specific penalties once per actual transfer.
package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.eqasim.core.components.raptor.*;
import org.matsim.api.core.v01.*;
import org.matsim.core.config.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.junit.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class TestModeSpecificTransferCostCalculator {
    @Test public void chargesEachDirectionalPenaltyOnce() {
        check(.218,.218,0,0,Double.POSITIVE_INFINITY,false);
        check(.1,.3,0,0,Double.POSITIVE_INFINITY,false);
    }

    @Test public void handlesZeroPenaltiesWithoutReplacingEarlierTransfers() {
        check(.218,0,0,0,Double.POSITIVE_INFINITY,false);
        check(0,.218,0,0,Double.POSITIVE_INFINITY,false);
        check(0,0,0,0,Double.POSITIVE_INFINITY,false);
    }

    @Test public void appliesBaseAndBoundsOncePerTransfer() {
        check(.1,.3,.05,.2,.25,false);
    }

    @Test public void preservesTravelAndWaitingCosts() {
        check(.1,.3,0,0,Double.POSITIVE_INFINITY,true);
    }

    @Test public void unchangedTransferCountDoesNotEvaluateTheTransferSupplier() {
        var params = new RaptorParameters(new SwissRailRaptorConfigGroup());
        var calculator = new ModeSpecificTransferCostCalculator();
        assertEquals(0, calculator.calcTransferCost(path(1), () -> {
            throw new AssertionError("A previously charged transfer must not be evaluated again");
        }, new RaptorStaticConfig(), params, 200, 1, .218, 200), 0);
    }

    @Test public void stillRejectsTimeDependentPenaltiesWithoutANewTransfer() {
        var params = new RaptorParameters(new SwissRailRaptorConfigGroup());
        params.setTransferPenaltyPerTravelTimeHour(.1);
        var error = assertThrows(IllegalStateException.class, () ->
            new ModeSpecificTransferCostCalculator().calcTransferCost(path(1), null,
                new RaptorStaticConfig(), params, 200, 1, .218, 200));
        assertTrue(error.getMessage().contains("time-based transfer penalty"));
    }

    @Test public void rejectsDecreasingTransferCounts() {
        assertThrows(IllegalArgumentException.class, () ->
            new ModeSpecificTransferCostCalculator().calcTransferCost(path(2), null,
                new RaptorStaticConfig(), new RaptorParameters(new SwissRailRaptorConfigGroup()),
                200, 1, .218, 200));
    }

    @Test public void defaultCalculatorStillUpdatesTimeDependentCosts() {
        var params = new RaptorParameters(new SwissRailRaptorConfigGroup());
        params.setTransferPenaltyFixCostPerTransfer(.1);
        params.setTransferPenaltyPerTravelTimeHour(.36);
        params.setTransferPenaltyMinimum(0);
        params.setTransferPenaltyMaximum(Double.POSITIVE_INFINITY);
        var calculator = new DefaultRaptorTransferCostCalculator();
        var config = new RaptorStaticConfig();
        double initial = calculator.calcTransferCost(path(0), null, config, params, 100, 1, 0, 100);
        double update = calculator.calcTransferCost(path(1), null, config, params, 200, 1, initial, 200);
        assertEquals(.11, initial, 1e-12);
        assertEquals(.01, update, 1e-12);
    }

    private static SwissRailRaptorCore.PathElement path(int count) {
        return new SwissRailRaptorCore.PathElement(null,null,0,0,0,0,0,0,0,count,false,false,null,null);
    }

    private static void check(double first, double second, double base, double minimum,
            double maximum, boolean timeCosts) {
        var scenario = scenario(first, second);
        var config = scenario.getConfig();
        var eqasim = ConfigUtils.addOrGetModule(config, EqasimRaptorConfigGroup.class);
        eqasim.perTransfer_u = -base;
        if (timeCosts) {
            eqasim.travelTimeBus_u_h = -3.6;
            eqasim.travelTimeRail_u_h = -7.2;
            eqasim.waitTime_u_h = -1.8;
        }
        var parameters = EqasimRaptorUtils.createParameters(config, eqasim, scenario.getTransitSchedule(), false);
        parameters.setTransferPenaltyMinimum(minimum);
        parameters.setTransferPenaltyMaximum(maximum);
        var data = SwissRailRaptorData.create(scenario.getTransitSchedule(),null,
                RaptorUtils.createStaticConfig(config),scenario.getNetwork(),null);
        var calls = new AtomicInteger();
        var calculator = new ModeSpecificTransferCostCalculator((from,to,settings) -> {
            calls.incrementAndGet();
            return settings.getModeToModeTransferPenalty(from,to);
        });
        var core = new SwissRailRaptorCore(data,new DefaultRaptorInVehicleCostCalculator(),calculator);
        var from = scenario.getTransitSchedule().getFacilities().get(Id.create("s0",TransitStopFacility.class));
        var to = scenario.getTransitSchedule().getFacilities().get(Id.create("s3",TransitStopFacility.class));
        var result = core.calcLeastCostRoute(90,from,to,List.of(new InitialStop(from,0,0,0,"walk")),
                List.of(new InitialStop(to,0,0,0,"walk")),parameters,null);
        assertNotNull(result);
        var modes = new ArrayList<String>();
        for (var part : result.getParts()) if (part.line != null) modes.add(part.route.getTransportMode());
        assertEquals(List.of("bus","rail","bus"), modes);
        assertEquals(2, result.getNumberOfTransfers());
        double expected = Math.min(maximum,Math.max(minimum,base+first))
                + Math.min(maximum,Math.max(minimum,base+second));
        // 200 s bus + 100 s rail + 210 s initial/transfer waiting, with the coefficients above.
        if (timeCosts) expected += .505;
        assertEquals(expected,result.getTotalCosts(),1e-12);
        assertEquals("One supplier evaluation per actual transfer",2,calls.get());
        if (!timeCosts) {
            calls.set(0);
            var range = core.calcRoutes(90,90,110,from,to,
                    List.of(new InitialStop(from,0,0,0,"walk")),
                    List.of(new InitialStop(to,0,0,0,"walk")),parameters,null);
            assertEquals(1, range.size());
            assertEquals(2, range.getFirst().getNumberOfTransfers());
            assertEquals(expected, range.getFirst().getTotalCosts(), 1e-12);
            assertEquals("Range queries also charge once per actual transfer",2,calls.get());
        }
    }

    static Scenario scenario(double firstPenalty, double secondPenalty) {
        var config = ConfigUtils.createConfig(new SwissRailRaptorConfigGroup(), new EqasimRaptorConfigGroup());
        config.transit().setUseTransit(true);
        config.transitRouter().setAdditionalTransferTime(0);
        config.transitRouter().setMaxBeelineWalkConnectionDistance(0);
        config.routing().getOrCreateModeRoutingParams("walk").setTeleportedModeSpeed(1.0);
        config.routing().getOrCreateModeRoutingParams("walk").setBeelineDistanceFactor(1.0);
        var eqasim = ConfigUtils.addOrGetModule(config, EqasimRaptorConfigGroup.class);
        eqasim.travelTimeBus_u_h = eqasim.travelTimeRail_u_h = eqasim.travelTimeOther_u_h = 0;
        eqasim.travelTimeSubway_u_h = eqasim.travelTimeTram_u_h = 0;
        eqasim.walkTime_u_h = eqasim.waitTime_u_h = eqasim.perTransfer_u = 0;
        var raptor = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
        raptor.setTransferWalkMargin(0);
        raptor.addModeToModeTransferPenalty(new SwissRailRaptorConfigGroup.ModeToModeTransferPenalty("bus", "rail", firstPenalty));
        raptor.addModeToModeTransferPenalty(new SwissRailRaptorConfigGroup.ModeToModeTransferPenalty("rail", "bus", secondPenalty));
        var scenario = ScenarioUtils.createScenario(config);
        var network = scenario.getNetwork();
        for (int i=0;i<5;i++) NetworkUtils.createAndAddNode(network, Id.createNodeId("n"+i), new Coord(i*10000,0));
        for (int i=0;i<4;i++) NetworkUtils.createAndAddLink(network, Id.createLinkId("l"+i), network.getNodes().get(Id.createNodeId("n"+i)), network.getNodes().get(Id.createNodeId("n"+(i+1))),10000,20,1000,1);
        var factory = scenario.getTransitSchedule().getFactory();
        for (int i=0;i<4;i++) {
            var stop = factory.createTransitStopFacility(Id.create("s"+i, TransitStopFacility.class),new Coord(i*10000,0),false);
            stop.setLinkId(Id.createLinkId("l"+i));scenario.getTransitSchedule().addStopFacility(stop);
        }
        for (int i=0;i<3;i++) {
            String mode = i==1?"rail":"bus";
            String name = "ride"+i;
            var from = scenario.getTransitSchedule().getFacilities().get(Id.create("s"+i,TransitStopFacility.class));
            var to = scenario.getTransitSchedule().getFacilities().get(Id.create("s"+(i+1),TransitStopFacility.class));
            var path = RouteUtils.createLinkNetworkRouteImpl(from.getLinkId(),to.getLinkId());
            path.setLinkIds(from.getLinkId(),List.of(),to.getLinkId());
            var route = factory.createTransitRoute(Id.create(name,TransitRoute.class),path,List.of(factory.createTransitRouteStop(from,0,0),factory.createTransitRouteStop(to,100,100)),mode);
            route.addDeparture(factory.createDeparture(Id.create("departure"+i,Departure.class),100+200*i));
            var line = factory.createTransitLine(Id.create(name,TransitLine.class));line.addRoute(route);scenario.getTransitSchedule().addTransitLine(line);
        }
        return scenario;
    }

}
