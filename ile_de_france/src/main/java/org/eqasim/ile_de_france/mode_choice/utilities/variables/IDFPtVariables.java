package org.eqasim.ile_de_france.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;

public class IDFPtVariables extends PtVariables {
	public final double headway_min;
	public final boolean isOnlyBus;
	public final double initialWaitingTime_min;

	public IDFPtVariables(double inVehicleTime_min, double waitingTime_min, double accessEgressTime_min,
			int numberOfLineSwitches, double cost_MU, double euclideanDistance_km, double headway_min,
			boolean isOnlyBus, double initialWaitingTime_min) {
		super(inVehicleTime_min, waitingTime_min, accessEgressTime_min, numberOfLineSwitches, cost_MU,
				euclideanDistance_km);

		this.headway_min = headway_min;
		this.isOnlyBus = isOnlyBus;
		this.initialWaitingTime_min = initialWaitingTime_min;
	}
}
