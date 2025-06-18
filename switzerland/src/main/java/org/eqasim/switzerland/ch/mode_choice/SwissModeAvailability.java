package org.eqasim.switzerland.ch.mode_choice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.core.population.PersonUtils;

public class SwissModeAvailability implements ModeAvailability {
	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		Boolean isFreight = (Boolean) person.getAttributes().getAttribute("isFreight");

		if (isFreight != null && isFreight) {
			return Collections.singleton("truck");
		}

		Collection<String> modes = new HashSet<>();

		// Modes that are always available
		modes.add(TransportMode.walk);
		modes.add(TransportMode.pt);

		// Check car availability
		boolean carAvailability = true;

		if (PersonUtils.getLicense(person).equals("no")) {
			carAvailability = false;
		}

		if (PersonUtils.getCarAvail(person).equals("never")) {
			carAvailability = false;
		}

		if (carAvailability) {
			modes.add(TransportMode.car);
		}

		// Check bike availability
		boolean bikeAvailability = true;

		if (person.getAttributes().getAttribute("bikeAvailability").equals("FOR_NONE")) {
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

		// Add special mode "car_passenger" if applicable
		Boolean isCarPassenger = (Boolean) person.getAttributes().getAttribute("isCarPassenger");

		if (isCarPassenger != null && isCarPassenger) {
			modes.add("car_passenger");
		}

		// Add special modes "*_loop" if applicable
		List<String> LOOP_MODES      = new ArrayList<>(Arrays.asList("walk_loop", "pt_loop", "bike_loop", "car_loop", "car_passenger_loop"));
		List<String> LOOP_ATTRIBUTES = new ArrayList<>(Arrays.asList("hasWalkLoopTrip", "hasPtLoopTrip", "hasBikeLoopTrip", "hasCarLoopTrip", "hasCarPassengerLoopTrip"));

		for (int i = 0; i < LOOP_MODES.size(); i++){
			String mode = LOOP_MODES.get(i);
			String attribute = LOOP_ATTRIBUTES.get(i);

			Boolean hasLoopTrip = (Boolean) person.getAttributes().getAttribute(attribute);
			if (hasLoopTrip != null && hasLoopTrip) {
				modes.add(mode);
			}
		}

		return modes;
	}
}