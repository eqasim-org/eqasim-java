package org.eqasim.core.components.network_calibration.demand_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.network_calibration.Processors.CountsProcessor;
import org.eqasim.core.components.network_calibration.Processors.FlowProcessor;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.routes.NetworkRoute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SubPopulationReducer implements IterationEndsListener {

	private static final Logger logger = LogManager.getLogger(SubPopulationReducer.class);

	// Adjustable calibration knobs.
	public static double FLOW_OVER_ESTIMATION_THRESHOLD = 0.20; // if we have an overestimation above this, I do correct
	public static double SUBPOPULATION_SHARE_THRESHOLD = 0.20; // if the share of subpopulation is above this, I correct
	public static double FLOW_UNDER_ESTIMATION_THRESHOLD = 0.20; // if we have an underestimation of that percentage, and we can bring back people, we bring them back
	public static int MIN_TRAVERSALS_PER_LINK = 20; //minimum number of traversals to consider that link
	public static double MAX_LINK_REMOVAL_PROBABILITY = 0.8; // cap the probability to remove people from that link
	public static double MAX_UNDER_PENALTY_PER_LINK = 0.8; // to bring back people
	public static double UNDER_PENALTY_WEIGHT = 0.9;
	public static double MIN_PERSON_REMOVAL_PROBABILITY = 1.0e-3;
	public static double MAX_PERSON_REMOVAL_PROBABILITY = 0.90;
	public static double RESTORE_OVER_PENALTY_WEIGHT = 0.7;
	public static double MIN_PERSON_RESTORE_PROBABILITY = 1.0e-3;
	public static double MAX_PERSON_RESTORE_PROBABILITY = 0.60;
	public static double PLAN_RESTORE_SCORE_THRESHOLD = 0.02;

	public static int WARM_UP_ITERATIONS = 30;
	public static int ITERATION_INTERVAL = 5;


	private final Population population;
	private final TripListConverter tripListConverter;
	private final CountsProcessor countsProcessor;
	private final FlowProcessor flowProcessor;
	private final double sampleSize;
	private final boolean activate;
	private final Random random;
	private final Map<Id<Person>, StoredPersonPlan> removedPersonPlans = new HashMap<>();

	public SubPopulationReducer(Population population, TripListConverter tripListConverter,
								CountsProcessor countsProcessor, FlowProcessor flowProcessor,
								double sampleSize, boolean activate) {
		this.population = population;
		this.tripListConverter = tripListConverter;
		this.countsProcessor = countsProcessor;
		this.flowProcessor = flowProcessor;
		this.sampleSize = sampleSize;
		this.activate = activate;
		this.random = MatsimRandom.getLocalInstance();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		int iteration = event.getIteration();
		if (activate && iteration >= WARM_UP_ITERATIONS && iteration%ITERATION_INTERVAL == 0) {
			reduceTrips();
		}
	}

	public void reduceTrips() {
		LinkTripStats stats = collectLinkTripStats();
		LinkDiagnostics diagnostics = buildLinkDiagnostics(stats);
		int restoredPersons = restorePersons(diagnostics);

		Map<Id<Person>, Double> personRemovalProbabilities = identifyPersonRemovalProbabilities(stats, diagnostics);

		if (personRemovalProbabilities.isEmpty()) {
			logger.info("SubPopulationReducer: restored {} persons; no eligible overestimated links for subpopulation demand reduction.", restoredPersons);
			return;
		}

		int modifiedPersons = applyReduction(personRemovalProbabilities);
		logger.info("SubPopulationReducer: restored {} persons; removed {} subpopulation persons ({} removal candidates, {} currently removed).",
				restoredPersons, modifiedPersons, personRemovalProbabilities.size(), removedPersonPlans.size());
	}

	private LinkTripStats collectLinkTripStats() {
		Map<Id<Link>, Integer> allTripTraversals = new HashMap<>();
		Map<Id<Link>, Integer> subpopulationTripTraversals = new HashMap<>();
		Map<Id<Person>, Map<Id<Link>, Integer>> subpopulationPersonTraversals = new HashMap<>();

		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			if (plan == null) {
				continue;
			}

			List<DiscreteModeChoiceTrip> trips = tripListConverter.convert(plan);
			boolean inSubPopulation = Tools.isInSubPopulation(person);

            for (DiscreteModeChoiceTrip trip : trips) {
                if (!TransportMode.car.equals(trip.getInitialMode()) &&
					!TransportMode.truck.equals(trip.getInitialMode())) {
                    continue;
                }

                for (Id<Link> linkId : getRouteLinkIds(trip)) {
                    if (countsProcessor.contains(linkId)) {
                        allTripTraversals.put(linkId, allTripTraversals.getOrDefault(linkId, 0) + 1);

                        if (inSubPopulation) {
                            subpopulationTripTraversals.put(linkId, subpopulationTripTraversals.getOrDefault(linkId, 0) + 1);
                            subpopulationPersonTraversals
                                    .computeIfAbsent(person.getId(), k -> new HashMap<>())
                                    .put(linkId, subpopulationPersonTraversals
                                            .computeIfAbsent(person.getId(), k -> new HashMap<>())
                                            .getOrDefault(linkId, 0) + 1);
                        }
                    }
                }
            }
		}

		return new LinkTripStats(allTripTraversals, subpopulationTripTraversals, subpopulationPersonTraversals);
	}

	private Map<Id<Person>, Double> identifyPersonRemovalProbabilities(LinkTripStats stats, LinkDiagnostics diagnostics) {

		Map<Id<Person>, Double> result = new HashMap<>();
		for (Map.Entry<Id<Person>, Map<Id<Link>, Integer>> personEntry : stats.subpopulationPersonTraversals.entrySet()) {
			Id<Person> personId = personEntry.getKey();
			if (removedPersonPlans.containsKey(personId)) {
				continue;
			}

			Map<Id<Link>, Integer> traversals = personEntry.getValue();

			double overProbability = 0.0;
			double underScore = 0.0;

			for (Map.Entry<Id<Link>, Integer> traversalEntry : traversals.entrySet()) {
				Id<Link> linkId = traversalEntry.getKey();
				int multiplicity = traversalEntry.getValue();

				LinkSignal signal = diagnostics.overSignals.get(linkId);
				if (signal != null) {
					double repeatedTraversalProbability = 1.0 - Math.pow(1.0 - signal.removalProbability, Math.max(1, multiplicity));
					overProbability = 1.0 - (1.0 - overProbability) * (1.0 - repeatedTraversalProbability);
				}

				double underPenalty = diagnostics.underSignals.getOrDefault(linkId, 0.0);
				if (underPenalty > 0.0) {
					underScore += underPenalty * Math.sqrt(Math.max(1, multiplicity));
				}
			}

			if (overProbability <= 0.0) {
				continue;
			}

			double penaltyFactor = Math.exp(-UNDER_PENALTY_WEIGHT * underScore);
			double finalProbability = clamp(overProbability * penaltyFactor,
					MIN_PERSON_REMOVAL_PROBABILITY, MAX_PERSON_REMOVAL_PROBABILITY);

			if (finalProbability > MIN_PERSON_REMOVAL_PROBABILITY) {
				result.put(personId, finalProbability);
			}
		}

		logger.info("SubPopulationReducer: {} overestimated links eligible, {} underestimated links penalizing removals.",
				diagnostics.overSignals.size(), diagnostics.underSignals.size());

		return result;
	}

	private LinkDiagnostics buildLinkDiagnostics(LinkTripStats stats) {
		Map<Id<Link>, LinkSignal> overSignals = new HashMap<>();
		Map<Id<Link>, Double> underSignals = new HashMap<>();

		for (Map.Entry<Id<Link>, Integer> entry : stats.allTripTraversals.entrySet()) {
			Id<Link> linkId = entry.getKey();
			int totalTraversals = entry.getValue();
			if (totalTraversals < MIN_TRAVERSALS_PER_LINK) {
				continue;
			}

			float counts = countsProcessor.getLinkCounts(linkId);
			if (counts <= 0.0) {
				continue;
			}

			double simulatedFlow = flowProcessor.getTotalLinkFlow(linkId) / Math.max(sampleSize, 1.0e-9);
			double relativeError = (simulatedFlow - counts) / Math.max(counts, 1.0e-9);

			int subTraversals = stats.subpopulationTripTraversals.getOrDefault(linkId, 0);
			double subShare = subTraversals / (double) totalTraversals;

			if (relativeError > FLOW_OVER_ESTIMATION_THRESHOLD && subShare > SUBPOPULATION_SHARE_THRESHOLD) {
				double removalShareNeeded = (relativeError - FLOW_OVER_ESTIMATION_THRESHOLD) / (1.0 + relativeError);
				double pLink = clamp(removalShareNeeded / Math.max(subShare, 1.0e-9), 0.0, MAX_LINK_REMOVAL_PROBABILITY);
				if (pLink > 0.0) {
					overSignals.put(linkId, new LinkSignal(pLink, relativeError, subShare));
				}
			}

			if (relativeError < -FLOW_UNDER_ESTIMATION_THRESHOLD) {
				double underMagnitude = -relativeError;
				double underPenalty = (underMagnitude - FLOW_UNDER_ESTIMATION_THRESHOLD) / (1.0 + underMagnitude);
				underPenalty = clamp(underPenalty, 0.0, MAX_UNDER_PENALTY_PER_LINK);
				if (underPenalty > 0.0) {
					underSignals.put(linkId, underPenalty);
				}
			}
		}

		return new LinkDiagnostics(overSignals, underSignals);
	}

	private int restorePersons(LinkDiagnostics diagnostics) {
		int restored = 0;
		for (Map.Entry<Id<Person>, StoredPersonPlan> entry : new HashMap<>(removedPersonPlans).entrySet()) {
			Id<Person> personId = entry.getKey();
			Person person = population.getPersons().get(personId);
			if (person == null) {
				removedPersonPlans.remove(personId);
				continue;
			}

			StoredPersonPlan stored = entry.getValue();
			double score = estimatePlanScore(stored.originalPlan, diagnostics);
			double probability = planRestoreProbability(score);
			stored.lastRestoreScore = score;

			if (probability > MIN_PERSON_RESTORE_PROBABILITY && random.nextDouble() < probability) {
				if (restorePersonPlan(person, stored.originalPlan)) {
					removedPersonPlans.remove(personId);
					restored++;
				}
			}
		}

		return restored;
	}

	// Positive score means this plan helps underestimated links more than it hurts overestimated ones.
	public double estimatePlanScore(Plan plan, LinkDiagnostics diagnostics) {
		Map<Id<Link>, Integer> traversals = collectPlanTraversals(plan);
		if (traversals.isEmpty()) {
			return Double.NEGATIVE_INFINITY;
		}

		double underSupport = 0.0;
		double overRisk = 0.0;

		for (Map.Entry<Id<Link>, Integer> entry : traversals.entrySet()) {
			Id<Link> linkId = entry.getKey();
			int multiplicity = entry.getValue();

			double underSignal = diagnostics.underSignals.getOrDefault(linkId, 0.0);
			if (underSignal > 0.0) {
				underSupport += underSignal * Math.sqrt(Math.max(1, multiplicity));
			}

			LinkSignal overSignal = diagnostics.overSignals.get(linkId);
			if (overSignal != null) {
				overRisk += overSignal.removalProbability * Math.sqrt(Math.max(1, multiplicity));
			}
		}

		return underSupport - RESTORE_OVER_PENALTY_WEIGHT * overRisk;
	}

	private double planRestoreProbability(double score) {
		if (!Double.isFinite(score) || score <= PLAN_RESTORE_SCORE_THRESHOLD) {
			return 0.0;
		}

		double scaled = (score - PLAN_RESTORE_SCORE_THRESHOLD) / (1.0 + Math.abs(score));
		return clamp(scaled, MIN_PERSON_RESTORE_PROBABILITY, MAX_PERSON_RESTORE_PROBABILITY);
	}

	private int applyReduction(Map<Id<Person>, Double> personRemovalProbabilities) {
		int modifiedPersons = 0;

		for (Person person : population.getPersons().values()) {
			if (!Tools.isInSubPopulation(person)) {
				continue;
			}
			if (removedPersonPlans.containsKey(person.getId())) {
				continue;
			}

			double probability = personRemovalProbabilities.getOrDefault(person.getId(), 0.0);
			if (probability <= MIN_PERSON_REMOVAL_PROBABILITY || random.nextDouble() >= probability) {
				continue;
			}

			if (removeThatPersonActivityChain(person)) {
				modifiedPersons++;
			}
		}

		return modifiedPersons;
	}

	private boolean removeThatPersonActivityChain(Person person) {
		Plan oldPlan = person.getSelectedPlan();
		if (oldPlan == null) {
			return false;
		}

		List<PlanElement> elements = oldPlan.getPlanElements();

		// Create a new plan
		Plan newPlan = population.getFactory().createPlan();

		for (PlanElement element : elements) {
			if (element instanceof Activity) {
				newPlan.addActivity((Activity) element);
				break;
			}
		}

		if (newPlan.getPlanElements().isEmpty()) {
			return false;
		}

		person.removePlan(oldPlan);
		person.addPlan(newPlan);
		person.setSelectedPlan(newPlan);
		removedPersonPlans.put(person.getId(), new StoredPersonPlan(oldPlan));
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

	private double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	private List<Id<Link>> getRouteLinkIds(DiscreteModeChoiceTrip trip) {
		List<Id<Link>> routeLinkIds = new java.util.ArrayList<>();

		for (PlanElement element : trip.getInitialElements()) {
			if (element instanceof Leg leg && leg.getRoute() instanceof NetworkRoute route) {
				routeLinkIds.addAll(route.getLinkIds());
			}
		}

		return routeLinkIds;
	}

	private Map<Id<Link>, Integer> collectPlanTraversals(Plan plan) {
		Map<Id<Link>, Integer> traversals = new HashMap<>();
		if (plan == null) {
			return traversals;
		}

		List<DiscreteModeChoiceTrip> trips = tripListConverter.convert(plan);
		for (DiscreteModeChoiceTrip trip : trips) {
			if (!TransportMode.car.equals(trip.getInitialMode()) && !TransportMode.truck.equals(trip.getInitialMode())) {
				continue;
			}

			for (Id<Link> linkId : getRouteLinkIds(trip)) {
				traversals.put(linkId, traversals.getOrDefault(linkId, 0) + 1);
			}
		}

		return traversals;
	}

	private record LinkTripStats(Map<Id<Link>, Integer> allTripTraversals,
								 Map<Id<Link>, Integer> subpopulationTripTraversals,
								 Map<Id<Person>, Map<Id<Link>, Integer>> subpopulationPersonTraversals) {
	}

	public record LinkDiagnostics(Map<Id<Link>, LinkSignal> overSignals,
								  Map<Id<Link>, Double> underSignals) {
	}

	private record LinkSignal(double removalProbability, double relativeError, double subpopulationShare) {
	}

	private static final class StoredPersonPlan {
		private final Plan originalPlan;
		private double lastRestoreScore;

		private StoredPersonPlan(Plan originalPlan) {
			this.originalPlan = originalPlan;
			this.lastRestoreScore = 0.0;
		}
	}
}
