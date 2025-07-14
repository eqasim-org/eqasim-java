package org.eqasim.switzerland.ch.mode_choice.constraints;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;

public class LoopModesConstraint extends AbstractTripConstraint {
	public static final String LOOP_MODE = "_loop";

	@Override
	public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
		if (trip.getInitialMode().contains(LOOP_MODE)) {
			if (!mode.contains(LOOP_MODE)) {
				return false;
			}
		}

		if (mode.contains(LOOP_MODE)) {
			if (!trip.getInitialMode().contains(LOOP_MODE)) {
				return false;
			}
		}

		return true;
	}

	static public class Factory implements TripConstraintFactory {
		@Override
		public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
				Collection<String> availableModes) {
			return new LoopModesConstraint();
		}
	}
}
