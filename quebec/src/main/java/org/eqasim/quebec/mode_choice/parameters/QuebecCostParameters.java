package org.eqasim.quebec.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class QuebecCostParameters implements ParameterDefinition {

	
	public double ptCostPerTrip_CAD;


	public static QuebecCostParameters buildDefault() {
		QuebecCostParameters parameters = new QuebecCostParameters();

		parameters.ptCostPerTrip_CAD  = 3.5;
		
		return parameters;
	}
}
