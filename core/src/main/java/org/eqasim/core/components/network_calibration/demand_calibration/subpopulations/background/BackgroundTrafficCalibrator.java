package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.background;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.network_calibration.demand_calibration.Tools;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficCategory;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficScore;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficScoringTracker;
import org.eqasim.core.misc.Constants;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;

/**
 * Runs one location-calibration step for freight and cross-border traffic.
 * Selection, parallel proposal generation, and sequential live-state commits
 * are kept as three explicit phases.
 */
public final class BackgroundTrafficCalibrator {
    private static final Logger logger = LogManager.getLogger(BackgroundTrafficCalibrator.class);

    private final Population population;
    private final BackgroundPlanRelocator relocator;
    private final int numberOfThreads;
    private final long randomSeed;
    private final Random random;
    private final double fraction;

    public BackgroundTrafficCalibrator(Population population,
                                       BackgroundPlanRelocator relocator,
                                       int numberOfThreads,
                                       long randomSeed,
                                       double fraction) {
        this.population = population;
        this.relocator = relocator;
        this.numberOfThreads = Math.max(1, numberOfThreads);
        this.randomSeed = randomSeed;
        this.random = MatsimRandom.getLocalInstance();
        this.fraction = fraction;

        if (fraction < 0.0 || fraction > 1.0) {
            throw new IllegalArgumentException("Background relocation fraction must be between zero and one");
        }
    }

    public Result update(int iteration, TrafficScoringTracker tracker) {
        List<Candidate> candidates = selectCandidates(tracker);
        List<Proposal> proposals = createProposals(candidates, iteration);
        Result result = applyProposals(proposals, tracker);

        logger.info("Background relocation: candidates={}, proposed={}, freight-accepted={}, cross-border-accepted={}",
                candidates.size(), proposals.size(), result.freightRelocated(), result.crossBorderRelocated());
        return result;
    }

    /** The complete background-agent selection policy lives here. */
    private List<Candidate> selectCandidates(TrafficScoringTracker tracker) {
        return population.getPersons().values().stream()
                        .map(person -> candidate(person, tracker))
                        .filter(Objects::nonNull)
                        .filter(candidate -> candidate.score().isWorthRelocating())
                        .filter(candidate -> random.nextDouble() < fraction)
                        .toList();
    }

    private Candidate candidate(Person person, TrafficScoringTracker tracker) {
        if (Boolean.TRUE.equals(person.getAttributes().getAttribute(Constants.OUTSIDE_AGENT_ATTRIBUTE))) {
            return null;
        }

        TrafficCategory category;
        if (Tools.isFreightPerson(person)) {
            category = TrafficCategory.FREIGHT;
        } else if (Tools.isCrossBorderPerson(person)) {
            category = TrafficCategory.CROSS_BORDER;
        } else {
            return null;
        }
        return new Candidate(person, tracker.getScore(person.getId()), category);
    }

    /** Routing is the expensive phase, so isolated proposals remain parallel. */
    private List<Proposal> createProposals(List<Candidate> candidates, int iteration) {
        List<Proposal> proposals;
        ForkJoinPool pool = new ForkJoinPool(numberOfThreads);
        try {
            proposals = pool.submit(() -> candidates.parallelStream()
                    .map(candidate -> propose(candidate, iteration))
                    .filter(Objects::nonNull)
                    .toList()).join();
        } finally {
            pool.shutdown();
        }
        // sort them from worst score to the best
        return proposals.stream()
                .sorted(Comparator.comparingInt(proposal -> proposal.originalScore().score()))
                .toList();
    }

    /** Commits serially so every decision observes all earlier accepted moves. */
    private Result applyProposals(List<Proposal> proposals, TrafficScoringTracker tracker) {
        int freightRelocated = 0;
        int crossBorderRelocated = 0;
        for (Proposal proposal : proposals) {
            Person person = population.getPersons().get(proposal.personId());
            if (person == null || !tracker.previewUpdate(person, proposal.plan()).improves()) continue;

            Plan previous = person.getSelectedPlan();
            person.addPlan(proposal.plan());
            person.setSelectedPlan(proposal.plan());
            if (previous != null) person.removePlan(previous);
            tracker.update(person);

            if (proposal.category() == TrafficCategory.FREIGHT) freightRelocated++;
            else crossBorderRelocated++;
        }
        return new Result(freightRelocated, crossBorderRelocated);
    }

    private Proposal propose(Candidate candidate, int iteration) {
        Person person = candidate.person();
        long seed = randomSeed ^ ((long) iteration << 32) ^ person.getId().hashCode();
        Plan plan = relocator.propose(person, new Random(seed));
        return plan == null ? null
                : new Proposal(person.getId(), plan, candidate.score(), candidate.category());
    }

    public record Result(int freightRelocated, int crossBorderRelocated) { }
    private record Candidate(Person person, TrafficScore score, TrafficCategory category) { }
    private record Proposal(Id<Person> personId,
                            Plan plan,
                            TrafficScore originalScore,
                            TrafficCategory category) { }
}
