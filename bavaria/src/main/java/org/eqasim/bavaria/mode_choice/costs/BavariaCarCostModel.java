package org.eqasim.bavaria.mode_choice.costs;

import java.util.List;

import org.eqasim.bavaria.mode_choice.parameters.BavariaCostParameters;
import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class BavariaCarCostModel extends AbstractCostModel {
	private final BavariaCostParameters costParameters;

	@Inject
	public BavariaCarCostModel(BavariaCostParameters costParameters) {
		super("car");

		this.costParameters = costParameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double parkingCost_EUR = calculateParkingCost_EUR(person, trip, elements);
		return costParameters.carCost_EUR_km * getInVehicleDistance_km(elements) + parkingCost_EUR;
	}

	private double calculateParkingCost_EUR(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		// TODO: May implement a parking cost model here.
		return 0.0;
	}
}
