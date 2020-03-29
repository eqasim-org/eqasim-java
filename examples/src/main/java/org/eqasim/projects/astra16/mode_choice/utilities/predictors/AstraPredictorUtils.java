package org.eqasim.projects.astra16.mode_choice.utilities.predictors;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;

public class AstraPredictorUtils {
	static public double getHouseholdIncome(Person person) {
		return (Double) person.getAttributes().getAttribute("householdIncome");
	}

	static public boolean isAgeOver60(Person person) {
		return (int) (Integer) person.getAttributes().getAttribute("age") >= 60;
	}

	static public boolean hasPurposeWork(Activity activity) {
		return activity.getType().equals("work");
	}

	static public boolean isInsideCity(Activity activity) {
		Boolean isInside = (Boolean) activity.getAttributes().getAttribute("city");
		return isInside != null && isInside;
	}
}
