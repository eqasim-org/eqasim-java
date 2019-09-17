package org.eqasim.core.simulation.mode_choice.utilities.variables;

public class PtVariables implements BaseVariables {
	public final double inVehicleTime_min;
	public final double waitingTime_min;
	public final double accessEgressTime_min;
	public final int numberOfLineSwitches;
	public final double cost_MU;
	public final double euclideanDistance_km;

	public PtVariables(double inVehicleTime_min, double waitingTime_min, double accessEgressTime_min,
			int numberOfLineSwitches, double cost_MU, double euclideanDistance_km) {
		this.inVehicleTime_min = inVehicleTime_min;
		this.waitingTime_min = waitingTime_min;
		this.accessEgressTime_min = accessEgressTime_min;
		this.numberOfLineSwitches = numberOfLineSwitches;
		this.cost_MU = cost_MU;
		this.euclideanDistance_km = euclideanDistance_km;
	}
}
