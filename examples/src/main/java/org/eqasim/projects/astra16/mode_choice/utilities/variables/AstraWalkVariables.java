package org.eqasim.projects.astra16.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;

public class AstraWalkVariables extends WalkVariables {
	final public double euclideanDistance_km;

	public AstraWalkVariables(WalkVariables delegate, double euclideanDistance_km) {
		super(delegate.travelTime_min);
		this.euclideanDistance_km = euclideanDistance_km;
	}
}
