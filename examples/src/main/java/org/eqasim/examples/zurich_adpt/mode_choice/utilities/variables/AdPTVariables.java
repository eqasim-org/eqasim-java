package org.eqasim.examples.zurich_adpt.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class AdPTVariables implements BaseVariables {

	final public double travelTime_min;
	final public double cost_MU;
	final public double euclideanInVehicleDistance_km;
	final public double waitingTime_min;
	final public double accessTime_min;
	final public double egressTime_min;

	public AdPTVariables(double travelTime_min, double cost_MU, double euclideanInVehicleDistance_km,
			double waitingTime_min, double accessTime_min, double egressTime_min) {
		this.travelTime_min = travelTime_min;
		this.cost_MU = cost_MU;
		this.euclideanInVehicleDistance_km = euclideanInVehicleDistance_km;
		this.waitingTime_min = waitingTime_min;
		this.accessTime_min = accessTime_min;
		this.egressTime_min = egressTime_min;
	}
}
