package org.eqasim.projects.dynamic_av.pricing.price;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class ProjectAvCostModel extends AbstractCostModel {
	private final PriceCalculator calculator;

	@Inject
	public ProjectAvCostModel(PriceCalculator listener) {
		super("av");
		this.calculator = listener;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double tripDistance_km = getInVehicleDistance_km(elements);
		return calculator.calculateTripPrice_MU(tripDistance_km);
	}
}
