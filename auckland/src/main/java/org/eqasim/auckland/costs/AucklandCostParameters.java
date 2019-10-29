package org.eqasim.auckland.costs;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class AucklandCostParameters implements ParameterDefinition {
	public double carConsumption_l_100km;
	public double carFuelCost_MU_l;
	public double ptBaseFare_MU;

	static public AucklandCostParameters buildDefault() {
		AucklandCostParameters parameters = new AucklandCostParameters();

		parameters.carConsumption_l_100km = 10.0;
		parameters.carFuelCost_MU_l = 2.32;

		parameters.ptBaseFare_MU = 3.45;

		return parameters;
	}
}
