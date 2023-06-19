package org.eqasim.ile_de_france.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class IDFParkingVariables implements BaseVariables {
	public final double parkingPressure;
	public final double parkingCost_EUR;

	public IDFParkingVariables(double parkingPressure, double parkingCost_EUR) {
		this.parkingPressure = parkingPressure;
		this.parkingCost_EUR = parkingCost_EUR;
	}
}
