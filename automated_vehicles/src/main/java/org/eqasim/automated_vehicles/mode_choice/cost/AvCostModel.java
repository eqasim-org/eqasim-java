package org.eqasim.automated_vehicles.mode_choice.cost;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class AvCostModel extends AbstractCostModel {
	private final AvCostListener listener;

	public AvCostModel(AvCostListener listener) {
		super("av");
		this.listener = listener;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double tripDistance_km = getInVehicleDistance_km(elements);
		return listener.getActivePrice_MU_km() * tripDistance_km;
	}
}
