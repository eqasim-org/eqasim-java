package org.eqasim.projects.dynamic_av.mode_choice.utilities.variables;

import org.eqasim.switzerland.mode_choice.utilities.variables.SwissPersonVariables;

public class ProjectPersonVariables extends SwissPersonVariables {
	public final double householdIncome_MU;

	public ProjectPersonVariables(SwissPersonVariables delegate, double householdIncome_MU) {
		super(delegate, delegate.homeLocation, delegate.hasGeneralSubscription, delegate.hasHalbtaxSubscription,
				delegate.hasRegionalSubscription, delegate.statedPreferenceRegion);
		this.householdIncome_MU = householdIncome_MU;
	}
}
