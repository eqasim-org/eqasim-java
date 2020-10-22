package org.eqasim.projects.astra16.mode_choice;

import java.util.Collection;
import java.util.List;

import org.eqasim.projects.astra16.service_area.ServiceArea;
import org.eqasim.switzerland.mode_choice.SwissModeAvailability;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissPredictorUtils;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;

public class AstraModeAvailability implements ModeAvailability {
	public static final String NAME = "AstraModeAvailability";

	private final SwissModeAvailability delegate;
	private final boolean useAv;
	private final ServiceArea serviceArea;

	public AstraModeAvailability(boolean useAv, SwissModeAvailability delegate, ServiceArea serviceArea) {
		this.useAv = useAv;
		this.delegate = delegate;
		this.serviceArea = serviceArea;
	}

	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		Collection<String> modes = delegate.getAvailableModes(person, trips);

		if (useAv && modes.contains(TransportMode.walk)) {
			Coord homeLocation = SwissPredictorUtils.getHomeLocation(person);

			if (homeLocation != null && serviceArea.covers(homeLocation)) {
				modes.add("av");
			}
		}

		return modes;
	}
}
