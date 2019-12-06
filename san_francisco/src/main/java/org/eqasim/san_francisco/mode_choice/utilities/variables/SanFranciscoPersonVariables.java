package org.eqasim.san_francisco.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class SanFranciscoPersonVariables implements BaseVariables {
	public final boolean hasSubscription;
    public final boolean cityTrip;
    public final double hhlIncome;
	public SanFranciscoPersonVariables(boolean hasSubscription, boolean cityTrip, double hhlIncome) {
		this.hasSubscription = hasSubscription;
		this.cityTrip = cityTrip;
		this.hhlIncome = hhlIncome;
	}
}
