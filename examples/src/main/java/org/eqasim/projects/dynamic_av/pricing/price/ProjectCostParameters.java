package org.eqasim.projects.dynamic_av.pricing.price;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class ProjectCostParameters implements ParameterDefinition {
	public double distanceCost_MU_km;
	public double vehicleCost_MU;
	public double priceFactor;

	public double defaultPrice_MU_km;
	public double alpha = 0.0;

	public static ProjectCostParameters buildDefault() {
		ProjectCostParameters parameters = new ProjectCostParameters();

		parameters.distanceCost_MU_km = 0.4;
		parameters.vehicleCost_MU = 40.0;
		parameters.priceFactor = 1.0;

		parameters.defaultPrice_MU_km = 0.4;
		parameters.alpha = 0.1;

		return parameters;
	}
}
