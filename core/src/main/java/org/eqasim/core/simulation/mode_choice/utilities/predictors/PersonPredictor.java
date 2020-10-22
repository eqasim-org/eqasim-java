package org.eqasim.core.simulation.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.variables.PersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class PersonPredictor extends CachedVariablePredictor<PersonVariables> {
	@Override
	public PersonVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		int age_a = PredictorUtils.getAge(person);

		return new PersonVariables(age_a);
	}
}
