package org.eqasim.switzerland.mode_choice.utilities.estimators;

import org.eqasim.switzerland.mode_choice.parameters.ModeChoiceParameters;
import org.eqasim.switzerland.mode_choice.utilities.variables.BikeVariables;
import org.eqasim.switzerland.mode_choice.utilities.variables.PersonVariables;

public class BikeEstimator {
	private final ModeChoiceParameters parameters;

	public BikeEstimator(ModeChoiceParameters parameters) {
		this.parameters = parameters;
	}

	public double estimateUtility(PersonVariables personVariables, BikeVariables variables) {
		double utility = 0.0;

		utility += parameters.bike.alpha;
		utility += parameters.bike.betaTravelTime * variables.travelTime_min;
		utility += parameters.bike.betaAgeOver18 * Math.max(0.0, personVariables.age_a - 18);

		if (personVariables.statedPreferenceRegion == 3) {
			utility += parameters.bike.betaStatedPreferenceRegion3;
		}

		return utility;
	}
}
