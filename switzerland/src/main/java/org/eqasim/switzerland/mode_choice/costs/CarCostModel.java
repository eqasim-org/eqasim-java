package org.eqasim.switzerland.mode_choice.costs;

import org.eqasim.switzerland.mode_choice.parameters.CostParameters;

public class CarCostModel {
	private final CostParameters costParameters;

	public CarCostModel(CostParameters costParameters) {
		this.costParameters = costParameters;
	}

	public double calculate_CHF(double distance_km) {
		return costParameters.carCostPerKm_CHF * distance_km;
	}
}
