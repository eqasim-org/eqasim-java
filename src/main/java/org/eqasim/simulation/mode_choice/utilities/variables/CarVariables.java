package org.eqasim.simulation.mode_choice.utilities.variables;

public class CarVariables {
	final public double travelTime_min;
	final public double cost_CHF;
	final public double crowflyDistance_km;
	final public double accessEgressTime_min;

	public CarVariables(double travelTime_min, double cost_CHF, double crowflyDistance_km,
			double accessEgressTime_min) {
		this.travelTime_min = travelTime_min;
		this.cost_CHF = cost_CHF;
		this.crowflyDistance_km = crowflyDistance_km;
		this.accessEgressTime_min = accessEgressTime_min;
	}
}
