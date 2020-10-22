package org.eqasim.ile_de_france.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class IDFCarCostModel extends AbstractCostModel {
	private final IDFCostParameters costParameters;

	@Inject
	public IDFCarCostModel(IDFCostParameters costParameters) {
		super("car");
		this.costParameters = costParameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		return costParameters.carCost_EUR_km * getInVehicleDistance_km(elements);
	}
}
