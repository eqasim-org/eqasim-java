package org.eqasim.examples.auckland_av;

import org.eqasim.auckland.costs.AucklandCostParameters;
import org.eqasim.automated_vehicles.mode_choice.cost.AvCostParameters;

public class AucklandAvCostParameters extends AucklandCostParameters {
	public AvCostParameters av = new AvCostParameters();

	static public AucklandAvCostParameters buildDefault() {
		AucklandAvCostParameters parameters = new AucklandAvCostParameters();

		parameters.carConsumption_l_100km = 10.0;
		parameters.carFuelCost_MU_l = 2.32;

		parameters.ptBaseFare_MU = 3.45;

		parameters.av.distanceCost_MU_km = 0.64;
		parameters.av.vehicleCost_MU = 63.55;
		parameters.av.priceFactor = 1.0;

		parameters.av.defaultPrice_MU_km = 0.64;
		parameters.av.alpha = 0.1;

		return parameters;
	}
}
