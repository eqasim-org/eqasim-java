package org.eqasim.ile_de_france.policies.transit_discount;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.policies.PolicyPersonFilter;
import org.eqasim.ile_de_france.policies.mode_choice.UtilityPenalty;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class TransitDiscountUtilityPenalty implements UtilityPenalty {
	private final CostModel costModel;
	private final IDFModeParameters modeParameters;
	private final double costFactor;
	private final PolicyPersonFilter personFilter;

	public TransitDiscountUtilityPenalty(CostModel costModel, IDFModeParameters modeParameters, double costFactor,
			PolicyPersonFilter personFilter) {
		this.costModel = costModel;
		this.modeParameters = modeParameters;
		this.costFactor = costFactor;
		this.personFilter = personFilter;
	}

	@Override
	public double calculatePenalty(String mode, Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		if (mode.equals(TransportMode.pt) && personFilter.applies(person.getId())) {
			double initialCost = costModel.calculateCost_MU(person, trip, elements);
			double updatedCost = initialCost * costFactor;
			return modeParameters.betaCost_u_MU * (updatedCost - initialCost);
		} else {
			return 0.0;
		}
	}
}
