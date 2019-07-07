package org.eqasim.ile_de_france.mode_choice.costs;

import org.eqasim.ile_de_france.mode_choice.parameters.CostParameters;

public class CarCostModel {
	private final CostParameters costParameters;

	public CarCostModel(CostParameters costParameters) {
		this.costParameters = costParameters;
	}

	public double calculate_EUR(double distance_km) {
		return costParameters.carCostPerKm_EUR * distance_km;
	}
}
