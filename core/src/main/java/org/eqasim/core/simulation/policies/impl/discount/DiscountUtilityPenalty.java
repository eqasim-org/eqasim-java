package org.eqasim.core.simulation.policies.impl.discount;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.policies.PolicyPersonFilter;
import org.eqasim.core.simulation.policies.utility.UtilityPenalty;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

public class DiscountUtilityPenalty implements UtilityPenalty {
	private final CostModel costModel;
	private final ModeParameters modeParameters;
	private final double costFactor;
	private final PolicyPersonFilter personFilter;
	private final String mode;

	public DiscountUtilityPenalty(String mode, CostModel costModel, ModeParameters modeParameters,
			double costFactor,
			PolicyPersonFilter personFilter) {
		this.mode = mode;
		this.costModel = costModel;
		this.modeParameters = modeParameters;
		this.costFactor = costFactor;
		this.personFilter = personFilter;
	}

	@Override
	public double calculatePenalty(String mode, Person person, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips, List<? extends PlanElement> elements) {
		if (mode.equals(this.mode) && personFilter.applies(person.getId())) {
			double initialCost = costModel.calculateCost_MU(person, trip, elements);
			double updatedCost = initialCost * costFactor;
			return -modeParameters.betaCost_u_MU * (updatedCost - initialCost);
		} else {
			return 0.0;
		}
	}
}
