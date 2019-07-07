package org.eqasim.ile_de_france.mode_choice.costs;

import org.eqasim.ile_de_france.mode_choice.utilities.variables.PersonVariables;

public class PtCostModel {
	public double calculate_EUR(PersonVariables personVariables, double crowflyDistance_km) {
		if (personVariables.hasSubscription) {
			return 0.0;
		}

		if (crowflyDistance_km < 3.0) {
			return 3.0;
		} else if (crowflyDistance_km < 5.0) {
			return 3.0;
		} else if (crowflyDistance_km < 10.0) {
			return 3.5;
		} else {
			return (int) Math.ceil(crowflyDistance_km / 5.0) * 2.0;
		}
	}
}
