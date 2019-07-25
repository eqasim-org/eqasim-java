package org.eqasim.sao_paulo.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class CostParameters implements ParameterDefinition {
	public double carCostPerKm_BRL = 0.0;

	public static CostParameters buildDefault() {
		CostParameters parameters = new CostParameters();

		parameters.carCostPerKm_BRL = 0.84;

		return parameters;
	}
}
