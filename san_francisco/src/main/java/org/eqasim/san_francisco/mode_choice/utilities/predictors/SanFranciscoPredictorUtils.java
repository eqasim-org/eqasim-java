package org.eqasim.san_francisco.mode_choice.utilities.predictors;

import org.matsim.api.core.v01.population.Person;

public class SanFranciscoPredictorUtils {
	static public boolean hasSubscription(Person person) {
		Boolean hasSubscription = (Boolean) person.getAttributes().getAttribute("ptSubscription");
		return hasSubscription != null && hasSubscription;
	}
}
