package org.eqasim.projects.astra16.mode_choice;

import java.util.Collection;
import java.util.List;

import org.eqasim.projects.astra16.service_area.ServiceArea;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.geometry.CoordUtils;

import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.routing.AVRoute;
import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import ch.ethz.matsim.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.TripConstraint;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

public class AvServiceConstraint extends AbstractTripConstraint {
	static public final String NAME = "AVServiceConstraint";

	private final double minimumDistance_km;
	private final double maximumWaitTime_min;
	private final ServiceArea serviceArea;

	private AvServiceConstraint(ServiceArea serviceArea, double minimumDistance_km, double maximumWaitTime_min) {
		this.serviceArea = serviceArea;
		this.minimumDistance_km = minimumDistance_km;
		this.maximumWaitTime_min = maximumWaitTime_min;
	}

	public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
		if (mode.equals(AVModule.AV_MODE)) {
			// Require minimum distance

			double directDistance_km = 1e-3 * CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(),
					trip.getDestinationActivity().getCoord());

			if (directDistance_km < minimumDistance_km) {
				return false;
			}

			// Require service area

			return serviceArea.covers(trip.getOriginActivity().getCoord())
					&& serviceArea.covers(trip.getDestinationActivity().getCoord());
		}

		return true;
	}

	public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate,
			List<TripCandidate> previousCandidates) {
		if (candidate.getMode().equals(AVModule.AV_MODE)) {
			RoutedTripCandidate routedCandidate = (RoutedTripCandidate) candidate;

			double waitingTime_min = 0.0;

			for (PlanElement element : routedCandidate.getRoutedPlanElements()) {
				if (element instanceof Leg) {
					Leg leg = (Leg) element;

					if (leg.getMode().equals(AVModule.AV_MODE)) {
						AVRoute route = (AVRoute) leg.getRoute();
						waitingTime_min += route.getWaitingTime() / 60.0;
					}
				}
			}

			// Consider maximum wait time
			if (waitingTime_min > maximumWaitTime_min) {
				return false;
			}
		}

		return true;
	}

	static public class Factory implements TripConstraintFactory {
		private final double minimumDistance_km;
		private final double maximumWaitTime_min;
		private final ServiceArea serviceArea;

		public Factory(ServiceArea serviceArea, double minimumDistance_km, double maximumWaitTime_min) {
			this.minimumDistance_km = minimumDistance_km;
			this.serviceArea = serviceArea;
			this.maximumWaitTime_min = maximumWaitTime_min;
		}

		@Override
		public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
				Collection<String> availableModes) {
			return new AvServiceConstraint(serviceArea, minimumDistance_km, maximumWaitTime_min);
		}
	}
}
