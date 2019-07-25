package org.eqasim.sao_paulo.mode_choice.utilities.variables;

public class PtVariables {
	public final double inVehicleTime_min;
	public final double waitingTime_min;
	public final double accessEgressTime_min;
	public final int numberOfLineSwitches;
	public final double cost_BRL;
	public final double crowflyDistance_km;

	public PtVariables(double inVehicleTime_min, double waitingTime_min, double accessEgressTime_min,
			int numberOfLineSwitches, double cost_BRL, double crowflyDistance_km) {
		this.inVehicleTime_min = inVehicleTime_min;
		this.waitingTime_min = waitingTime_min;
		this.accessEgressTime_min = accessEgressTime_min;
		this.numberOfLineSwitches = numberOfLineSwitches;
		this.cost_BRL = cost_BRL;
		this.crowflyDistance_km = crowflyDistance_km;
	}
}
