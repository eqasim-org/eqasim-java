package org.eqasim.simulation.mode_choice.utilities.predictors;

import org.eqasim.simulation.mode_choice.utilities.variables.PersonVariables;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;

public class PersonPredictor {
	public PersonVariables predict(Person person) {
		Coord homeLocation = PredictorUtils.getHomeLocation(person);

		boolean hasGeneralSubscription = PredictorUtils.hasGeneralSubscription(person);
		boolean hasHalbtaxSubscription = PredictorUtils.hasHalbtaxSubscription(person);
		boolean hasRegionalSubscription = PredictorUtils.hasRegionalSubscription(person);

		int age_a = PredictorUtils.getAge(person);
		int statedPreferenceRegion = PredictorUtils.getStatedPreferenceRegion(person);

		return new PersonVariables(homeLocation, hasGeneralSubscription, hasHalbtaxSubscription,
				hasRegionalSubscription, age_a, statedPreferenceRegion);
	}
}
