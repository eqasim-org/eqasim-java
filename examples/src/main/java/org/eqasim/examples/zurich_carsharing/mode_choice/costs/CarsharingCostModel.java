package org.eqasim.examples.zurich_carsharing.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.examples.zurich_carsharing.mode_choice.parameters.CarsharingCostParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class CarsharingCostModel extends AbstractCostModel {
	private final CarsharingCostParameters parameters;

	@Inject
	public CarsharingCostModel(CarsharingCostParameters costParameters) {
		super("freefloating");
		this.parameters = costParameters;
	}

	public double calculateCost(double travelTime_sec) {
		return parameters.traveltimeCost_MU * travelTime_sec / 60.0;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		return 0;
	}
	
	
}
