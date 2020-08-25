package org.eqasim.ile_de_france.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class IDFPersonVariables implements BaseVariables {
	public final boolean hasSubscription;
	public final boolean hasLicense;
	public final boolean householdCarAvailability;

	public IDFPersonVariables(boolean hasSubscription, boolean hasLicense, boolean householdCarAvailability) {
		this.hasSubscription = hasSubscription;
		this.hasLicense = hasLicense;
		this.householdCarAvailability = householdCarAvailability;
	}
}
