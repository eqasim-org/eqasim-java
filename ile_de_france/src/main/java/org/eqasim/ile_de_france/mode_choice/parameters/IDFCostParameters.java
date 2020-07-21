package org.eqasim.ile_de_france.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class IDFCostParameters implements ParameterDefinition {
	public double carCost_EUR_km = 0.0;

	public static IDFCostParameters buildDefault() {
		IDFCostParameters parameters = new IDFCostParameters();

		parameters.carCost_EUR_km = 0.15;

		return parameters;
	}
}
