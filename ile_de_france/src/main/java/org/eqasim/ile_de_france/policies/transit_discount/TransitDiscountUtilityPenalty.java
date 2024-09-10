package org.eqasim.ile_de_france.policies.transit_discount;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.policies.mode_choice.UtilityPenalty;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class TransitDiscountUtilityPenalty implements UtilityPenalty {
	private final CostModel costModel;
	private final IDFModeParameters modeParameters;
	private final double costFactor;

	public TransitDiscountUtilityPenalty(CostModel costModel, IDFModeParameters modeParameters, double costFactor) {
		this.costModel = costModel;
		this.modeParameters = modeParameters;
		this.costFactor = costFactor;
	}

	@Override
	public double calculatePenalty(String mode, Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		if (mode.equals(TransportMode.pt)) {
			double initialCost = costModel.calculateCost_MU(person, trip, elements);
			double updatedCost = initialCost * costFactor;
			return modeParameters.betaCost_u_MU * (updatedCost - initialCost);
		} else {
			return 0.0;
		}
	}
}
