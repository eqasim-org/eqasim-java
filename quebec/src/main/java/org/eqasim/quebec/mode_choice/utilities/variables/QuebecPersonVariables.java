package org.eqasim.quebec.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class QuebecPersonVariables implements BaseVariables {
	public final boolean hasSubscription;

	public QuebecPersonVariables(boolean hasSubscription) {
		this.hasSubscription = hasSubscription;
	
	}
}
