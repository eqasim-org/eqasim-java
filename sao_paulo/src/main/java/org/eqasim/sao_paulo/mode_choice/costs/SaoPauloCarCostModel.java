package org.eqasim.sao_paulo.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.sao_paulo.mode_choice.parameters.SaoPauloCostParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class SaoPauloCarCostModel extends AbstractCostModel {
	private final SaoPauloCostParameters costParameters;

	@Inject
	public SaoPauloCarCostModel(SaoPauloCostParameters costParameters) {
		super("car");

		this.costParameters = costParameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		return costParameters.carCost_BRL_km * getInVehicleDistance_km(elements);
	}
}
