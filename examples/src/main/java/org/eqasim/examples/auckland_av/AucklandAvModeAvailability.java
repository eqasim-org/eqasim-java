package org.eqasim.examples.auckland_av;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Person;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import ch.ethz.matsim.discrete_mode_choice.model.mode_availability.ModeAvailability;

public class AucklandAvModeAvailability implements ModeAvailability {
	static public final String NAME = "AucklandAvModeAvailability";

	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		return Arrays.asList("car", "pt", "walk", "av");
	}
}
