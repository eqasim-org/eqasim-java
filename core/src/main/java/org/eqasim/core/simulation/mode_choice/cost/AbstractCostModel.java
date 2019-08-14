package org.eqasim.core.simulation.mode_choice.cost;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

public abstract class AbstractCostModel implements CostModel {
	private final String mode;

	protected AbstractCostModel(String mode) {
		this.mode = mode;
	}

	protected double getInVehicleDistance_km(List<? extends PlanElement> elements) {
		double distance_km = 0.0;

		for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				if (leg.getMode().contentEquals(mode)) {
					distance_km += leg.getRoute().getDistance() * 1e-3;
				}
			}
		}

		return distance_km;
	}
}
