package org.eqasim.projects.dynamic_av.pricing.price;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class ProjectCostParameters implements ParameterDefinition {
	public double defaultPrice_MU_km;
	public int horizon;
	public int transientIterations;
	
	public double baseFare_CHF = 0.0;
	public double distanceFare_CHF_km = Double.NaN;
	public double minimumDistanceFare_CHF_km = 0.0;

	public static ProjectCostParameters buildDefault() {
		ProjectCostParameters parameters = new ProjectCostParameters();

		parameters.defaultPrice_MU_km = 0.4;
		parameters.transientIterations = 10;
		parameters.horizon = 10;

		return parameters;
	}
}
