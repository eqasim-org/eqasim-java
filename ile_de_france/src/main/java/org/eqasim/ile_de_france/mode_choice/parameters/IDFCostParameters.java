package org.eqasim.ile_de_france.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class IDFCostParameters implements ParameterDefinition {
	public double carCost_EUR_km = 0.0;
	public double ptCost_EUR_h = 0.0;
	
	public static IDFCostParameters buildDefault() {
		IDFCostParameters parameters = new IDFCostParameters();

		// Consumption in liters per 100km times price in 2010
		parameters.carCost_EUR_km = 1e-2 * 7.0 * 1.14;
		parameters.ptCost_EUR_h = Double.NaN;

		return parameters;
	}
}
