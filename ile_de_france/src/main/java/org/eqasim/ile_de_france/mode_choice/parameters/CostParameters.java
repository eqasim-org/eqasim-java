package org.eqasim.ile_de_france.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class CostParameters implements ParameterDefinition {
	public double carCostPerKm_EUR = 0.0;

	public static CostParameters buildDefault() {
		CostParameters parameters = new CostParameters();

		parameters.carCostPerKm_EUR = 0.2;

		return parameters;
	}
}
