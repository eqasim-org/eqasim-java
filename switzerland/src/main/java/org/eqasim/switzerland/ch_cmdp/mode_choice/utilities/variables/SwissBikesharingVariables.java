package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class SwissBikesharingVariables implements BaseVariables {
	final public double travelTime_min;
    final public double accessegressTime_min;

	public SwissBikesharingVariables(double travelTime_min, double accessegressTime_min) {
		this.travelTime_min = travelTime_min;
        this.accessegressTime_min = accessegressTime_min;
	}
}
