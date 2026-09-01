package org.eqasim.switzerland.ch_cmdp.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class SwissBikesharingCostParameters implements ParameterDefinition {
	public double CHF_km = 0.0;

	public double CHF_min = 0.0;
	public double CHF_base = 0.0;
	public double CHF_minimum = 0.0;


	public static SwissBikesharingCostParameters buildDefault() {
		SwissBikesharingCostParameters parameters = new SwissBikesharingCostParameters();
		// bikesharing
		parameters.CHF_km = 0.26; //per kilometer
		parameters.CHF_min = 0.0; // per minute
		parameters.CHF_base = 1.0; // base fare
		parameters.CHF_minimum = 1.5; // minimum fare

		return parameters;
	}
}
