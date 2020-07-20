package org.eqasim.jakarta.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.jakarta.mode_choice.parameters.JakartaCostParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class JakartaMotorcycleCostModel extends AbstractCostModel {
	private final JakartaCostParameters costParameters;

	@Inject
	public JakartaMotorcycleCostModel(JakartaCostParameters costParameters) {
		super("motorcycle");

		this.costParameters = costParameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		return costParameters.motorcycleCost_KIDR_km * getInVehicleDistance_km(elements);
	}
}