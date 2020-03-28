package org.eqasim.projects.astra16.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;
import org.eqasim.switzerland.ovgk.OVGK;

public class AstraPtVariables extends PtVariables {
	public final double railTravelTime_min;
	public final double busTravelTime_min;
	public final double headway_min;
	public final OVGK ovgk;

	public AstraPtVariables(PtVariables delegate, double railTravelTime_min, double busTravelTime_min,
			double headway_min, OVGK ovgk) {
		super(delegate.inVehicleTime_min, delegate.waitingTime_min, delegate.accessEgressTime_min,
				delegate.numberOfLineSwitches, delegate.cost_MU, delegate.euclideanDistance_km);

		this.busTravelTime_min = busTravelTime_min;
		this.railTravelTime_min = railTravelTime_min;
		this.headway_min = headway_min;
		this.ovgk = ovgk;
	}
}
