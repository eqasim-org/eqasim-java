package org.eqasim.projects.dynamic_av.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;

public class ProjectPtVariables extends PtVariables {
	public final double railTravelTime_min;
	public final double busTravelTime_min;
	public final double headway_min;

	public ProjectPtVariables(PtVariables delegate, double railTravelTime_min, double busTravelTime_min,
			double headway_min) {
		super(delegate.inVehicleTime_min, delegate.waitingTime_min, delegate.accessEgressTime_min,
				delegate.numberOfLineSwitches, delegate.cost_MU, delegate.euclideanDistance_km);

		this.busTravelTime_min = busTravelTime_min;
		this.railTravelTime_min = railTravelTime_min;
		this.headway_min = headway_min;
	}
}
