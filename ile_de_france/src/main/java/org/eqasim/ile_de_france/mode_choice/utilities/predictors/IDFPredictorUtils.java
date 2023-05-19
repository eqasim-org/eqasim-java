package org.eqasim.ile_de_france.mode_choice.utilities.predictors;

import java.util.Objects;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonUtils;

public class IDFPredictorUtils {
	static public boolean hasSubscription(Person person) {
		Boolean hasSubscription = (Boolean) person.getAttributes().getAttribute("hasPtSubscription");
		return hasSubscription != null && hasSubscription;
	}

	/*-static public boolean hasDrivingPermit(Person person) {
		Boolean hasDrivingPermit = (Boolean) person.getAttributes().getAttribute("hasDrivingPermit");
		return hasDrivingPermit != null && hasDrivingPermit;
	}*/

	static public boolean hasDrivingPermit(Person person) {
		String hasLicense = PersonUtils.getLicense(person);
		return hasLicense != null && hasLicense.equals("yes");
	}

	static public boolean isUrbanArea(Activity activity) {
		Boolean isUrban = (Boolean) activity.getAttributes().getAttribute("isUrban");
		return isUrban != null && isUrban;
	}

	static public final double MAXIMUM_HEADWAY_MIN = 240.0;
	static public final String HEADWAY_MIN_ATTRIBUTE = "headway_min";

	static public double getHeadway_min(Activity activity) {
		return 0.0; // Not used anymore because of calculation effort
		//return Math.min(MAXIMUM_HEADWAY_MIN,
		//		Objects.requireNonNull((Double) activity.getAttributes().getAttribute(HEADWAY_MIN_ATTRIBUTE)));
	}
}
