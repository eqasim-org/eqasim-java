package org.eqasim.examples.zurich_adpt.mode_choice.mode_parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class AdPTModeParameters implements ParameterDefinition {
	public double alpha_u = 0.0;
	public double betaTravelTime_u_min = 0.0;
	public double betaAccessEgressTime_min = 0.0;
	public double betaWaitingTime_u_min = 0.0;
	public double betaWork_u = 0.0;
	public double betaAgeOver60_u = 0.0;

	static public AdPTModeParameters buildDefault() {
		AdPTModeParameters parameters = new AdPTModeParameters();

		parameters.alpha_u = 0.0;
		parameters.betaWaitingTime_u_min = -0.0124;
		parameters.betaAccessEgressTime_min = -0.0142;
		parameters.betaTravelTime_u_min = -0.015;

		parameters.betaWork_u = -1.9377;
		parameters.betaAgeOver60_u = -2.6588;

		return parameters;
	}
}
