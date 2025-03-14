package org.eqasim.bavaria.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class BavariaPersonVariables implements BaseVariables {
	public final boolean hasSubscription;
	public final boolean hasDrivingPermit;
	public final boolean isParisResident;

	public BavariaPersonVariables(boolean hasSubscription, boolean hasDrivingPermit, boolean isParisResident) {
		this.hasSubscription = hasSubscription;
		this.hasDrivingPermit = hasDrivingPermit;
		this.isParisResident = isParisResident;
	}
}
