package org.eqasim.ile_de_france.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class IDFPersonVariables implements BaseVariables {
	public final boolean hasSubscription;

	public IDFPersonVariables(boolean hasSubscription) {
		this.hasSubscription = hasSubscription;
	}
}
