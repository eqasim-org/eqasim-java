package org.eqasim.projects.astra16.mode_choice;

import java.util.Collection;
import java.util.List;

import org.eqasim.switzerland.mode_choice.SwissModeAvailability;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;

import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import ch.ethz.matsim.discrete_mode_choice.model.mode_availability.ModeAvailability;

public class AstraModeAvailability implements ModeAvailability {
	public static final String NAME = "AstraModeAvailability";

	private final SwissModeAvailability delegate;
	private final boolean useAv;

	public AstraModeAvailability(boolean useAv, SwissModeAvailability delegate) {
		this.useAv = useAv;
		this.delegate = delegate;
	}

	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		Collection<String> modes = delegate.getAvailableModes(person, trips);

		/*
		 * if (useAv && modes.contains(TransportMode.walk)) { Coord homeLocation =
		 * SwissPredictorUtils.getHomeLocation(person);
		 * 
		 * if (homeLocation != null && operatingArea.covers(homeLocation)) {
		 * modes.add(AVModule.AV_MODE); } }
		 */

		if (useAv && modes.contains(TransportMode.walk)) {
			modes.add(AVModule.AV_MODE);
		}

		return modes;
	}
}
