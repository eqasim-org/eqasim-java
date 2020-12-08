package org.eqasim.quebec.mode_choice.utilities.predictors;

import org.matsim.api.core.v01.population.Person;

public class QuebecPredictorUtils {
	static public boolean hasSubscription(Person person) {
		Boolean hasSubscription = (Boolean) person.getAttributes().getAttribute("ptSubscription");
		return hasSubscription != null && hasSubscription;
	}
	
	static public boolean IsPassenger(Person person) {
		Boolean IsPassenger = (Boolean) person.getAttributes().getAttribute("is_passenger");
		return IsPassenger != null && IsPassenger;
	}
	



}
