package org.eqasim.sao_paulo.mode_choice.utilities.predictors;

import org.eqasim.sao_paulo.mode_choice.utilities.variables.SaoPauloPersonVariables;
import org.matsim.api.core.v01.population.Person;

public class SaoPauloPersonPredictor {
	public SaoPauloPersonVariables predictVariables(Person person) {
		boolean hasSubscription = SaoPauloPredictorUtils.hasSubscription(person);

		return new SaoPauloPersonVariables(hasSubscription);
	}
}
