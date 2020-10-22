package org.eqasim.switzerland.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.switzerland.mode_choice.parameters.SwissCostParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class SwissCarCostModel extends AbstractCostModel {
	private final SwissCostParameters parameters;

	@Inject
	public SwissCarCostModel(SwissCostParameters costParameters) {
		super("car");
		this.parameters = costParameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		return parameters.carCost_CHF_km * getInVehicleDistance_km(elements);
	}
}
