package org.eqasim.switzerland.ch_cmdp.mode_choice.constraints;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

public class ValidBikesharingTripConstraint extends AbstractTripConstraint {
	@Override
	public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate,
			List<TripCandidate> previousCandidates) {
		if (candidate.getMode().contains("sharing")) {
			if (!(candidate instanceof RoutedTripCandidate routedTripCandidate)) {
				return false;
			}

			int legCount = 0;

			for (PlanElement element : routedTripCandidate.getRoutedPlanElements()) {
				if (element instanceof Leg) {
					legCount++;
					if (legCount > 1) {
						return true;
					}
				}
			}

			return false;
		}

		return true;
	}

	static public class Factory implements TripConstraintFactory {
		@Override
		public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
				Collection<String> availableModes) {
			return new ValidBikesharingTripConstraint();
		}
	}
}
