package org.eqasim.san_francisco.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class SanFranciscoCostParameters implements ParameterDefinition {
	public double carCost_USD_mile;
	
	public double ptCostPerTrip_USD_20km;
	public double ptCostPerTrip_USD_40km;
	public double ptCostPerTrip_USD_40plus_km;

	public static SanFranciscoCostParameters buildDefault() {
		SanFranciscoCostParameters parameters = new SanFranciscoCostParameters();

		parameters.carCost_USD_mile = 0.14;
		parameters.ptCostPerTrip_USD_20km = 2.5;
		parameters.ptCostPerTrip_USD_40km = 2.0;
		parameters.ptCostPerTrip_USD_40plus_km = 1.5;

		return parameters;
	}
}
