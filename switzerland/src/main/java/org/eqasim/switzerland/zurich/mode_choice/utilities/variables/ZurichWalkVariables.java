package org.eqasim.switzerland.zurich.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;

public class ZurichWalkVariables extends WalkVariables {
	final public double euclideanDistance_km;

	public ZurichWalkVariables(WalkVariables delegate, double euclideanDistance_km) {
		super(delegate.travelTime_min);
		this.euclideanDistance_km = euclideanDistance_km;
	}
}