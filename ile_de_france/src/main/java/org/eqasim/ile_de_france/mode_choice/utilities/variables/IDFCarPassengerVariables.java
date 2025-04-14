package org.eqasim.ile_de_france.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class IDFCarPassengerVariables implements BaseVariables {
	final public double travelTime_min;
	final public double euclideanDistance_km;
	final public double accessEgressTime_min;

	public IDFCarPassengerVariables(double travelTime_min, double euclideanDistance_km, double accessEgressTime_min) {
		this.travelTime_min = travelTime_min;
		this.euclideanDistance_km = euclideanDistance_km;
		this.accessEgressTime_min = accessEgressTime_min;
	}
}
