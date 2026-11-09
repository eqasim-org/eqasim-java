package org.eqasim.core.components.travel_disutility;

import java.util.concurrent.Callable;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import org.eqasim.core.simulation.policies.routing.ZeroRoutingPenalty;
import org.eqasim.core.tools.random.Normal;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelDisutility;
import static org.junit.Assert.*;

public class TestRoutingNoise {
    @Test
    public void sharedLinkPrefixesPreserveCostsIncludingLateIdsAndChangedSigma() throws Exception {
        Link link = link("noise-test-existing");
        var factory = new EqasimTravelDisutilityFactory(new ZeroRoutingPenalty(), 1.25, 0.7, 1997);
        var costs = factory.createTravelDisutility((l, time, person, vehicle) -> 80);
        Link late = link("noise-test-created-after-snapshot");
        compare(costs, link, 0.7);
        compare(costs, late, 0.7);
        factory.setSigmaNoise(0.2);
        var changed = factory.createTravelDisutility((l, time, person, vehicle) -> 80);
        compare(changed, link, 0.2);
        compare(changed, late, 0.2);
        compare(costs, link, 0.7); // Existing workers retain their original sigma.
        try (var executor = Executors.newFixedThreadPool(6)) {
            var tasks = new ArrayList<Callable<Boolean>>();
            for (int worker = 0; worker < 6; worker++) tasks.add(() -> {
                var local = factory.createTravelDisutility((l, time, person, vehicle) -> 80);
                compare(local, link, 0.2);
                compare(local, late, 0.2);
                return true;
            });
            for (var future : executor.invokeAll(tasks)) assertTrue(future.get());
        }
    }

    @Test
    public void randomnessDisabledDoesNotRequirePerson() {
        var factory = new EqasimTravelDisutilityFactory(new ZeroRoutingPenalty(), 0);
        var costs = factory.createTravelDisutility((link, time, person, vehicle) -> 80);
        assertEquals(80, costs.getLinkTravelDisutility(link("no-noise"), 100, null, null), 0);
    }

    private static void compare(TravelDisutility costs, Link link, double sigma) {
        for (int i = 0; i < 100; i++) {
            var person = PopulationUtils.getFactory().createPerson(Id.createPersonId("noise-person-" + i));
            double expected = (80 + 1.25 * (link.getLength() / 10.0))
                    * Math.max(1.0 + sigma * Normal.get(link.getId().toString() + person.getId(), 1997), 1e-3);
            assertEquals(Double.doubleToRawLongBits(expected),
                    Double.doubleToRawLongBits(costs.getLinkTravelDisutility(link, 100, person, null)));
        }
    }

    private static Link link(String id) {
        var factory = NetworkUtils.createNetwork().getFactory();
        var from = factory.createNode(Id.createNodeId(id + "-from"), new Coord(0, 0));
        var to = factory.createNode(Id.createNodeId(id + "-to"), new Coord(100, 0));
        var link = factory.createLink(Id.createLinkId(id), from, to);
        link.setLength(100);
        return link;
    }
}
