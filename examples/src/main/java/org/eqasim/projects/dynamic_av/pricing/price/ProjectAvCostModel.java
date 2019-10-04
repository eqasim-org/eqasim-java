package org.eqasim.projects.dynamic_av.pricing.price;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class ProjectAvCostModel extends AbstractCostModel {
	private final PriceCalculator listener;

	@Inject
	public ProjectAvCostModel(PriceCalculator listener) {
		super("av");
		this.listener = listener;
	}

	/*
	 * TODO: Actually everything in this function is done over and over again. Would
	 * make sense to calculate this at a central spot at the end of the Mobsim.
	 */
	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double tripDistance_km = getInVehicleDistance_km(elements);
		return listener.getActivePrice_MU_km() * tripDistance_km;
	}
}
