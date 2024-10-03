package org.eqasim.ile_de_france.mode_choice;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPredictorUtils;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;

public class IDFModeAvailability implements ModeAvailability {
	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		Collection<String> modes = new HashSet<>();

		// Modes that are always available
		modes.add(TransportMode.walk);
		modes.add(TransportMode.pt);
		modes.add(IDFModeChoiceModule.CAR_PASSENGER);

		// Check car availability
		if (IDFPredictorUtils.hasCarAvailability(person) && IDFPredictorUtils.hasDrivingLicense(person)) {
			modes.add(TransportMode.car);
		}

		// Check bicycle availability
		if (IDFPredictorUtils.hasBicycleAvailability(person)) {
			modes.add(IDFModeChoiceModule.BICYCLE);
		}

		// Add special mode "outside" if applicable
		if (IDFPredictorUtils.isOutside(person)) {
			modes.add("outside");
		}

		return modes;
	}
}
