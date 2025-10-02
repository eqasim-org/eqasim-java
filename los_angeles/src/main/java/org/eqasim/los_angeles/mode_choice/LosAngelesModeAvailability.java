package org.eqasim.los_angeles.mode_choice;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.core.population.PersonUtils;

public class LosAngelesModeAvailability implements ModeAvailability {
	private final Set<String> additionalModes;

	public LosAngelesModeAvailability(Set<String> additionalModes) {
		this.additionalModes = additionalModes;
	}

	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		Collection<String> modes = new HashSet<>();

		// Modes that are always available
		modes.add(TransportMode.walk);
		modes.add(TransportMode.pt);
		//modes.add(TransportMode.bike);

		// Check car availability
				boolean carAvailability = true;

				if (PersonUtils.getLicense(person).equals("no")) {
					carAvailability = false;
				}

				if ("none".equals((String) person.getAttributes().getAttribute("carAvailability"))) {
					carAvailability = false;
				}

				if (carAvailability) {
					modes.add(TransportMode.car);
				}

		// Add special mode "outside" if applicable
		Boolean isOutside = (Boolean) person.getAttributes().getAttribute("outside");

		if (isOutside != null && isOutside) {
			modes.add("outside");
		}

		// Add special mode "car_passenger" if applicable
		Boolean isCarPassenger = (Boolean) person.getAttributes().getAttribute("isPassenger");

		if (isCarPassenger != null && isCarPassenger) {
			modes.add("car_passenger");
		}

		// Add additional modes
		modes.addAll(additionalModes);

		return modes;
	}
}
