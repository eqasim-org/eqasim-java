package org.sutlab.seville.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class SevilleCostParameters implements ParameterDefinition {
	public double carCost_EUR_km = 0.0;
	public double busCost_EUR = 0.0;
	public double metroCost_EUR = 0.0;
	public double ptCost_EUR = 0.0;

	public static SevilleCostParameters buildDefault() {
		SevilleCostParameters parameters = new SevilleCostParameters();

		//http://visit-seville.com/seville-metro/ https://visit-seville.com/transport/
		parameters.carCost_EUR_km = 0.2;
		parameters.busCost_EUR = 1.4;
		parameters.metroCost_EUR = 1.35;
		parameters.ptCost_EUR = 1.8; //Renfe tickets

		return parameters;
	}
}