package org.eqasim.core.simulation.mode_choice.epsilon;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class EpsilonAdapter implements UtilityEstimator {
	private final UtilityEstimator delegate;
	private final EpsilonProvider epsilonProvider;
	private final String mode;

	public EpsilonAdapter(String mode, UtilityEstimator delegate, EpsilonProvider epsilonProvider) {
		this.delegate = delegate;
		this.mode = mode;
		this.epsilonProvider = epsilonProvider;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double utility = delegate.estimateUtility(person, trip, elements);
		utility += epsilonProvider.getEpsilon(person.getId(), trip.getIndex(), mode);
		return utility;
	}
}
