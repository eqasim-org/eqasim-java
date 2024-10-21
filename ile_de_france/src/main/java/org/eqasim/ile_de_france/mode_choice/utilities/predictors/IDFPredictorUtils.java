package org.eqasim.ile_de_france.mode_choice.utilities.predictors;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonUtils;

public class IDFPredictorUtils {
	static public boolean hasSubscription(Person person) {
		Boolean hasSubscription = (Boolean) person.getAttributes().getAttribute("hasPtSubscription");
		return hasSubscription != null && hasSubscription;
	}

	static public boolean isOutside(Person person) {
		Boolean isOutside = (Boolean) person.getAttributes().getAttribute("outside");
		return isOutside != null && isOutside;
	}

	static public boolean hasDrivingLicense(Person person) {
		return !"no".equals(PersonUtils.getLicense(person));
	}

	static public boolean hasCarAvailability(Person person) {
		return !"none".equals((String) person.getAttributes().getAttribute("carAvailability"));
	}

	static public boolean hasBicycleAvailability(Person person) {
		return !"none".equals((String) person.getAttributes().getAttribute("bicycleAvailability"));
	}
	
	static public boolean isParisResident(Person person) {
		Boolean isResident = (Boolean) person.getAttributes().getAttribute("isParis");
		return isResident != null && isResident;
	}
}
