package org.eqasim.los_angeles.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class LosAngelesPersonVariables implements BaseVariables {
	public final boolean hasSubscription;
    public final boolean cityTrip;
    public final double hhlIncome;
	public LosAngelesPersonVariables(boolean hasSubscription, boolean cityTrip, double hhlIncome) {
		this.hasSubscription = hasSubscription;
		this.cityTrip = cityTrip;
		this.hhlIncome = hhlIncome;
	}
}
