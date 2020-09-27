package org.eqasim.core.simulation.mode_choice.utilities;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.components.estimators.AbstractTripRouterEstimator;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.contribs.discrete_mode_choice.replanning.time_interpreter.TimeInterpreter;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;

public class ModalUtilityEstimator extends AbstractTripRouterEstimator {
	private final Map<String, UtilityEstimator> estimators;

	public ModalUtilityEstimator(TripRouter tripRouter, ActivityFacilities facilities,
			Map<String, UtilityEstimator> estimators, TimeInterpreter.Factory timeInterpreterFactory,
			Collection<String> preroutedModes) {
		super(tripRouter, facilities, timeInterpreterFactory, preroutedModes);
		this.estimators = estimators;
	}

	@Override
	protected double estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips, List<? extends PlanElement> elements) {
		UtilityEstimator estimator = estimators.get(mode);

		if (estimator == null) {
			throw new IllegalStateException(String.format("No estimator registered for mode '%s'", mode));
		} else {
			return estimator.estimateUtility(person, trip, elements);
		}
	}
}
