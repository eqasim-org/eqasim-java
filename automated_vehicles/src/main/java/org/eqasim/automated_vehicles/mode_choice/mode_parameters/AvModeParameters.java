package org.eqasim.automated_vehicles.mode_choice.mode_parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class AvModeParameters implements ParameterDefinition {
	public double alpha_u = 0.0;
	public double betaTravelTime_u_min = 0.0;
	public double betaWaitingTime_u_min = 0.0;
	public double betaAccessEgressTime_u_min = 0.0;

	static public AvModeParameters buildDefault() {
		AvModeParameters parameters = new AvModeParameters();

		parameters.alpha_u = -0.533;
		parameters.betaWaitingTime_u_min = -0.0379;
		parameters.betaTravelTime_u_min = -0.0605;
		parameters.betaAccessEgressTime_u_min = -0.08;

		return parameters;
	}
}
