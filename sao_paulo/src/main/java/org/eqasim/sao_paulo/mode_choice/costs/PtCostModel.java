package org.eqasim.sao_paulo.mode_choice.costs;

import org.eqasim.sao_paulo.mode_choice.utilities.variables.PersonVariables;

public class PtCostModel {
	public double calculate_BRL(PersonVariables personVariables, double crowflyDistance_km) {
		if (personVariables.hasSubscription) {
			return 0.0;
		}

		return 1.0;
	}
}
