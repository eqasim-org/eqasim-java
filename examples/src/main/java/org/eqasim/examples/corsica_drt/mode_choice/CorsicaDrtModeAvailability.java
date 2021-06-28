package org.eqasim.examples.corsica_drt.mode_choice;

import java.util.Collection;
import java.util.List;

import org.eqasim.ile_de_france.mode_choice.IDFModeAvailability;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;

public class CorsicaDrtModeAvailability implements ModeAvailability {
	static public final String NAME = "ParisDrtModeAvailability";

	private final ModeAvailability delegate = new IDFModeAvailability();

	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		Collection<String> modes = delegate.getAvailableModes(person, trips);

		if (modes.contains(TransportMode.walk)) {
			modes.add("drt");
		}

		return modes;
	}
}
