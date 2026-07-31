package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations;

import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.Processors.CountsProcessor;
import org.eqasim.core.components.network_calibration.Processors.FlowProcessor;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.TripRouter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Calibrates subpopulation demand against traffic counts.
 *
 * <p>The algorithm has two phases:</p>
 * <ol>
 *   <li><b>Warm-up cloning</b> (first two calibration iterations): only cross-border
 *       agents are cloned at counting stations where the model under-estimates and
 *       cross-border traffic is the main responsible.</li>
 *   <li><b>Reduction / restoration</b> (later iterations): agents are reduced to a
 *       home-only plan when they participate more in over-estimation than under-estimation,
 *       and previously removed agents are restored when their original plan helps
 *       under-estimated stations. Link errors are updated after every single agent
 *       change so decisions are always made against the current error landscape.</li>
 * </ol>
 */
public class Calibrator implements IterationEndsListener {
    private static final Logger logger = LogManager.getLogger(Calibrator.class);

    public static boolean DEBUG = false;

    public static int WARM_UP_ITERATIONS = DEBUG ? 0 : 20;
    public static int ITERATION_INTERVAL_BELOW_50_ITERATIONS = 10;
    public static int ITERATION_INTERVAL = 5;

    public static double FLOW_OVER_ESTIMATION_THRESHOLD = 0.20;
    public static double FLOW_UNDER_ESTIMATION_THRESHOLD = DEBUG ? 0.0 : 0.20;
    public static double REDUCTION_TOLERANCE = 0.15;

    public static double SUBPOPULATION_SHARE_THRESHOLD = 0.20;
    public static double CROSSBORDER_SHARE_THRESHOLD = DEBUG ? 0.0 : 0.40;
    public static int MIN_TRAVERSALS_PER_LINK = DEBUG ? 1 : 20;

    public static double RESTORE_OVER_PENALTY_WEIGHT = 0.7;
    public static double CLONE_OVER_PENALTY_WEIGHT = 0.7;
    public static double EXPANSION_DAMPING = 0.7;

    public static double RELOCATION_RADIUS_METERS = 3_000.0;

    private final Population population;
    private final boolean calibrate;
    private final Random random;

    private final State state;
    private final Analytics analytics;
    private final CrossBorderCloneFactory cloneFactory;
    private final FlowProcessor flowProcessor;
    private final double sampleSize;
    private final CalibrationFormulas.Parameters parameters;

