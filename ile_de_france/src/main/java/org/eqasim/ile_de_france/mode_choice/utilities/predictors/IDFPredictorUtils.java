package org.eqasim.ile_de_france.mode_choice.utilities.predictors;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonUtils;

public class IDFPredictorUtils {
	static public boolean hasSubscription(Person person) {
		Boolean hasSubscription = (Boolean) person.getAttributes().getAttribute("hasPtSubscription");
		return hasSubscription != null && hasSubscription;
	}

	static public boolean isUrbanArea(Activity activity) {
		Boolean isUrban = (Boolean) activity.getAttributes().getAttribute("isUrban");
		return isUrban != null && isUrban;
	}

	static public boolean hasLicense(Person person) {
		return PersonUtils.hasLicense(person);
	}

	public static boolean getHouseholdCarAvailability(Person person) {
		if ("none".equals((String) person.getAttributes().getAttribute("carAvailability"))) {
			return false;
		}

		return true;
	}
	
	public static boolean getHouseholdBikeAvailability(Person person) {
		if ("none".equals((String) person.getAttributes().getAttribute("bikeAvailability"))) {
			return false;
		}

		return true;
	}
}
