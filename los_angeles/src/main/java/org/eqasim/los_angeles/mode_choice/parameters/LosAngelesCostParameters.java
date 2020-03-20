package org.eqasim.los_angeles.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class LosAngelesCostParameters implements ParameterDefinition {
	public double carCost_USD_mile;
	
	public double ptCostPerTrip_USD_20km;
	public double ptCostPerTrip_USD_40km;
	public double ptCostPerTrip_USD_40plus_km;

	public static LosAngelesCostParameters buildDefault() {
		LosAngelesCostParameters parameters = new LosAngelesCostParameters();

		parameters.carCost_USD_mile = 0.2;
		parameters.ptCostPerTrip_USD_20km = 1.75;
		parameters.ptCostPerTrip_USD_40km = 1.75;
		parameters.ptCostPerTrip_USD_40plus_km = 1.5;

		return parameters;
	}
}
