package org.eqasim.ile_de_france.mode_choice.utilities.predictors;

import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPersonVariables;
import org.matsim.api.core.v01.population.Person;

public class IDFPersonPredictor {
	public IDFPersonVariables predictVariables(Person person) {
		boolean hasSubscription = IDFPredictorUtils.hasSubscription(person);
		return new IDFPersonVariables(hasSubscription);
	}
}
