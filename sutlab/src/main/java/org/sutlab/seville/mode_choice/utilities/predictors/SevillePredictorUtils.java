package org.sutlab.seville.mode_choice.utilities.predictors;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonUtils;

public class SevillePredictorUtils {
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

	public static int getCluster(Person person) {
		//this is a specific design. see swiss calibration setup in cmdp for switzerland. Here it is clustered according to cantonal regions. 
		// since we are working at city level we only calibrate to  the whole region
		return 0;
	}

	
}