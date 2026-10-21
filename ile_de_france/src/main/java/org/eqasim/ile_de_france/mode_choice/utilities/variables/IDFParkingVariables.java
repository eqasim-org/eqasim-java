package org.eqasim.ile_de_france.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class IDFParkingVariables implements BaseVariables {
	public final double parkingPressure;

	public final double originParkingTariff_EUR_h;
	public final double destinationParkingTariff_EUR_h;

	public IDFParkingVariables(double parkingPressure, double originParkingTariff_EUR_h,
			double destinationParkingTariff_EUR_h) {
		this.parkingPressure = parkingPressure;
		this.originParkingTariff_EUR_h = originParkingTariff_EUR_h;
		this.destinationParkingTariff_EUR_h = destinationParkingTariff_EUR_h;
	}
}
