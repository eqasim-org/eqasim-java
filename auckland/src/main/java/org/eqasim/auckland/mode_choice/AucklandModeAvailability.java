package org.eqasim.auckland.mode_choice;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;

public class AucklandModeAvailability implements ModeAvailability {
	static public final String NAME = "AucklandModeAvailability";

	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		return Arrays.asList(TransportMode.car, TransportMode.pt, TransportMode.walk);
	}
}
