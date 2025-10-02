package org.eqasim.bavaria.mode_choice;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eqasim.bavaria.mode_choice.utilities.predictors.BavariaPredictorUtils;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;

public class BavariaModeAvailability implements ModeAvailability {
	private final Set<String> additionalModes;

	public BavariaModeAvailability(Set<String> additionalModes) {
		this.additionalModes = additionalModes;
	}

	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		Collection<String> modes = new HashSet<>();

		// Modes that are always available
		modes.add(TransportMode.walk);
		modes.add(TransportMode.pt);

		// Check car availability
		if (BavariaPredictorUtils.hasCarAvailability(person)) {
			modes.add(BavariaModeChoiceModule.CAR_PASSENGER);

			if (BavariaPredictorUtils.hasDrivingLicense(person)) {
				modes.add(TransportMode.car);
			}
		}

		// Check bicycle availability
		if (BavariaPredictorUtils.hasBicycleAvailability(person)) {
			modes.add(BavariaModeChoiceModule.BICYCLE);
		}

		// Add special mode "outside" if applicable
		if (BavariaPredictorUtils.isOutside(person)) {
			modes.add("outside");
		}

		// Add additional modes
		modes.addAll(additionalModes);

		return modes;
	}
}
