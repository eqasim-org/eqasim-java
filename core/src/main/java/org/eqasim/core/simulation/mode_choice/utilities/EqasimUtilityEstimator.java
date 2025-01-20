package org.eqasim.core.simulation.mode_choice.utilities;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eqasim.core.simulation.mode_choice.epsilon.EpsilonProvider;
import org.eqasim.core.simulation.policies.utility.UtilityPenalty;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.components.estimators.AbstractTripRouterEstimator;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;

public class EqasimUtilityEstimator extends AbstractTripRouterEstimator {
	private final Map<String, UtilityEstimator> estimators;
	private final UtilityPenalty utilityPenalty;
	private final EpsilonProvider epsilonProvider;

	public EqasimUtilityEstimator(TripRouter tripRouter, ActivityFacilities facilities,
			Map<String, UtilityEstimator> estimators, TimeInterpretation timeInterpretation,
			Collection<String> preroutedModes, EpsilonProvider epsilonProvider, UtilityPenalty utilityPenalty) {
		super(tripRouter, facilities, timeInterpretation, preroutedModes);
		this.estimators = estimators;
		this.epsilonProvider = epsilonProvider;
		this.utilityPenalty = utilityPenalty;
	}

	@Override
	protected double estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips, List<? extends PlanElement> elements) {
		UtilityEstimator estimator = estimators.get(mode);

		if (estimator == null) {
			throw new IllegalStateException(String.format("No estimator registered for mode '%s'", mode));
		} else {
			double utility = estimator.estimateUtility(person, trip, elements);
			utility += epsilonProvider.getEpsilon(person.getId(), trip.getIndex(), mode);
			utility += utilityPenalty.calculatePenalty(mode, person, trip, elements);
			return utility;
		}
	}
}
