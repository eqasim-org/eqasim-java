package org.eqasim.san_francisco.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.san_francisco.mode_choice.parameters.SanFranciscoCostParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class SanFranciscoCarCostModel extends AbstractCostModel {
	private final SanFranciscoCostParameters costParameters;

	@Inject
	public SanFranciscoCarCostModel(SanFranciscoCostParameters costParameters) {
		super("car");

		this.costParameters = costParameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double inVehicleDistance_kfeet = getInVehicleDistance_km(elements);
		return costParameters.carCost_USD_mile * inVehicleDistance_kfeet / 5.28;
	}
}
