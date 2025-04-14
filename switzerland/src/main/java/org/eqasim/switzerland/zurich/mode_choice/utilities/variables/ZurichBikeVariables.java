package org.eqasim.switzerland.zurich.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;

public class ZurichBikeVariables extends BikeVariables {
	final public double euclideanDistance_km;

	public ZurichBikeVariables(BikeVariables delegate, double euclideanDistance_km) {
		super(delegate.travelTime_min);
		this.euclideanDistance_km = euclideanDistance_km;
	}
}