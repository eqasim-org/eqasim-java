package org.eqasim.switzerland.ch_cmdp.walk;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.*;
import org.junit.Test;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesUtils;

/** All workers share the immutable model/calculator/index, while owning their routing wrappers. */
public class TestParallelWalkRouting {
    @Test(timeout = 60000) public void sharedCalculatorGivesIdenticalIndependentRoutesAcrossWorkers() throws Exception {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        var network = scenario.getNetwork();
        for (int x = 0; x < 100; x++) for (int y = 0; y < 100; y++)
            NetworkUtils.createAndAddNode(network, Id.createNodeId(x*100+y),
                    new Coord(x*100, y*100, 400+(x*3+y)%200));
        var link = NetworkUtils.createAndAddLink(network, Id.createLinkId("link"),
                network.getNodes().get(Id.createNodeId(0)), network.getNodes().get(Id.createNodeId(1)), 100, 10, 1000, 1);
        var calculator = new WalkTravelTimeCalculator(scenario, new MobisWalkTravelTime());
        var params = scenario.getConfig().routing().getOrCreateModeRoutingParams("walk");
        var requests = new ArrayList<RoutingRequest>();
        double[] expected = new double[2048];
        var random = new Random(4711);
        for (int i = 0; i < expected.length; i++) {
            var from = FacilitiesUtils.wrapLinkAndCoord(link, new Coord(random.nextDouble()*9900, random.nextDouble()*9900));
            var to = FacilitiesUtils.wrapLinkAndCoord(link, new Coord(random.nextDouble()*9900, random.nextDouble()*9900));
            requests.add(DefaultRoutingRequest.withoutAttributes(from, to, 18000+i, null));
            expected[i] = calculator.calculate(from.getCoord(), to.getCoord(), null, 18000+i, 0);
        }
        for (int workers : new int[]{1, 8, 16}) {
            var pool = Executors.newFixedThreadPool(workers);
            var start = new CyclicBarrier(workers+1);
            var tasks = new ArrayList<Future<?>>();
            try {
                for (int worker = 0; worker < workers; worker++) {
                    final int offset = worker;
                    tasks.add(pool.submit(() -> {
                        var router = new WalkRoutingModule(DefaultRoutingModules.createTeleportationRouter("walk", scenario, params), calculator);
                        start.await(15, TimeUnit.SECONDS);
                        for (int repeat = 0; repeat < 10; repeat++) for (int i = offset; i < requests.size(); i += workers) {
                            var route = (Leg) router.calcRoute(requests.get(i)).getFirst();
                            assertEquals(expected[i], route.getTravelTime().seconds(), 0);
                            assertEquals(expected[i], route.getRoute().getTravelTime().seconds(), 0);
                            assertEquals(requests.get(i).getDepartureTime(), route.getDepartureTime().seconds(), 0);
                            // A caller mutating a returned route must not contaminate later queries or other workers.
                            route.setTravelTime(-1); route.getRoute().setTravelTime(-1);
                        }
                        return null;
                    }));
                }
                start.await(15, TimeUnit.SECONDS);
                for (var task : tasks) task.get(30, TimeUnit.SECONDS);
            } finally {
                pool.shutdownNow();
                assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
            }
        }
    }
}
