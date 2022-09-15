package org.eqasim.switzerland.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class SwissCostParameters implements ParameterDefinition {
	public double carCost_CHF_km = 0.0;

	public double ptCost_CHF_km = 0.0;
	public double ptMinimumCost_CHF = 0.0;

	public double ptRegionalRadius_km = 0.0;

	public double ptRegionalInVehicleDistance_km = 0.0;

	public static SwissCostParameters buildDefault() {
		SwissCostParameters parameters = new SwissCostParameters();

		parameters.carCost_CHF_km = 0.26;

		parameters.ptCost_CHF_km = 0.5;
		parameters.ptMinimumCost_CHF = 0.0; //g/ not used

		parameters.ptRegionalRadius_km = 0.0; //g/ not used

		parameters.ptRegionalInVehicleDistance_km = 10.0;

		return parameters;
	}
}
