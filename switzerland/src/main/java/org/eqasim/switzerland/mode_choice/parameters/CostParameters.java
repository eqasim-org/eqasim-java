package org.eqasim.switzerland.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class CostParameters implements ParameterDefinition {
	public double carCostPerKm_CHF = 0.0;

	public double ptCostPerKm_CHF = 0.0;
	public double ptCostMinimum_CHF = 0.0;

	public double ptRegionalRadius_km = 0.0;

	public static CostParameters buildDefault() {
		CostParameters parameters = new CostParameters();

		parameters.carCostPerKm_CHF = 0.26;

		parameters.ptCostPerKm_CHF = 0.6;
		parameters.ptCostMinimum_CHF = 2.7;

		parameters.ptRegionalRadius_km = 15.0;

		return parameters;
	}
}
