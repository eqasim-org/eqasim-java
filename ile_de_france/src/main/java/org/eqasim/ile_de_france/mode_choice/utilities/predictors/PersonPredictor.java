package org.eqasim.ile_de_france.mode_choice.utilities.predictors;

import org.eqasim.ile_de_france.mode_choice.utilities.variables.PersonVariables;
import org.matsim.api.core.v01.population.Person;

public class PersonPredictor {
	public PersonVariables predict(Person person) {
		int age_a = PredictorUtils.getAge(person);
		boolean hasSubscription = PredictorUtils.hasSubscription(person);

		return new PersonVariables(hasSubscription, age_a);
	}
}
