package org.eqasim.automated_vehicles.mode_choice.constraints;

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

public class AvWalkConstraint extends AbstractTripConstraint {
	@Override
	public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate,
			List<TripCandidate> previousCandidates) {
		if (candidate.getMode().equals("av")) {
			if (candidate instanceof RoutedTripCandidate) {
				// Go through all plan elments
				for (PlanElement element : ((RoutedTripCandidate) candidate).getRoutedPlanElements()) {
					if (element instanceof Leg) {
						if (((Leg) element).getMode().equals("av")) {
							// If we find at least one av leg, we're good
							return true;
						}
					}
				}

				// If there was no av leg, we do not accept this candidate
				return false;
			} else {
				throw new IllegalStateException("Need a route to evaluate constraint");
			}
		}

		return true;
	}

	static public class Factory implements TripConstraintFactory {
		@Override
		public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> trips,
				Collection<String> availableModes) {
			return new AvWalkConstraint();
		}
	}
}