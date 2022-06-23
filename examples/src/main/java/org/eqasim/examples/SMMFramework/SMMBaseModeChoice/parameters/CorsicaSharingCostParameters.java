package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.parameters;

import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;

public class CorsicaSharingCostParameters extends IDFCostParameters {
	public double sharingCost_Eur_Km=0.0;
	public double sharingBooking_Cost=0.0;

	public static CorsicaSharingCostParameters buildDefault() {
		// Copy & paste

		CorsicaSharingCostParameters parameters = new CorsicaSharingCostParameters();

		parameters.carCost_EUR_km = 0.30;
		parameters.sharingBooking_Cost = 0.25;
		parameters.sharingBooking_Cost=1;

		return parameters;
	}
}
