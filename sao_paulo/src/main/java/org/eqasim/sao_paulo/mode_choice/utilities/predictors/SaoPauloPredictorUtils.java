package org.eqasim.sao_paulo.mode_choice.utilities.predictors;

import org.matsim.api.core.v01.population.Person;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class SaoPauloPredictorUtils {
	static public boolean hasSubscription(Person person) {
		Boolean hasSubscription = (Boolean) person.getAttributes().getAttribute("ptSubscription");
		return hasSubscription != null && hasSubscription;
	}
	
	static public boolean startsEndsinCity(DiscreteModeChoiceTrip trip) {
		boolean startInCity = (boolean) trip.getOriginActivity().getAttributes().getAttribute("city");
		boolean endInCity = (boolean) trip.getDestinationActivity().getAttributes().getAttribute("city");
		return startInCity | endInCity;
	}

	static public double hhlIncome(Person person) {
        double hhlIncome = (double) person.getAttributes().getAttribute("hhlIncome");
		return hhlIncome;
	}
}
