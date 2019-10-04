package org.eqasim.projects.dynamic_av;

import java.util.Collection;
import java.util.List;

import org.eqasim.projects.dynamic_av.service_area.OperatingArea;
import org.eqasim.switzerland.mode_choice.SwissModeAvailability;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissPredictorUtils;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import ch.ethz.matsim.discrete_mode_choice.model.mode_availability.ModeAvailability;

public class ProjectModeAvailability implements ModeAvailability {
	private final ModeAvailability delegate = new SwissModeAvailability();

	private final OperatingArea operatingArea;
	private final boolean useAv;

	@Inject
	public ProjectModeAvailability(OperatingArea operatingArea, ProjectConfigGroup config) {
		this.operatingArea = operatingArea;
		this.useAv = config.getUseAv();
	}

	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		Collection<String> modes = delegate.getAvailableModes(person, trips);

		if (useAv && modes.contains(TransportMode.walk)) {
			Coord homeLocation = SwissPredictorUtils.getHomeLocation(person);

			if (homeLocation != null && operatingArea.covers(homeLocation)) {
				modes.add("av");
			}
		}

		return modes;
	}
}
