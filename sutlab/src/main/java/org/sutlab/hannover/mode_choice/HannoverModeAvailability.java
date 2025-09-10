package org.sutlab.hannover.mode_choice;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.sutlab.hannover.mode_choice.utilities.predictors. HannoverPredictorUtils;

public class HannoverModeAvailability implements ModeAvailability {
	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		Collection<String> modes = new HashSet<>();

		// Modes that are always available
		modes.add(TransportMode.walk);
		modes.add(TransportMode.pt);

		// Check car availability
		if (HannoverPredictorUtils.hasCarAvailability(person)) {
			modes.add(HannoverModeChoiceModule.CAR_PASSENGER);

			if (HannoverPredictorUtils.hasDrivingLicense(person)) {
				modes.add(TransportMode.car);
			}
		}

		// Check bicycle availability
		if (HannoverPredictorUtils.hasBicycleAvailability(person)) {
			modes.add(HannoverModeChoiceModule.BICYCLE);
		}

		// Add special mode "outside" if applicable
		if (HannoverPredictorUtils.isOutside(person)) {
			modes.add("outside");
		}

		return modes;
	}
}