package org.eqasim.wayne_county.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class WayneCountyCostParameters implements ParameterDefinition {
	public double carCost_USD_km;
	


	public static WayneCountyCostParameters buildDefault() {
		WayneCountyCostParameters parameters = new WayneCountyCostParameters();

		parameters.carCost_USD_km = 0.068; //11 cents per mile
		return parameters;
	}
}
