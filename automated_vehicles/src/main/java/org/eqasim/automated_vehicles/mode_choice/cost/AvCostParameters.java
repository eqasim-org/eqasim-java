package org.eqasim.automated_vehicles.mode_choice.cost;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class AvCostParameters implements ParameterDefinition {
	public double baseFare_MU;
	public double profitMargin;

	public double defaultPrice_MU_km;
	public double alpha = 0.0;

	public static AvCostParameters buildDefault() {
		AvCostParameters parameters = new AvCostParameters();

		parameters.baseFare_MU = 0.0;
		parameters.profitMargin = 0.0;

		parameters.defaultPrice_MU_km = 0.4;
		parameters.alpha = 0.1;

		return parameters;
	}
}
