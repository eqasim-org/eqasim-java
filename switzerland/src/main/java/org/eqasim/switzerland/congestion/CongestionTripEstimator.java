package org.eqasim.switzerland.congestion;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.TripEstimator;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.candidates.DefaultRoutedTripCandidate;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

public class CongestionTripEstimator implements TripEstimator {
	private final TripEstimator delegate;
	private final UtilityEstimator carEstimator;

	public CongestionTripEstimator(TripEstimator delegate, UtilityEstimator carEstimator) {
		this.delegate = delegate;
		this.carEstimator = carEstimator;
	}

	private double getTravelTime(List<? extends PlanElement> elements) {
		double travelTime = 0.0;

		for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;
				travelTime += leg.getTravelTime();
			}
		}

		return travelTime;
	}

	@Override
	public TripCandidate estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips) {
		TripCandidate delegateResult = delegate.estimateTrip(person, mode, trip, previousTrips);

		if (mode.equals(TransportMode.car) && trip.getInitialMode().equals(TransportMode.car)) {
			List<? extends PlanElement> updatedElements = ((RoutedTripCandidate) delegateResult)
					.getRoutedPlanElements();

			double initialTravelTime = getTravelTime(trip.getInitialElements());
			double updatedTravelTime = getTravelTime(updatedElements);

			boolean useExistingRoute = false;
			System.err.println("useExistingRoute: Tchervi, I'm set to false now. Change me in an intelligent way!");

			if (useExistingRoute) {
				double utility = carEstimator.estimateUtility(person, trip, trip.getInitialElements());
				return new DefaultRoutedTripCandidate(utility, TransportMode.car, trip.getInitialElements());
			} else {
				return delegateResult;
			}
		}

		return delegateResult;
	}
}
