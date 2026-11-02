package org.sutlab.seville_extended.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class SevilleCarVariables implements BaseVariables {
	public final double travelTime_min;
	public final double accessEgressTime_min;
	public final double cost_MU;
	public final double routeDistance_km;
	public final double speed_kmh;

	public SevilleCarVariables(double travelTime_min, double accessEgressTime_min, double cost_MU,
			double routeDistance_km, double speed_kmh) {
		this.travelTime_min = travelTime_min;
		this.accessEgressTime_min = accessEgressTime_min;
		this.cost_MU = cost_MU;
		this.routeDistance_km = routeDistance_km;
		this.speed_kmh = speed_kmh;
	}
}
