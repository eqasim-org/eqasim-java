package org.eqasim.switzerland.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.costs.AbstractCostModel;
import org.eqasim.switzerland.mode_choice.parameters.SwissCostParameters;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class SwissCarCostModel extends AbstractCostModel {
	private final SwissCostParameters costParameters;

	@Inject
	public SwissCarCostModel(SwissCostParameters costParameters) {
		super(TransportMode.car);
		this.costParameters = costParameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double inVehicleDistance_km = getInVehicleDistance_km(elements);
		return costParameters.carCost_CHF_km * inVehicleDistance_km;
	}
}
