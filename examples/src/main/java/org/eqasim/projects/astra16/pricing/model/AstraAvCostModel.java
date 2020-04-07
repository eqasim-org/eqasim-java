package org.eqasim.projects.astra16.pricing.model;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class AstraAvCostModel extends AbstractCostModel {
	static public final String NAME = "AstraAvCostModel";
	// private final PriceInterpolator interpolator;

	public AstraAvCostModel() { // PriceInterpolator interpolator) {
		super("av");
		// this.interpolator = interpolator;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		// return interpolator.getPricePerTrip_CHF()
		// + interpolator.getPricePerKm_CHF() * getInVehicleDistance_km(elements);

		return 0.5 * getInVehicleDistance_km(elements);
	}
}
