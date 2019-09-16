package org.eqasim.automated_vehicles.mode_choice.cost;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class AvCostParameters implements ParameterDefinition {
	public double distanceCost_MU_km;
	public double vehicleCost_MU;
	public double priceFactor;

	public static AvCostParameters buildDefault() {
		AvCostParameters parameters = new AvCostParameters();

		parameters.distanceCost_MU_km = -0.4;
		parameters.vehicleCost_MU = 0.0;
		parameters.priceFactor = 1.0;

		return parameters;
	}
}
