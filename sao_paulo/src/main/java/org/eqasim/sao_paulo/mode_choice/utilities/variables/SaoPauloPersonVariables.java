package org.eqasim.sao_paulo.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class SaoPauloPersonVariables implements BaseVariables {
	public final boolean hasSubscription;
	public double hhlIncome;
	public boolean cityTrip;

	public SaoPauloPersonVariables(boolean hasSubscription, boolean cityTrip, double hhlIncome) {
		this.hasSubscription = hasSubscription;
		this.cityTrip = cityTrip;
		this.hhlIncome = hhlIncome;
	}
}
