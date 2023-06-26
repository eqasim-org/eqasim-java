package org.eqasim.examples.corsica_carpooling.mode_choice.parameters;

import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;

public class CarpoolingCostParameters extends IDFCostParameters {
	public double carpoolingCost_EUR_km;

	public static CarpoolingCostParameters buildDefault() {
		// Copy & paste

		CarpoolingCostParameters parameters = new CarpoolingCostParameters();

		parameters.carCost_EUR_km = 0.15;
		parameters.carpoolingCost_EUR_km = 0.3;

		return parameters;
	}
}
