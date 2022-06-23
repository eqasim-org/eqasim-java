package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.cost;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.parameters.CorsicaDrtCostParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class DrtCostModel extends AbstractCostModel {
	private final CorsicaDrtCostParameters parameters;

	public DrtCostModel(CorsicaDrtCostParameters parameters) {
		super("drt");
		this.parameters = parameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double tripDistance_km = getInVehicleDistance_km(elements);
		return parameters.drtCost_EUR_km * tripDistance_km;
	}
}
