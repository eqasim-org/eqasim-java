package org.eqasim.examples.zurich_carsharing.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class CarsharingCostParameters implements ParameterDefinition {
	public double traveltimeCost_MU;


	public static CarsharingCostParameters buildDefault() {
		CarsharingCostParameters parameters = new CarsharingCostParameters();

		parameters.traveltimeCost_MU = 0.4;
		
		return parameters;
	}
}
