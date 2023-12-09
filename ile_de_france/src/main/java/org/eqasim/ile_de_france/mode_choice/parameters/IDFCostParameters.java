package org.eqasim.ile_de_france.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class IDFCostParameters implements ParameterDefinition {
	public static final double DEFAULT_CAR_COST_KM = 0.15;
	public static final double DEFAULT_DRT_COST_EUR_KM = 0.3;
	public double carCost_EUR_km = 0.0;
	public double drtCost_EUR_km;

	public static IDFCostParameters buildDefault() {
		IDFCostParameters parameters = new IDFCostParameters();

		parameters.carCost_EUR_km = DEFAULT_CAR_COST_KM;
		parameters.drtCost_EUR_km = DEFAULT_DRT_COST_EUR_KM;

		return parameters;
	}
}
