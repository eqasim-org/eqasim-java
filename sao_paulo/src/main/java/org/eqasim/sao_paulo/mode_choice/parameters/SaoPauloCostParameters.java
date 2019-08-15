package org.eqasim.sao_paulo.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class SaoPauloCostParameters implements ParameterDefinition {
	public double carCost_BRL_km;
	
	public double ptCostPerTrip_BRL;

	public static SaoPauloCostParameters buildDefault() {
		SaoPauloCostParameters parameters = new SaoPauloCostParameters();

		parameters.carCost_BRL_km = 0.84;
		parameters.ptCostPerTrip_BRL = 3.8;

		return parameters;
	}
}
