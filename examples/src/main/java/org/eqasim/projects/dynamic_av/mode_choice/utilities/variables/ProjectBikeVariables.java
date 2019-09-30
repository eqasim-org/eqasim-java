package org.eqasim.projects.dynamic_av.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;

public class ProjectBikeVariables extends BikeVariables {
	final public double euclideanDistance_km;

	public ProjectBikeVariables(BikeVariables delegate, double euclideanDistance_km) {
		super(delegate.travelTime_min);
		this.euclideanDistance_km = euclideanDistance_km;
	}
}
