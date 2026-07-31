package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators;

import java.util.LinkedList;
import java.util.List;

import org.eqasim.switzerland.ch_cmdp.config.SwissIntermodalAccessEgressConfigGroup;
import org.eqasim.switzerland.ch_cmdp.routing.IntermodalVehicleRoutingAttributes;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.DefaultTourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;

import com.google.inject.Inject;

/**
 * Estimates tours sequentially and prevents PT access by bike when the remaining
 * mode chain cannot bring that bike back home later in the tour.
 */
public class IntermodalVehicleContinuityTourEstimator implements TourEstimator {
	private final TripEstimator delegate;
	private final TimeInterpretation timeInterpretation;
	private final SwissIntermodalAccessEgressConfigGroup config;

	@Inject
	public IntermodalVehicleContinuityTourEstimator(TripEstimator delegate, TimeInterpretation timeInterpretation,
			SwissIntermodalAccessEgressConfigGroup config) {
		this.delegate = delegate;
		this.timeInterpretation = timeInterpretation;
		this.config = config;
	}

	@Override
	public TourCandidate estimateTour(Person person, List<String> modes, List<DiscreteModeChoiceTrip> trips,
			List<TourCandidate> preceedingTours) {
		List<TripCandidate> tripCandidates = new LinkedList<>();
		double utility = 0.0;

		TimeTracker timeTracker = new TimeTracker(timeInterpretation);
		timeTracker.setTime(trips.get(0).getDepartureTime());

		for (int i = 0; i < modes.size(); i++) {
			String mode = modes.get(i);
			DiscreteModeChoiceTrip trip = trips.get(i);

			if (i > 0) {
				timeTracker.addActivity(trip.getOriginActivity());
				trip.setDepartureTime(timeTracker.getTime().seconds());
			}

			TripCandidate tripCandidate = estimateTrip(person, mode, trip, tripCandidates, modes, trips, i);
			utility += tripCandidate.getUtility();
			timeTracker.addDuration(tripCandidate.getDuration());

			tripCandidates.add(tripCandidate);
		}

		return new DefaultTourCandidate(utility, tripCandidates);
	}

	private TripCandidate estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips, List<String> modes, List<DiscreteModeChoiceTrip> trips, int tripIndex) {
		if (!shouldForbidBikeAccess(mode, previousTrips, modes, trips, tripIndex)) {
			return delegate.estimateTrip(person, mode, trip, previousTrips);
		}

		// This branch is only for PT candidates where taking the restricted vehicle
		// to access transit would strand it at a stop with no later PT-home leg to
		// retrieve it. The stop finder sees this temporary attribute and removes
		// those bike-access stop options, leaving regular walk access available.
		Attributes attributes = trip.getTripAttributes();
		Object previousForbiddenAccess = attributes.putAttribute(
				IntermodalVehicleRoutingAttributes.FORBIDDEN_ACCESS_MODE, config.getBikeRestrictedMode());

		try {
			return delegate.estimateTrip(person, mode, trip, previousTrips);
		} finally {
			restoreAttribute(attributes, IntermodalVehicleRoutingAttributes.FORBIDDEN_ACCESS_MODE,
					previousForbiddenAccess);
		}
	}

	private boolean shouldForbidBikeAccess(String mode, List<TripCandidate> previousTrips, List<String> modes,
			List<DiscreteModeChoiceTrip> trips, int tripIndex) {
		if (!config.enforceIntermodalVehicleContinuityDuringRouting() || !TransportMode.pt.equals(mode)) {
			return false;
		}

		String bikeMode = config.getBikeRestrictedMode();
		if (isParkedAtPtStop(previousTrips, bikeMode)) {
			// Once the vehicle is already parked at a PT stop, another bike access
			// before retrieving it would imply using a vehicle that is not at origin.
			return true;
		}

		// If no later PT trip returns home, this PT leg cannot safely park the bike
		// at an access stop because the tour has no opportunity to pick it up again.
		return !hasLaterPtTripReturningHome(modes, trips, tripIndex + 1);
	}

	private boolean hasLaterPtTripReturningHome(List<String> modes, List<DiscreteModeChoiceTrip> trips, int startIndex) {
		for (int i = startIndex; i < modes.size(); i++) {
			if (TransportMode.pt.equals(modes.get(i)) && isHomeActivity(trips.get(i).getDestinationActivity())) {
				return true;
			}
		}

		return false;
	}

	private boolean isParkedAtPtStop(List<TripCandidate> previousTrips, String bikeMode) {
		Id<TransitStopFacility> parkedStopId = null;

		for (TripCandidate previousTrip : previousTrips) {
			if (bikeMode.equals(previousTrip.getMode())) {
				parkedStopId = null;
				continue;
			}

			if (!(previousTrip instanceof RoutedTripCandidate routedTripCandidate)) {
				continue;
			}

			IntermodalVehicleUse use = IntermodalVehicleUse.from(routedTripCandidate.getRoutedPlanElements(),
					bikeMode);
			if (use.usesAccess) {
				parkedStopId = use.accessStopId;
			}
			if (use.usesEgress) {
				parkedStopId = null;
			}
		}

		return parkedStopId != null;
	}

	private boolean isHomeActivity(Activity activity) {
		return config.getIntermodalVehicleContinuityHomeActivityType().equals(activity.getType());
	}

	private void restoreAttribute(Attributes attributes, String name, Object previousValue) {
		if (previousValue == null) {
			attributes.removeAttribute(name);
		} else {
			attributes.putAttribute(name, previousValue);
		}
	}

	static private class IntermodalVehicleUse {
		private final boolean usesAccess;
		private final boolean usesEgress;
		private final Id<TransitStopFacility> accessStopId;

		private IntermodalVehicleUse(boolean usesAccess, boolean usesEgress,
				Id<TransitStopFacility> accessStopId) {
			this.usesAccess = usesAccess;
			this.usesEgress = usesEgress;
			this.accessStopId = accessStopId;
		}

		static private IntermodalVehicleUse from(List<? extends PlanElement> elements, String vehicleMode) {
			// First and last PT legs separate access legs from egress legs in a routed
			// intermodal PT trip.
			int firstPtIndex = -1;
			int lastPtIndex = -1;
			Id<TransitStopFacility> accessStopId = null;

			for (int i = 0; i < elements.size(); i++) {
				if (elements.get(i) instanceof Leg leg && leg.getRoute() instanceof TransitPassengerRoute route) {
					if (firstPtIndex < 0) {
						firstPtIndex = i;
						accessStopId = route.getAccessStopId();
					}
					lastPtIndex = i;
				}
			}

			if (firstPtIndex < 0) {
				return new IntermodalVehicleUse(false, false, null);
			}

			boolean usesAccess = false;
			boolean usesEgress = false;

			for (int i = 0; i < firstPtIndex; i++) {
				if (elements.get(i) instanceof Leg leg && vehicleMode.equals(leg.getMode())) {
					usesAccess = true;
				}
			}

			for (int i = lastPtIndex + 1; i < elements.size(); i++) {
				if (elements.get(i) instanceof Leg leg && vehicleMode.equals(leg.getMode())) {
					usesEgress = true;
				}
			}

			return new IntermodalVehicleUse(usesAccess, usesEgress, accessStopId);
		}
	}
}
