package org.eqasim.sao_paulo.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class SaoPauloPersonVariables implements BaseVariables {
	public final boolean hasSubscription;

	public SaoPauloPersonVariables(boolean hasSubscription) {
		this.hasSubscription = hasSubscription;
	}
}
