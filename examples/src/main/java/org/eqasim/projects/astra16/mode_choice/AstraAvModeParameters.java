package org.eqasim.projects.astra16.mode_choice;

import org.eqasim.automated_vehicles.mode_choice.mode_parameters.AvModeParameters;
import org.eqasim.projects.astra16.mode_choice.AstraModeParameters.AstraBaseModeParameters;

public class AstraAvModeParameters extends AvModeParameters {
	public final AstraBaseModeParameters project = new AstraBaseModeParameters();

	static public AstraAvModeParameters buildFrom6Feb2020() {
		AstraAvModeParameters parameters = new AstraAvModeParameters();

		// Av
		parameters.alpha_u = -0.0608;
		parameters.betaTravelTime_u_min = -0.0150;
		parameters.betaWaitingTime_u_min = -0.0925;
		parameters.betaAccessEgressTime_u_min = -0.0142;

		// Av Project
		parameters.project.betaAgeOver60 = -2.6588;
		parameters.project.betaWork = -1.9377;

		return parameters;
	}
}
