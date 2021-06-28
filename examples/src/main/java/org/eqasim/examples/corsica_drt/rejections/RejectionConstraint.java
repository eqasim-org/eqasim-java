package org.eqasim.examples.corsica_drt.rejections;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;

public class RejectionConstraint extends AbstractTripConstraint {
	public final static String NAME = "RejectionConstraint";

	private final boolean rejected;
	private final Collection<String> modes;

	private RejectionConstraint(boolean rejected, Collection<String> modes) {
		this.modes = modes;
		this.rejected = rejected;
	}

	@Override
	public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
		if (modes.contains(mode)) {
			return !rejected;
		}

		return true;
	}

	public static class Factory implements TripConstraintFactory {
		private final RejectionTracker tracker;
		private final Random random;
		private final Collection<String> modes;

		public Factory(RejectionTracker tracker, Random random, Collection<String> modes) {
			this.tracker = tracker;
			this.random = random;
			this.modes = modes;
		}

		@Override
		public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
				Collection<String> availableModes) {
			return new RejectionConstraint(random.nextDouble() < tracker.getRejectionProbability(person.getId()),
					modes);
		}
	}
}
