package org.eqasim.switzerland.ch_cmdp.routing;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.facilities.FacilitiesUtils;

/** Selective initial routing shared by simulation startup and the standalone population tool. */
public final class MissingTripRouting {
    private MissingTripRouting() { }

    /** A network leg needs a network route; a teleported bike leg must not retain an old network route. */
    public static boolean needsRouting(TripStructureUtils.Trip trip, Scenario scenario) {
        if (trip.getLegsOnly().isEmpty()) return true;
        for (Leg leg : trip.getLegsOnly()) {
            var route = leg.getRoute();
            if (route == null || !leg.getTravelTime().isDefined()) return true;
            boolean network = scenario.getConfig().routing().getNetworkModes().contains(leg.getMode());
            if (network) {
                if (!(route instanceof NetworkRoute)) return true;
                if (route.getStartLinkId() == null || route.getEndLinkId() == null) return true;
                var from = scenario.getNetwork().getLinks().get(route.getStartLinkId());
                var to = scenario.getNetwork().getLinks().get(route.getEndLinkId());
                if (from == null || to == null || !from.getAllowedModes().contains(leg.getMode())
                        || !to.getAllowedModes().contains(leg.getMode())) return true;
            } else if (leg.getMode().equals("bike") && route instanceof NetworkRoute) return true;
        }
        return false;
    }

    /** Create one router per worker only if a selected trip actually needs repair. */
    public static void run(Scenario scenario, int threads, Set<String> modes, Provider<Worker> workers) {
        if (threads < 1) throw new IllegalArgumentException("threads must be positive");
        if (!hasMissingRoutes(scenario, modes)) return;
        var repaired = new LongAdder();
        ParallelPersonAlgorithmUtils.run(scenario.getPopulation(), threads,
                (ParallelPersonAlgorithmUtils.PersonAlgorithmProvider) () -> {
                    Worker worker;
                    synchronized (workers) { worker = workers.get(); }
                    return person -> person.getPlans().forEach(plan -> repaired.add(worker.run(plan, modes)));
                });
        LogManager.getLogger(MissingTripRouting.class).info("Prepared {} missing/incompatible trip routes", repaired.sum());
    }

    /** Fast preflight lets the standalone tool skip building a routing injector for an already routed population. */
    public static boolean hasMissingRoutes(Scenario scenario, Set<String> modes) {
        return scenario.getPopulation().getPersons().values().stream().anyMatch(person ->
                person.getPlans().stream().anyMatch(plan -> TripStructureUtils.getTrips(plan).stream()
                        .anyMatch(trip -> selected(trip, modes) && needsRouting(trip, scenario))));
    }

    private static boolean selected(TripStructureUtils.Trip trip, Set<String> modes) {
        return modes.isEmpty() || modes.contains(TripStructureUtils.identifyMainMode(trip.getTripElements()));
    }

    public static final class Worker {
        private final Scenario scenario;
        private final TripRouter router;
        private final TimeInterpretation time;
        @Inject public Worker(Scenario scenario, TripRouter router, TimeInterpretation time) {
            this.scenario = scenario; this.router = router; this.time = time;
        }

        /** Preserve complete trips and their objects; account for their times when routing subsequent trips. */
        public long run(Plan plan, Set<String> modes) {
            var tracker = new TimeTracker(time);
            long repaired = 0;
            for (var trip : TripStructureUtils.getTrips(plan)) {
                tracker.addActivity(trip.getOriginActivity());
                List<? extends PlanElement> elements = trip.getTripElements();
                if (selected(trip, modes) && needsRouting(trip, scenario)) {
                    if (!tracker.getTime().isDefined()) throw new IllegalStateException("Undefined departure time for " + plan.getPerson().getId());
                    String mode = TripStructureUtils.identifyMainMode(elements);
                    elements = router.calcRoute(mode,
                            FacilitiesUtils.toFacility(trip.getOriginActivity(), scenario.getActivityFacilities()),
                            FacilitiesUtils.toFacility(trip.getDestinationActivity(), scenario.getActivityFacilities()),
                            tracker.getTime().seconds(), plan.getPerson(), trip.getTripAttributes());
                    if (elements == null || elements.isEmpty()) throw new IllegalStateException("No " + mode + " route for " + plan.getPerson().getId());
                    org.matsim.core.router.PlanRouter.putVehicleFromOldTripIntoNewTripIfMeaningful(trip, elements);
                    TripRouter.insertTrip(plan, trip.getOriginActivity(), elements, trip.getDestinationActivity());
                    repaired++;
                }
                tracker.addElements(elements);
            }
            return repaired;
        }
    }
}
