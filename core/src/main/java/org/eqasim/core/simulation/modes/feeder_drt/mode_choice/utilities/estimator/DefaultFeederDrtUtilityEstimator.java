package org.eqasim.core.simulation.modes.feeder_drt.mode_choice.utilities.estimator;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.modes.feeder_drt.router.FeederDrtRoutingModule;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class DefaultFeederDrtUtilityEstimator implements UtilityEstimator {
	private final Map<String, UtilityEstimator> drtEstimators;
	private final Map<String, UtilityEstimator> ptEstimators;

	public DefaultFeederDrtUtilityEstimator(Map<String, UtilityEstimator> ptEstimators, Map<String, UtilityEstimator> drtEstimators) {
		this.drtEstimators = drtEstimators;
		this.ptEstimators = ptEstimators;
	}

	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		// It would probably be cleaner to use TripStructureUtils to cut the elements list to subtrips
		// But then I would have to loop through the individual trips to see what modes they are to then pass them to appropriate estimators
		List<PlanElement> currentTrip = new LinkedList<>();
		double totalUtility = 0;


		String stageActivityType = null;
		UtilityEstimator ptEstimator = null;
		UtilityEstimator drtEstimator = null;

		FeederDrtRoutingModule.FeederDrtTripSegmentType nextSegmentType = FeederDrtRoutingModule.FeederDrtTripSegmentType.MAIN;

		for (PlanElement element : elements) {
			if (element instanceof Activity stageActivity && stageActivity.getType().equals(stageActivityType)) {
				FeederDrtRoutingModule.FeederDrtTripSegmentType previousSegmentType = (FeederDrtRoutingModule.FeederDrtTripSegmentType) stageActivity.getAttributes().getAttribute(FeederDrtRoutingModule.STAGE_ACTIVITY_PREVIOUS_SEGMENT_TYPE_ATTR);
				if(previousSegmentType.equals(FeederDrtRoutingModule.FeederDrtTripSegmentType.MAIN)) {
					totalUtility += ptEstimator.estimateUtility(person, trip, currentTrip);
					nextSegmentType = FeederDrtRoutingModule.FeederDrtTripSegmentType.DRT;
				} else if (previousSegmentType.equals(FeederDrtRoutingModule.FeederDrtTripSegmentType.DRT)) {
					totalUtility += drtEstimator.estimateUtility(person, trip, currentTrip);
					nextSegmentType = FeederDrtRoutingModule.FeederDrtTripSegmentType.MAIN;
				} else {
					throw new IllegalStateException(String.format("Unhandled previous segment type %s in trip of person %s", previousSegmentType, person.getId().toString()));
				}
				currentTrip.clear();
			} else {
				currentTrip.add(element);
				if (element instanceof Leg leg) {
					if(stageActivityType == null) {
						String routingMode = leg.getRoutingMode();
						ptEstimator = this.ptEstimators.get(routingMode);
						drtEstimator = this.drtEstimators.get(routingMode);
						stageActivityType = routingMode + " interaction";
					}
				}
			}
		}
		if(currentTrip.size() > 0) {
			if(ptEstimator == null) {
				throw new IllegalStateException(String.format("Plan of person %s has no legs", person.getId().toString()));
			}
			if (nextSegmentType.equals(FeederDrtRoutingModule.FeederDrtTripSegmentType.MAIN)) {
				totalUtility += ptEstimator.estimateUtility(person, trip, currentTrip);
			} else {
				totalUtility += drtEstimator.estimateUtility(person, trip, currentTrip);
			}
			currentTrip.clear();
		}
		return totalUtility;
	}
}
