package org.eqasim.ile_de_france.mode_choice.utilities.predictors;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;

public class IDFPredictorUtils {
	static public boolean hasSubscription(Person person) {
		Boolean hasSubscription = (Boolean) person.getAttributes().getAttribute("hasPtSubscription");
		return hasSubscription != null && hasSubscription;
	}

	static public boolean isUrbanArea(Activity activity) {
		Boolean isUrban = (Boolean) activity.getAttributes().getAttribute("isUrban");
		return isUrban != null && isUrban;
	}
}
