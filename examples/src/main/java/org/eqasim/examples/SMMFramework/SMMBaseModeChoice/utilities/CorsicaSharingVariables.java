package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.utilities;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class CorsicaSharingVariables implements BaseVariables {
	final public double travelTime_min;
	final public double euclideanDistance_km;
	//final public double waitingTime_min;
	//final public double accessEgressTime_min;
	final public double accessTime_min ;
	final public  double egressTime_min;
	final public double detour_min;
	final public double cost_MU ;
	final public double parkingTime_min;

	public CorsicaSharingVariables(double travelTime_min, double cost_MU, double euclideanDistance_km,
								   double accessTime_min, double egressTime_min, double detour_min, double parkingTime_min) {
		this.travelTime_min = travelTime_min;
		this.cost_MU = cost_MU;
		this.euclideanDistance_km = euclideanDistance_km;

		//this.accessEgressTime_min = accessEgressTime_min;
		this.accessTime_min = accessTime_min;
		this.egressTime_min = egressTime_min;
		this.detour_min = detour_min;
		this.parkingTime_min = parkingTime_min;
	}
}
