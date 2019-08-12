package org.eqasim.core.simulation.mode_choice.utilities.predictors;

import org.eqasim.core.simulation.mode_choice.utilities.variables.PersonVariables;
import org.matsim.api.core.v01.population.Person;

public class PersonPredictor {
	public PersonVariables predictVariables(Person person) {
		int age_a = PredictorUtils.getAge(person);
		return new PersonVariables(age_a);
	}
}
