package org.eqasim.examples.zurich_carsharing.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class CarsharingModeParameters implements ParameterDefinition {
	public double alpha_u = 0.0;
	public double betaTravelTime_u_min = 0.0;
	public double betaAccessTime_u_min = 0.0;

	static public CarsharingModeParameters buildDefault() {
		CarsharingModeParameters parameters = new CarsharingModeParameters();

		parameters.alpha_u = -3.0;
		parameters.betaAccessTime_u_min = -0.08;
		parameters.betaTravelTime_u_min = -0.067;

		return parameters;
	}
}
