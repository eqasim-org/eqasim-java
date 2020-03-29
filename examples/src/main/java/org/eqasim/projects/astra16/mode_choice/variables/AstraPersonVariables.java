package org.eqasim.projects.astra16.mode_choice.variables;

import org.eqasim.switzerland.mode_choice.utilities.variables.SwissPersonVariables;

public class AstraPersonVariables extends SwissPersonVariables {
	public final double householdIncome_MU;

	public AstraPersonVariables(SwissPersonVariables delegate, double householdIncome_MU) {
		super(delegate, delegate.homeLocation, delegate.hasGeneralSubscription, delegate.hasHalbtaxSubscription,
				delegate.hasRegionalSubscription, delegate.statedPreferenceRegion);
		this.householdIncome_MU = householdIncome_MU;
	}
}
