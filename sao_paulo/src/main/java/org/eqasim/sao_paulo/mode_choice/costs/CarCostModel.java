package org.eqasim.sao_paulo.mode_choice.costs;

import org.eqasim.sao_paulo.mode_choice.parameters.CostParameters;

public class CarCostModel {
	private final CostParameters costParameters;

	public CarCostModel(CostParameters costParameters) {
		this.costParameters = costParameters;
	}

	public double calculate_BRL(double distance_km) {
		return costParameters.carCostPerKm_BRL * distance_km;
	}
}
