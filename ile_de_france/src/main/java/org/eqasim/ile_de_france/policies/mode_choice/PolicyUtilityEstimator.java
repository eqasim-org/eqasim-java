package org.eqasim.ile_de_france.policies.mode_choice;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class PolicyUtilityEstimator implements UtilityEstimator {
	private final UtilityPenalty penalty;
	private final UtilityEstimator delegate;
	private final String mode;

	public PolicyUtilityEstimator(UtilityEstimator delegate, UtilityPenalty penalty, String mode) {
		this.delegate = delegate;
		this.penalty = penalty;
		this.mode = mode;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double utility = delegate.estimateUtility(person, trip, elements);
		utility += penalty.calculatePenalty(mode, person, trip, elements);
		return utility;
	}
}
