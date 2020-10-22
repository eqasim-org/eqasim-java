package org.eqasim.core.simulation.mode_choice.constraints;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;

public class PassengerConstraint extends AbstractTripConstraint {
	public static final String PASSENGER_MODE = "car_passenger";

	@Override
	public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
		if (trip.getInitialMode().equals(PASSENGER_MODE)) {
			if (!mode.equals(PASSENGER_MODE)) {
				return false;
			}
		}

		if (mode.equals(PASSENGER_MODE)) {
			if (!trip.getInitialMode().equals(PASSENGER_MODE)) {
				return false;
			}
		}

		return true;
	}

	static public class Factory implements TripConstraintFactory {
		@Override
		public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
				Collection<String> availableModes) {
			return new PassengerConstraint();
		}
	}
}
