package org.eqasim.switzerland.zurich.mode_choice.utilities.variables;

import org.eqasim.switzerland.ch.mode_choice.utilities.variables.SwissPersonVariables;

public class ZurichPersonVariables extends SwissPersonVariables {
	public final double householdIncome_MU;

	public ZurichPersonVariables(SwissPersonVariables delegate, double householdIncome_MU) {
		super(delegate, delegate.homeLocation, delegate.hasGeneralSubscription, delegate.hasHalbtaxSubscription,
				delegate.hasRegionalSubscription, delegate.statedPreferenceRegion);
		this.householdIncome_MU = householdIncome_MU;
	}
}