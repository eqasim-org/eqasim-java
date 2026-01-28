package org.sutlab.seville.mode_choice;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.sutlab.seville.mode_choice.utilities.predictors.SevillePredictorUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class SevilleModeAvailability implements ModeAvailability {
	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		Collection<String> modes = new HashSet<>();

		// Modes that are always available
		modes.add(TransportMode.walk);
		modes.add(TransportMode.pt);

		// Check car availability
		if (SevillePredictorUtils.hasCarAvailability(person)) {
			modes.add(SevilleModeChoiceModule.CAR_PASSENGER);

			if (SevillePredictorUtils.hasDrivingLicense(person)) {
				modes.add(TransportMode.car);
			}
		}

		// Check bicycle availability
		if (SevillePredictorUtils.hasBicycleAvailability(person)) {
			modes.add(SevilleModeChoiceModule.BICYCLE);
		}

		// Add special mode "outside" if applicable
		if (SevillePredictorUtils.isOutside(person)) {
			modes.add("outside");
		}

		return modes;
	}
}