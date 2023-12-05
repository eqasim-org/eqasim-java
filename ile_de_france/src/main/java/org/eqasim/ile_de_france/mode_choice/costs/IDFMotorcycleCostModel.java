package org.eqasim.ile_de_france.mode_choice.costs;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class IDFMotorcycleCostModel extends AbstractCostModel {
	private final IDFCostParameters costParameters;

	@Inject
	public IDFMotorcycleCostModel(IDFCostParameters costParameters) {
		super("motorcycle");
		this.costParameters = costParameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		return costParameters.motorcycleCost_EUR_km * getInVehicleDistance_km(elements);
	}
}
