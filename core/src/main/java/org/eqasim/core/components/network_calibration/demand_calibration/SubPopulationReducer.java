package org.eqasim.core.components.network_calibration.demand_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.network_calibration.Processors.CountsProcessor;
import org.eqasim.core.components.network_calibration.Processors.FlowProcessor;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.routes.NetworkRoute;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class SubPopulationReducer implements IterationEndsListener {

	private static final Logger logger = LogManager.getLogger(SubPopulationReducer.class);

	private static final double FLOW_OVER_ESTIMATION_THRESHOLD = 0.30;
	private static final double SUBPOPULATION_SHARE_THRESHOLD = 0.40;

	private final Population population;
	private final TripListConverter tripListConverter;
	private final CountsProcessor countsProcessor;
	private final FlowProcessor flowProcessor;
	private final double sampleSize;
	private final boolean activate;
	private final Random random;

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

	public void reduceTrips() {
		LinkTripStats stats = collectLinkTripStats();
		Map<TripKey, Double> tripRemovalProbabilities = identifyTripRemovalProbabilities(stats);

		if (tripRemovalProbabilities.isEmpty()) {
			return;
		}

		int modifiedTrips = applyReduction(tripRemovalProbabilities);
		logger.info("SubPopulationReducer: reduced {} subpopulation car trips on overestimated links ({} candidates).",
				modifiedTrips, tripRemovalProbabilities.size());
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		int iteration = event.getIteration();
        if (!activate || iteration <= 0) {
			return;
		}
        if (iteration==30||iteration==50) {
            reduceTrips();
        }
	}

	private LinkTripStats collectLinkTripStats() {
		Map<Id<Link>, Integer> allTripTraversals = new HashMap<>();
		Map<Id<Link>, Integer> subpopulationTripTraversals = new HashMap<>();
		Map<Id<Link>, Set<TripKey>> subpopulationTripsPerLink = new HashMap<>();

		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			List<DiscreteModeChoiceTrip> trips = tripListConverter.convert(plan);
			boolean inSubPopulation = Tools.isInSubPopulation(person);

			for (int tripIndex = 0; tripIndex < trips.size(); tripIndex++) {
				DiscreteModeChoiceTrip trip = trips.get(tripIndex);
				if (!TransportMode.car.equals(trip.getInitialMode()) &&
                    !TransportMode.truck.equals(trip.getInitialMode())) {
					continue;
				}

				TripKey tripKey = new TripKey(person.getId().toString(), tripIndex);
				for (Id<Link> linkId : getRouteLinkIds(trip)) {
					allTripTraversals.put(linkId, allTripTraversals.getOrDefault(linkId, 0) + 1);

					if (inSubPopulation) {
						subpopulationTripTraversals.put(linkId, subpopulationTripTraversals.getOrDefault(linkId, 0) + 1);
						subpopulationTripsPerLink.computeIfAbsent(linkId, k -> new HashSet<>()).add(tripKey);
					}
				}
			}
		}

		return new LinkTripStats(allTripTraversals, subpopulationTripTraversals, subpopulationTripsPerLink);
	}

	private Map<TripKey, Double> identifyTripRemovalProbabilities(LinkTripStats stats) {
		Map<TripKey, Double> result = new HashMap<>();

		for (Map.Entry<Id<Link>, Integer> entry : stats.allTripTraversals.entrySet()) {
			Id<Link> linkId = entry.getKey();
			int totalTraversals = entry.getValue();
			if (totalTraversals <= 0) {
				continue;
			}

			float counts = countsProcessor.getLinkCounts(linkId);
			if (counts <= 0.0) {
				continue;
			}

			double simulatedFlow = flowProcessor.getTotalLinkFlow(linkId) / Math.max(sampleSize, 1.0e-9);
			double overEstimation = (simulatedFlow - counts) / Math.max(counts, 1.0e-9);
			if (overEstimation <= FLOW_OVER_ESTIMATION_THRESHOLD) {
				continue;
			}

			int subTraversals = stats.subpopulationTripTraversals.getOrDefault(linkId, 0);
			double subShare = subTraversals / (double) totalTraversals;
			if (subShare <= SUBPOPULATION_SHARE_THRESHOLD) {
				continue;
			}

			double removalShareNeeded = (overEstimation - FLOW_OVER_ESTIMATION_THRESHOLD) / (1.0 + overEstimation);
			// We only remove from the selected subpopulation, so scale by its link share.
			double perTripRemovalProbability = clamp(removalShareNeeded / Math.max(subShare, 1.0e-9), 0.0, 1.0);
			if (perTripRemovalProbability <= 0.0) {
				continue;
			}

			for (TripKey tripKey : stats.subpopulationTripsPerLink.getOrDefault(linkId, java.util.Collections.emptySet())) {
				// Combine probabilities across multiple problematic links for one trip.
				double previous = result.getOrDefault(tripKey, 0.0);
				double combined = 1.0 - (1.0 - previous) * (1.0 - perTripRemovalProbability);
				result.put(tripKey, combined);
			}
		}

		return result;
	}

	private int applyReduction(Map<TripKey, Double> tripRemovalProbabilities) {
		int modifiedTrips = 0;

		for (Person person : population.getPersons().values()) {
			if (!Tools.isInSubPopulation(person)) {
				continue;
			}

			List<DiscreteModeChoiceTrip> trips = tripListConverter.convert(person.getSelectedPlan());
			for (int tripIndex = 0; tripIndex < trips.size(); tripIndex++) {
				TripKey key = new TripKey(person.getId().toString(), tripIndex);
				double probability = tripRemovalProbabilities.getOrDefault(key, 0.0);
				if (probability <= 0.0 || random.nextDouble() >= probability) {
					continue;
				}

				DiscreteModeChoiceTrip trip = trips.get(tripIndex);
				if (!TransportMode.car.equals(trip.getInitialMode()) &&
                    !TransportMode.truck.equals(trip.getInitialMode())) {
					continue;
				}

				boolean changed = false;
				for (PlanElement element : trip.getInitialElements()) {
					if (element instanceof Leg leg) {
						leg.setMode(TransportMode.walk);
						leg.setRoute(null);
						changed = true;
					}
				}

				if (changed) {
					modifiedTrips++;
				}
			}
		}

		return modifiedTrips;
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

	private record LinkTripStats(Map<Id<Link>, Integer> allTripTraversals,
								 Map<Id<Link>, Integer> subpopulationTripTraversals,
								 Map<Id<Link>, Set<TripKey>> subpopulationTripsPerLink) {
	}

	private record TripKey(String personId, int tripIndex) {
	}
}
