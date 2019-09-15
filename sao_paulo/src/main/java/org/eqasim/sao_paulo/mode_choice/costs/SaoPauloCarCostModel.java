package org.eqasim.sao_paulo.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.costs.AbstractCostModel;
import org.eqasim.sao_paulo.mode_choice.parameters.SaoPauloCostParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class SaoPauloCarCostModel extends AbstractCostModel {
	private final SaoPauloCostParameters costParameters;

	@Inject
	public SaoPauloCarCostModel(SaoPauloCostParameters costParameters) {
		super("car");

		this.costParameters = costParameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double inVehicleDistance_km = getInVehicleDistance_km(elements);
		return costParameters.carCostPerKm_BRL * inVehicleDistance_km;
	}
}
