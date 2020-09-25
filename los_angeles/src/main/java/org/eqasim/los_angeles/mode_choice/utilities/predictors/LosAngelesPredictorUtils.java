package org.eqasim.los_angeles.mode_choice.utilities.predictors;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class LosAngelesPredictorUtils {
	static public boolean hasSubscription(Person person) {
		Boolean hasSubscription = (Boolean) person.getAttributes().getAttribute("ptSubscription");
		return hasSubscription != null && hasSubscription;
	}
	
	static public boolean startsEndsinCity(DiscreteModeChoiceTrip trip) {
		boolean startInCity = (boolean) trip.getOriginActivity().getAttributes().getAttribute("city");
		boolean endInCity = (boolean) trip.getDestinationActivity().getAttributes().getAttribute("city");
		return startInCity | endInCity;
	}
	
	static public boolean startsEndsinOrangeCounty(DiscreteModeChoiceTrip trip) {
		boolean startInOrange = (boolean) trip.getOriginActivity().getAttributes().getAttribute("orangeCounty");
		boolean endInOrange = (boolean) trip.getDestinationActivity().getAttributes().getAttribute("orangeCounty");
		return startInOrange | endInOrange;
	}

	static public double hhlIncome(Person person) {
        double hhlIncome = (double) person.getAttributes().getAttribute("hhlIncome");
		return hhlIncome;
	}
}