    public Calibrator(Scenario scenario,
                      TripListConverter tripListConverter,
                      Provider<CountsProcessor> countsProcessorProvider,
                      Provider<FlowProcessor> flowProcessorProvider,
                      EqasimConfigGroup eqasimConfig,
                      NetworkCalibrationConfigGroup calConfig,
                      Provider<TripRouter> tripRouterProvider) {
        this.population = scenario.getPopulation();
        this.calibrate = calConfig.getAllObjectives().contains("subpopulations") && calConfig.isCalibrationEnabled();
        this.random = MatsimRandom.getLocalInstance();

        this.state = new State();

        CountsProcessor countsProcessor = calibrate ? countsProcessorProvider.get() : null;
        this.flowProcessor = calibrate ? flowProcessorProvider.get() : null;
        this.sampleSize = eqasimConfig.getSampleSize();
        this.analytics = calibrate ? new Analytics(tripListConverter, countsProcessor) : null;

        this.cloneFactory = calibrate
                ? new CrossBorderCloneFactory(scenario, tripListConverter, tripRouterProvider, random, RELOCATION_RADIUS_METERS)
                : null;

        this.parameters = new CalibrationFormulas.Parameters(
                FLOW_OVER_ESTIMATION_THRESHOLD,
                FLOW_UNDER_ESTIMATION_THRESHOLD,
                REDUCTION_TOLERANCE,
                SUBPOPULATION_SHARE_THRESHOLD,
                CROSSBORDER_SHARE_THRESHOLD,
                MIN_TRAVERSALS_PER_LINK,
                RESTORE_OVER_PENALTY_WEIGHT,
                CLONE_OVER_PENALTY_WEIGHT,
                EXPANSION_DAMPING
        );
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        int iteration = event.getIteration();
        if (doUpdate(iteration)) {
            calibrateSubpopulations(iteration);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    //  Update schedule
    // -----------------------------------------------------------------------------------------------------------------

    private boolean doUpdate(int iteration) {
        if (DEBUG) {
            return true;
        }
        if (!calibrate || iteration < WARM_UP_ITERATIONS) {
            return false;
        }
        return doCloning(iteration) || doReduction(iteration);
    }

    private boolean doCloning(int iteration) {
        return iteration == WARM_UP_ITERATIONS
                || iteration == WARM_UP_ITERATIONS + ITERATION_INTERVAL;
    }

    private boolean doReduction(int iteration) {
        if (iteration <= WARM_UP_ITERATIONS + ITERATION_INTERVAL) {
            return false;
        }
        if (iteration <= 50) {
            return iteration % ITERATION_INTERVAL_BELOW_50_ITERATIONS == 0;
        }
        return iteration % ITERATION_INTERVAL == 0;
    }

    // -----------------------------------------------------------------------------------------------------------------
    //  Main calibration loop
    // -----------------------------------------------------------------------------------------------------------------

    private void calibrateSubpopulations(int iteration) {
        state.incrementCalibrationSteps();

        if (doCloning(iteration)) {
            int clonedNow = performWarmUpCloning();
            logger.info("Subpopulation calibration [iter {}]: warm-up cloning, cloned={}", iteration, clonedNow);
            return;
        }

        if (doReduction(iteration)) {
            Analytics.PopulationTraversalStats stats = analytics.collectPopulationTraversalStats(
                    state.removedPlansView(), population.getPersons().values()
            );

            LinkErrorTracker tracker = new LinkErrorTracker(
                    analytics.countsProcessor(),
                    flowProcessor,
                    stats.allTraversals(),
                    stats.subpopulationTraversals(),
                    stats.crossBorderTraversals(),
                    sampleSize,
                    parameters
            );

            int restoredNow = restoreRemovedAgents(stats, tracker);
            int removedNow = reduceActiveAgents(stats, tracker);

            logger.info(
                    "Subpopulation calibration [iter {}]: monitored-links={}, removed={}, restored={}, active-removed={}",
                    iteration,
                    tracker.monitoredLinks().size(),
                    removedNow,
                    restoredNow,
                    state.removedPersonIds().size()
            );
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    //  Warm-up: clone cross-border agents to fix under-estimated stations
    // -----------------------------------------------------------------------------------------------------------------

    private int performWarmUpCloning() {
        Analytics.PopulationTraversalStats stats = analytics.collectPopulationTraversalStats(
                state.removedPlansView(), population.getPersons().values()
        );

        LinkErrorTracker tracker = new LinkErrorTracker(
                analytics.countsProcessor(),
                flowProcessor,
                stats.allTraversals(),
                stats.subpopulationTraversals(),
                stats.crossBorderTraversals(),
                sampleSize,
                parameters
        );

        List<ScoredAgent> donors = new ArrayList<>();
        for (Map.Entry<Id<Person>, Map<Id<Link>, Integer>> entry : stats.crossBorderPersonTraversals().entrySet()) {
            double score = CalibrationFormulas.cloneScore(entry.getValue(), tracker, parameters);
            if (score > 0.0) {
                donors.add(new ScoredAgent(entry.getKey(), entry.getValue(), score));
            }
        }

        if (donors.isEmpty()) {
            return 0;
        }

        donors.sort(Comparator.comparingDouble(ScoredAgent::score).reversed());

        int cloneCount = CalibrationFormulas.computeCloneCount(tracker, parameters);
        cloneCount = Math.min(cloneCount, donors.size());
        if (cloneCount <= 0) {
            return 0;
        }

        List<Id<Person>> selectedDonors = donors.stream()
                .limit(cloneCount)
                .map(ScoredAgent::personId)
                .collect(Collectors.toList());

        return cloneFactory.applyCloning(selectedDonors, cloneCount);
    }

    // -----------------------------------------------------------------------------------------------------------------
    //  Reduction: turn over-estimating subpopulation agents into home-only plans
    // -----------------------------------------------------------------------------------------------------------------

    private int reduceActiveAgents(Analytics.PopulationTraversalStats stats, LinkErrorTracker tracker) {
        List<ScoredAgent> candidates = new ArrayList<>();
        for (Map.Entry<Id<Person>, Map<Id<Link>, Integer>> entry : stats.personTraversals().entrySet()) {
            Person person = population.getPersons().get(entry.getKey());
            if (person == null || state.isRemoved(entry.getKey())) {
                continue;
            }
            double score = CalibrationFormulas.removalScore(entry.getValue(), tracker, parameters);
            if (score > 0.0) {
                candidates.add(new ScoredAgent(entry.getKey(), entry.getValue(), score));
            }
        }

        candidates.sort(Comparator.comparingDouble(ScoredAgent::score).reversed());

        int removed = 0;
        for (ScoredAgent candidate : candidates) {
            Person person = population.getPersons().get(candidate.personId());
            if (person == null || state.isRemoved(candidate.personId())) {
                continue;
            }

            // Re-evaluate against the up-to-date error landscape.
            double currentScore = CalibrationFormulas.removalScore(candidate.traversals(), tracker, parameters);
            if (currentScore <= 0.0) {
                continue;
            }

            Plan oldPlan = person.getSelectedPlan();
            if (oldPlan == null) {
                continue;
            }

            if (reduceToHomePlan(person, oldPlan)) {
                tracker.removeAgent(candidate.traversals());
                state.markRemoved(person, oldPlan);
                removed++;
            }
        }

        return removed;
    }

    // -----------------------------------------------------------------------------------------------------------------
    //  Restoration: bring back previously removed agents for under-estimated stations
    // -----------------------------------------------------------------------------------------------------------------

    private int restoreRemovedAgents(Analytics.PopulationTraversalStats stats, LinkErrorTracker tracker) {
        List<ScoredAgent> candidates = new ArrayList<>();
        for (Map.Entry<Id<Person>, Map<Id<Link>, Integer>> entry : stats.removedPersonTraversals().entrySet()) {
            Person person = population.getPersons().get(entry.getKey());
            if (person == null || !state.isRemoved(entry.getKey())) {
                continue;
            }
            double score = CalibrationFormulas.restoreScore(entry.getValue(), tracker, parameters);
            if (score > 0.0) {
                candidates.add(new ScoredAgent(entry.getKey(), entry.getValue(), score));
            }
        }

        candidates.sort(Comparator.comparingDouble(ScoredAgent::score).reversed());

        int restored = 0;
        for (ScoredAgent candidate : candidates) {
            Person person = population.getPersons().get(candidate.personId());
            State.StoredPersonPlan stored = state.getStoredPlan(candidate.personId());
            if (person == null || stored == null) {
                continue;
            }

            // Re-evaluate against the up-to-date error landscape.
            double currentScore = CalibrationFormulas.restoreScore(candidate.traversals(), tracker, parameters);
            if (currentScore <= 0.0) {
                continue;
            }

            if (restorePersonPlan(person, stored.originalPlan())) {
                tracker.addAgent(candidate.traversals());
                state.markRestored(person);
                restored++;
            }
        }

        return restored;
    }

    // -----------------------------------------------------------------------------------------------------------------
    //  Plan manipulation helpers
    // -----------------------------------------------------------------------------------------------------------------

    private boolean reduceToHomePlan(Person person, Plan originalPlan) {
        Plan homePlan = population.getFactory().createPlan();
        for (PlanElement element : originalPlan.getPlanElements()) {
            if (element instanceof Activity activity) {
                homePlan.addActivity(activity);
                break;
            }
        }

        if (homePlan.getPlanElements().isEmpty()) {
            return false;
        }

        person.removePlan(originalPlan);
        person.addPlan(homePlan);
        person.setSelectedPlan(homePlan);
        return true;
    }

    private boolean restorePersonPlan(Person person, Plan originalPlan) {
        Plan currentPlan = person.getSelectedPlan();
        if (currentPlan != null) {
            person.removePlan(currentPlan);
        }
        person.addPlan(originalPlan);
        person.setSelectedPlan(originalPlan);
        return true;
    }

    // -----------------------------------------------------------------------------------------------------------------
    //  Records
    // -----------------------------------------------------------------------------------------------------------------

    private record ScoredAgent(Id<Person> personId, Map<Id<Link>, Integer> traversals, double score) {
    }
}