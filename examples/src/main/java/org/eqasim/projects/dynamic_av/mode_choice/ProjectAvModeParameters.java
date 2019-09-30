package org.eqasim.projects.dynamic_av.mode_choice;

import org.eqasim.automated_vehicles.mode_choice.mode_parameters.AvModeParameters;
import org.eqasim.projects.dynamic_av.mode_choice.ProjectModeParameters.ProjectBaseModeParameters;

public class ProjectAvModeParameters extends AvModeParameters {
	public final ProjectBaseModeParameters project = new ProjectBaseModeParameters();
	
	static public ProjectAvModeParameters buildDefault() {
		ProjectAvModeParameters parameters = new ProjectAvModeParameters();

		// Av
		parameters.alpha_u = -0.415;
		parameters.betaTravelTime_u_min = -0.0277;
		parameters.betaWaitingTime_u_min = -0.0769;
		parameters.betaAccessEgressTime_u_min = -0.0751;
		
		// Av Project
		parameters.project.betaAgeOver60 = 0.574;
		parameters.project.betaWork = -0.785;

		return parameters;
	}
}
