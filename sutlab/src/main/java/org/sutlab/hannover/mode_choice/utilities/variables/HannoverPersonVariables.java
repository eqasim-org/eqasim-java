package org.sutlab.hannover.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class HannoverPersonVariables implements BaseVariables {
	public final boolean hasSubscription;
	public final boolean hasDrivingPermit;
	public final boolean isParisResident;

	public HannoverPersonVariables(boolean hasSubscription, boolean hasDrivingPermit, boolean isParisResident) {
		this.hasSubscription = hasSubscription;
		this.hasDrivingPermit = hasDrivingPermit;
		this.isParisResident = isParisResident;
	}
}