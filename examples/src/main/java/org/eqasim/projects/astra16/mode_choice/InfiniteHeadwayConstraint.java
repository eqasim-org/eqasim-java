package org.eqasim.projects.astra16.mode_choice;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import ch.ethz.matsim.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.TripConstraint;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.TripConstraintFactory;

public class InfiniteHeadwayConstraint extends AbstractTripConstraint {
	static public final String NAME = "InfiniteHeadway";

	@Override
	public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
		if (mode.equals(TransportMode.pt)) {
			Double headway_min = (Double) trip.getOriginActivity().getAttributes().getAttribute("headway_min");

			if (headway_min != null) {
				return Double.isFinite(headway_min);
			}
		}

		return true;
	}

	static public class Factory implements TripConstraintFactory {
		@Override
		public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
				Collection<String> availableModes) {
			return new InfiniteHeadwayConstraint();
		}
	}
}
