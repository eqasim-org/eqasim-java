package org.eqasim.examples.corsica_carpooling.mode_choice.utilities;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class CarpoolingVariables implements BaseVariables {
	final public double travelTime_min;
	final public double cost_MU;
	final public double euclideanDistance_km;

	public CarpoolingVariables(double travelTime_min, double cost_MU, double euclideanDistance_km) {
		this.travelTime_min = travelTime_min;
		this.cost_MU = cost_MU;
		this.euclideanDistance_km = euclideanDistance_km;
	}
}
