package org.eqasim.switzerland.zurich.mode_choice;

import java.util.Collection;
import java.util.List;

import org.eqasim.switzerland.ch.mode_choice.SwissModeAvailability;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;


public class ZurichModeAvailability implements ModeAvailability {
	public static final String NAME = "ZurichModeAvailability";

	private final SwissModeAvailability delegate;

	public ZurichModeAvailability(SwissModeAvailability delegate) {
		this.delegate = delegate;
	}

	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		Collection<String> modes = delegate.getAvailableModes(person, trips);

		return modes;
	}
}