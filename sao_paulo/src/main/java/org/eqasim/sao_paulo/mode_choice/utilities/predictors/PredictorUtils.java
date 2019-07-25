package org.eqasim.sao_paulo.mode_choice.utilities.predictors;

import org.matsim.api.core.v01.population.Person;

public class PredictorUtils {
	static public int getAge(Person person) {
		Integer age = (Integer) person.getAttributes().getAttribute("age");
		return age == null ? -1 : age;
	}

	static public boolean hasSubscription(Person person) {
		Boolean hasSubscription = (Boolean) person.getAttributes().getAttribute("ptSubscription");
		return hasSubscription != null && hasSubscription;
	}
}
