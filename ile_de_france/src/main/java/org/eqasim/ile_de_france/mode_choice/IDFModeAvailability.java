package org.eqasim.ile_de_france.mode_choice;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPredictorUtils;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;

public class IDFModeAvailability implements ModeAvailability {
	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		Collection<String> modes = new HashSet<>();

		// Modes that are always available
		modes.add(TransportMode.walk);
		modes.add(TransportMode.pt);
		modes.add("car_passenger");

		// Check car availability
		boolean carAvailability = true;

		if (!IDFPredictorUtils.hasDrivingPermit(person)) {
			carAvailability = false;
		}

		if ("none".equals((String) person.getAttributes().getAttribute("carAvailability"))) {
			carAvailability = false;
		}

		if (carAvailability) {
			modes.add(TransportMode.car);
			modes.add("car_pt");
		}

		// Check bike availability
		boolean bikeAvailability = true;

		if ("none".equals((String) person.getAttributes().getAttribute("bikeAvailability"))) {
			bikeAvailability = false;
		}

		if (bikeAvailability) {
			modes.add(TransportMode.bike);
		}

		// Add special mode "outside" if applicable
		Boolean isOutside = (Boolean) person.getAttributes().getAttribute("outside");

		if (isOutside != null && isOutside) {
			modes.add("outside");
		}

		return modes;
	}
}
