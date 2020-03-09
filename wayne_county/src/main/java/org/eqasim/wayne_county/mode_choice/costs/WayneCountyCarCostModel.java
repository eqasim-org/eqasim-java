package org.eqasim.wayne_county.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.wayne_county.mode_choice.parameters.WayneCountyCostParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class WayneCountyCarCostModel extends AbstractCostModel {
	private final WayneCountyCostParameters parameters;

	@Inject
	public WayneCountyCarCostModel(WayneCountyCostParameters costParameters) {
		super("car");
		this.parameters = costParameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		return parameters.carCost_USD_km * getInVehicleDistance_km(elements);
	}
}
