package org.eqasim.los_angeles.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class LosAngelesPersonVariables implements BaseVariables {
	public final boolean hasSubscription;
    public final boolean cityTrip;
    public final boolean orangeTrip;
    public final double hhlIncome;
	public LosAngelesPersonVariables(boolean hasSubscription, boolean cityTrip, boolean orangeTrip, double hhlIncome) {
		this.hasSubscription = hasSubscription;
		this.cityTrip = cityTrip;
		this.orangeTrip = orangeTrip;
		this.hhlIncome = hhlIncome;
	}
}
