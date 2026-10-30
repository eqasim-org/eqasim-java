package org.matsim.core.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eqasim.core.tools.random.Uniform;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.utils.timing.TimeInterpretation;
import static org.junit.Assert.*;

public class TestRepeatablePlanRouter {
    private static final double IMPROVED_TIME = 1000 - 800 * Math.log(2);

    private static TripRouter router(Config config) {
        return new TripRouter.Builder(config).setRoutingModule("car", request -> {
            var leg = PopulationUtils.createLeg("car");
            leg.setTravelTime(IMPROVED_TIME);
            leg.setRoute(org.matsim.core.population.routes.RouteUtils.createGenericRouteImpl(null, null));
            TripStructureUtils.setRoutingMode(leg, "car");
            return List.of(leg);
        }).build();
    }

    private static Plan plan(int id) {
        var person = PopulationUtils.getFactory().createPerson(Id.createPersonId(id));
        var plan = PopulationUtils.createPlan(person);
        var home = PopulationUtils.createActivityFromCoord("home", new Coord(0, 0));
        home.setEndTime(100);
        plan.addActivity(home);
        var leg = PopulationUtils.createLeg("car");
        leg.setTravelTime(1000);
        // Complete existing routes exercise acceptance; missing routes must always be prepared.
        leg.setRoute(org.matsim.core.population.routes.RouteUtils.createGenericRouteImpl(null, null));
        TripStructureUtils.setRoutingMode(leg, "car");
        plan.addLeg(leg);
        plan.addActivity(PopulationUtils.createActivityFromCoord("work", new Coord(1000, 0)));
        person.addPlan(plan);
        return plan;
    }

    private static boolean accepted(Plan plan) {
        return ((Leg) plan.getPlanElements().get(1)).getTravelTime().seconds() == IMPROVED_TIME;
    }

    @Test public void strategyUsesIterationAndPreservesDecisionsAcrossWorkerCountsAndOrder() {
        var config = ConfigUtils.createConfig();
        config.global().setRandomSeed(518745);
        var expected = new boolean[2][256];
        for (int iteration = 1; iteration <= 2; iteration++) {
            var direct = new PlanRouter(router(config), null, TimeInterpretation.create(config), iteration);
            for (int id = 0; id < 256; id++) {
                var plan = plan(id); direct.run(plan); expected[iteration - 1][id] = accepted(plan);
                assertEquals(Uniform.get(Integer.toString(id), 518745, iteration, 0) < 0.5, accepted(plan));
            }
            for (int workers : new int[]{0, 1, 4}) {
                config.global().setNumberOfThreads(workers);
                var module = new ReRoute(null, () -> router(config), config.global(), TimeInterpretation.create(config));
                final int current = iteration;
                module.prepareReplanning(() -> current);
                var plans = new ArrayList<Plan>();
                for (int id = 0; id < 256; id++) plans.add(plan(id));
                Collections.reverse(plans);
                for (Plan plan : plans) module.handlePlan(plan);
                module.finishReplanning();
                for (Plan plan : plans) assertEquals(expected[iteration - 1][Integer.parseInt(plan.getPerson().getId().toString())], accepted(plan));
            }
        }
        int different = 0;
        for (int id = 0; id < 256; id++) if (expected[0][id] != expected[1][id]) different++;
        assertTrue(different > 64);
        assertTrue(different < 192);
    }

    @Test public void missingRoutesAreInstalledRegardlessOfAcceptanceDraw() {
        var config = ConfigUtils.createConfig();
        var router = new PlanRouter(router(config), null, TimeInterpretation.create(config), 1);
        for (int id = 0; id < 32; id++) {
            var plan = plan(id);
            ((Leg) plan.getPlanElements().get(1)).setRoute(null);
            router.run(plan);
            assertTrue(accepted(plan));
            assertNotNull(((Leg) plan.getPlanElements().get(1)).getRoute());
        }
    }

    @Test public void drawsChangeWithSeedAndTripAndStayUniform() {
        double sum = 0;
        int changedSeed = 0, changedTrip = 0;
        for (int id = 0; id < 10000; id++) {
            String key = "person-" + id;
            double draw = Uniform.get(key, 42, 3, 0);
            assertTrue(draw >= 0 && draw < 1);
            assertEquals(draw, Uniform.get(key, 42, 3, 0), 0);
            sum += draw;
            if (draw != Uniform.get(key, 43, 3, 0)) changedSeed++;
            if (draw != Uniform.get(key, 42, 3, 1)) changedTrip++;
        }
        assertEquals(10000, changedSeed);
        assertEquals(10000, changedTrip);
        assertEquals(0.5, sum / 10000, 0.015);
    }
}
