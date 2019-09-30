package org.eqasim.projects.dynamic_av.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;

public class ProjectWalkVariables extends WalkVariables {
	final public double euclideanDistance_km;

	public ProjectWalkVariables(WalkVariables delegate, double euclideanDistance_km) {
		super(delegate.travelTime_min);
		this.euclideanDistance_km = euclideanDistance_km;
	}
}
