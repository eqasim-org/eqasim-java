package org.eqasim.projects.dynamic_av.mode_choice.constraints;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.geometry.CoordUtils;

import ch.ethz.matsim.av.routing.AVRoute;
import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.TripConstraint;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

public class AVTripConstraint implements TripConstraint {
	static public final String NAME = "AVTripConstraint";

	private final double minimumDistance_km;
	private final double maximumWaitTime_min;

	public AVTripConstraint(double minimumDistance_km, double maximumWaitTime_min) {
		this.minimumDistance_km = minimumDistance_km;
		this.maximumWaitTime_min = maximumWaitTime_min;
	}

	@Override
	public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
		if (mode.equals("av")) {
			double euclideanDistance_km = CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(),
					trip.getDestinationActivity().getCoord()) * 1e-3;

			if (euclideanDistance_km < minimumDistance_km) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate,
			List<TripCandidate> previousCandidates) {
		if (candidate.getMode().equals("av")) {
			List<? extends PlanElement> elements = ((RoutedTripCandidate) candidate).getRoutedPlanElements();

			for (PlanElement element : elements) {
				if (element instanceof Leg) {
					Leg leg = (Leg) element;

					if (leg.getMode().equals("av")) {
						AVRoute route = (AVRoute) leg.getRoute();

						if (route.getWaitingTime() / 60.0 > maximumWaitTime_min) {
							return false;
						}
					}
				}
			}
		}

		return true;
	}

	static public class Factory implements TripConstraintFactory {
		private final double minimumDistance_km;
		private final double maximumWaitTime_min;

		public Factory(double minimumDistance_km, double maximumWaitTime_min) {
			this.minimumDistance_km = minimumDistance_km;
			this.maximumWaitTime_min = maximumWaitTime_min;
		}

		@Override
		public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
				Collection<String> availableModes) {
			return new AVTripConstraint(minimumDistance_km, maximumWaitTime_min);
		}
	}
}
