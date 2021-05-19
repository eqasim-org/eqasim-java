package org.eqasim.examples.corsica_drt.mode_choice.parameters;

import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;

public class CorsicaDrtCostParameters extends IDFCostParameters {
	public double drtCost_EUR_km;

	public static CorsicaDrtCostParameters buildDefault() {
		// Copy & paste

		CorsicaDrtCostParameters parameters = new CorsicaDrtCostParameters();

		parameters.carCost_EUR_km = 0.15;
		parameters.drtCost_EUR_km = 0.3;

		return parameters;
	}
}
