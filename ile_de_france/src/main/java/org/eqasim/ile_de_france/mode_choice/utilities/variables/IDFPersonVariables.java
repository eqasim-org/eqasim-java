package org.eqasim.ile_de_france.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class IDFPersonVariables implements BaseVariables {
	public final boolean hasSubscription;
	public final boolean hasDrivingPermit;
	public final double householdIncomePerCU_EUR;
	public final String residenceMunicipalityId;

	public IDFPersonVariables(boolean hasSubscription, boolean hasDrivingPermit, double householdIncomePerCU_EUR,
			String residenceMunicipalityId) {
		this.hasSubscription = hasSubscription;
		this.hasDrivingPermit = hasDrivingPermit;
		this.householdIncomePerCU_EUR = householdIncomePerCU_EUR;
		this.residenceMunicipalityId = residenceMunicipalityId;
	}
}
