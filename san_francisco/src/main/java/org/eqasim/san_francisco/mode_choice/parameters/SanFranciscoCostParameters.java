package org.eqasim.san_francisco.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class SanFranciscoCostParameters implements ParameterDefinition {
	public double carCost_USD_mile;
	
	public double ptCostPerTrip_USD_10km;

	public static SanFranciscoCostParameters buildDefault() {
		SanFranciscoCostParameters parameters = new SanFranciscoCostParameters();

		parameters.carCost_USD_mile = 0.13;
		parameters.ptCostPerTrip_USD_10km = 2.5;

		return parameters;
	}
}
