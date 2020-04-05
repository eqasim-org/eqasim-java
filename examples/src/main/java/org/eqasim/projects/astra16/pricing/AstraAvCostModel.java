package org.eqasim.projects.astra16.pricing;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class AstraAvCostModel extends AbstractCostModel {
	static public final String NAME = "AstraAvCostModel";

	public AstraAvCostModel() {
		super("av");
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double tripDistance_km = getInVehicleDistance_km(elements);
		return tripDistance_km * 0.5;
	}
}
