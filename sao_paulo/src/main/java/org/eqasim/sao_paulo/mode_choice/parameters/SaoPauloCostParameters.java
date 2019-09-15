package org.eqasim.sao_paulo.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class SaoPauloCostParameters implements ParameterDefinition {
	public double carCostPerKm_BRL = 0.0;

	public static SaoPauloCostParameters buildDefault() {
		SaoPauloCostParameters parameters = new SaoPauloCostParameters();

		parameters.carCostPerKm_BRL = 0.84;

		return parameters;
	}
}
