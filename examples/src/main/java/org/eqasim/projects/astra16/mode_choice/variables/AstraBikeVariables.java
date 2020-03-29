package org.eqasim.projects.astra16.mode_choice.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;

public class AstraBikeVariables extends BikeVariables {
	final public double euclideanDistance_km;

	public AstraBikeVariables(BikeVariables delegate, double euclideanDistance_km) {
		super(delegate.travelTime_min);
		this.euclideanDistance_km = euclideanDistance_km;
	}
}
