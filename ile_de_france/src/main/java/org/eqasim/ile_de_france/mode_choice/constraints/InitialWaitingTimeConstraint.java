package org.eqasim.ile_de_france.mode_choice.constraints;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.pt.routes.TransitPassengerRoute;

public class InitialWaitingTimeConstraint extends AbstractTripConstraint {
	private final double maximumInitialWaitingTime_min;

	InitialWaitingTimeConstraint(double maximumInitialWaitingTime_min) {
		this.maximumInitialWaitingTime_min = maximumInitialWaitingTime_min;
	}

	@Override
	public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate,
			List<TripCandidate> previousCandidates) {
		if (candidate.getMode().equals(TransportMode.pt)) {
			if (candidate instanceof RoutedTripCandidate) {
				// Go through all legs

				for (PlanElement element : ((RoutedTripCandidate) candidate).getRoutedPlanElements()) {
					if (element instanceof Leg) {
						Leg leg = (Leg) element;

						if (leg.getRoute() instanceof TransitPassengerRoute route) {
							// We check the first PT leg and make a decision
							double departureTime = leg.getDepartureTime().seconds();
							double waitingTime_min = (route.getBoardingTime().seconds() - departureTime) / 60.0;

							if (waitingTime_min > maximumInitialWaitingTime_min) {
								return false;
							}

							return true;
						}
					}
				}
			} else {
				throw new IllegalStateException("Need a route to evaluate constraint");
			}
		}

		return true;
	}

	static public class Factory implements TripConstraintFactory {
		private final double maximumInitialWaitingTime_min;

		public Factory(double maximumInitialWaitingTime_min) {
			this.maximumInitialWaitingTime_min = maximumInitialWaitingTime_min;
		}

		@Override
		public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> trips,
				Collection<String> availableModes) {
			return new InitialWaitingTimeConstraint(maximumInitialWaitingTime_min);
		}
	}
}