package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * Estimates tours sequentially and prevents PT access by restricted private
 * vehicles when the remaining mode chain cannot bring them back home later in
 * the tour.
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
		List<Set<String>> forcedForbiddenAccessModes = createForcedForbiddenAccessModes(trips.size());
		int maximumAttempts = Math.max(1, config.getRestrictedIntermodalAccessEgressModes().size()) + 1;
		RuntimeException lastFailure = null;

		// Routing is sequential: an early PT trip may park a vehicle that a later
		// return-home PT trip cannot retrieve. In that case, retry the whole tour
		// with that earlier access mode forbidden so Raptor can fall back to walk
		// access. The bound is one retry per restricted vehicle mode.
		for (int attempt = 0; attempt < maximumAttempts; attempt++) {
			try {
				return estimateTourAttempt(person, modes, trips, forcedForbiddenAccessModes);
			} catch (RetryTourEstimationException e) {
				lastFailure = e.failure;
				if (!forcedForbiddenAccessModes.get(e.sourceTripIndex).add(e.vehicleMode)) {
					throw e.failure;
				}
			}
		}

		throw lastFailure == null ? new IllegalStateException("Unable to estimate intermodal vehicle tour.")
				: lastFailure;
	}

	private TourCandidate estimateTourAttempt(Person person, List<String> modes, List<DiscreteModeChoiceTrip> trips,
			List<Set<String>> forcedForbiddenAccessModes) {
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

			TripCandidate tripCandidate;
			try {
				tripCandidate = estimateTrip(person, mode, trip, tripCandidates, modes, trips, i,
						forcedForbiddenAccessModes.get(i));
			} catch (RuntimeException e) {
				RetryInstruction retry = getRetryInstruction(mode, trip, tripCandidates);
				if (retry == null) {
					throw e;
				}
				throw new RetryTourEstimationException(retry.sourceTripIndex, retry.vehicleMode, e);
			}

			utility += tripCandidate.getUtility();
			timeTracker.addDuration(tripCandidate.getDuration());

			tripCandidates.add(tripCandidate);
		}

		return new DefaultTourCandidate(utility, tripCandidates);
	}

	private TripCandidate estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips, List<String> modes, List<DiscreteModeChoiceTrip> trips, int tripIndex,
			Collection<String> forcedForbiddenAccessModes) {
		Set<String> forbiddenAccessModes = new LinkedHashSet<>(forcedForbiddenAccessModes);
		forbiddenAccessModes.addAll(getForbiddenAccessModes(mode, previousTrips, modes, trips, tripIndex));
		if (forbiddenAccessModes.isEmpty()) {
			return delegate.estimateTrip(person, mode, trip, previousTrips);
		}

		// This branch is only for PT candidates where taking the restricted vehicle
		// to access transit would strand it at a stop with no later PT-home leg to
		// retrieve it, or where a retry forbids an earlier access mode that led to
		// an infeasible return egress. The stop finder sees this temporary
		// attribute and removes those access stop options, leaving regular walk
		// access available.
		Attributes attributes = trip.getTripAttributes();
		Object previousForbiddenAccess = attributes.putAttribute(
				IntermodalVehicleRoutingAttributes.FORBIDDEN_ACCESS_MODE, String.join(",", forbiddenAccessModes));

		try {
			return delegate.estimateTrip(person, mode, trip, previousTrips);
		} finally {
			restoreAttribute(attributes, IntermodalVehicleRoutingAttributes.FORBIDDEN_ACCESS_MODE,
					previousForbiddenAccess);
		}
	}

	private Collection<String> getForbiddenAccessModes(String mode, List<TripCandidate> previousTrips, List<String> modes,
			List<DiscreteModeChoiceTrip> trips, int tripIndex) {
		if (!config.enforceIntermodalVehicleContinuityDuringRouting() || !TransportMode.pt.equals(mode)) {
			return List.of();
		}

		boolean hasLaterPtTripReturningHome = hasLaterPtTripReturningHome(modes, trips, tripIndex + 1);
		List<String> forbiddenModes = new ArrayList<>();
		for (String vehicleMode : config.getRestrictedIntermodalAccessEgressModes()) {
			if (isParkedAtPtStop(previousTrips, vehicleMode)) {
				// Once the vehicle is already parked at a PT stop, another access
				// before retrieving it would imply using a vehicle that is not at origin.
				forbiddenModes.add(vehicleMode);
				continue;
			}

			// If no later PT trip returns home, this PT leg cannot safely park the
			// vehicle at an access stop because the tour has no opportunity to pick it
			// up again.
			if (!hasLaterPtTripReturningHome) {
				forbiddenModes.add(vehicleMode);
			}
		}

		return forbiddenModes;
	}

	private RetryInstruction getRetryInstruction(String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips) {
		if (!config.enforceIntermodalVehicleContinuityDuringRouting() || !TransportMode.pt.equals(mode)
				|| !isHomeActivity(trip.getDestinationActivity())) {
			return null;
		}

		ParkedVehicle parkedVehicle = findParkedVehicleWithSource(previousTrips);
		if (parkedVehicle == null) {
			return null;
		}

		return new RetryInstruction(parkedVehicle.sourceTripIndex, parkedVehicle.mode);
	}

	private boolean hasLaterPtTripReturningHome(List<String> modes, List<DiscreteModeChoiceTrip> trips, int startIndex) {
		for (int i = startIndex; i < modes.size(); i++) {
			if (TransportMode.pt.equals(modes.get(i)) && isHomeActivity(trips.get(i).getDestinationActivity())) {
				return true;
			}
		}

		return false;
	}

	private boolean isParkedAtPtStop(List<TripCandidate> previousTrips, String vehicleMode) {
		Id<TransitStopFacility> parkedStopId = null;

		for (TripCandidate previousTrip : previousTrips) {
			if (vehicleMode.equals(previousTrip.getMode())) {
				parkedStopId = null;
				continue;
			}

			if (!(previousTrip instanceof RoutedTripCandidate routedTripCandidate)) {
				continue;
			}

			IntermodalVehicleUse use = IntermodalVehicleUse.from(routedTripCandidate.getRoutedPlanElements(),
					vehicleMode);
			if (use.usesAccess) {
				parkedStopId = use.accessStopId;
			}
			if (use.usesEgress) {
				parkedStopId = null;
			}
		}

		return parkedStopId != null;
	}

	private ParkedVehicle findParkedVehicleWithSource(List<TripCandidate> previousTrips) {
		Map<String, ParkedVehicle> parkedVehicles = new LinkedHashMap<>();
		for (String vehicleMode : config.getRestrictedIntermodalAccessEgressModes()) {
			parkedVehicles.put(vehicleMode, null);
		}

		for (int i = 0; i < previousTrips.size(); i++) {
			TripCandidate previousTrip = previousTrips.get(i);
			for (String vehicleMode : config.getRestrictedIntermodalAccessEgressModes()) {
				if (vehicleMode.equals(previousTrip.getMode())) {
					parkedVehicles.put(vehicleMode, null);
					continue;
				}

				if (!(previousTrip instanceof RoutedTripCandidate routedTripCandidate)) {
					continue;
				}

				IntermodalVehicleUse use = IntermodalVehicleUse.from(routedTripCandidate.getRoutedPlanElements(),
						vehicleMode);
				if (use.usesAccess) {
					parkedVehicles.put(vehicleMode, new ParkedVehicle(vehicleMode, i));
				}
				if (use.usesEgress) {
					parkedVehicles.put(vehicleMode, null);
				}
			}
		}

		for (ParkedVehicle parkedVehicle : parkedVehicles.values()) {
			if (parkedVehicle != null) {
				return parkedVehicle;
			}
		}

		return null;
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

	static private List<Set<String>> createForcedForbiddenAccessModes(int size) {
		List<Set<String>> forbiddenAccessModes = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			forbiddenAccessModes.add(new LinkedHashSet<>());
		}
		return forbiddenAccessModes;
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

	static private class ParkedVehicle {
		private final String mode;
		private final int sourceTripIndex;

		private ParkedVehicle(String mode, int sourceTripIndex) {
			this.mode = mode;
			this.sourceTripIndex = sourceTripIndex;
		}
	}

	static private class RetryInstruction {
		private final int sourceTripIndex;
		private final String vehicleMode;

		private RetryInstruction(int sourceTripIndex, String vehicleMode) {
			this.sourceTripIndex = sourceTripIndex;
			this.vehicleMode = vehicleMode;
		}
	}

	static private class RetryTourEstimationException extends RuntimeException {
		private final int sourceTripIndex;
		private final String vehicleMode;
		private final RuntimeException failure;

		private RetryTourEstimationException(int sourceTripIndex, String vehicleMode, RuntimeException failure) {
			super(failure);
			this.sourceTripIndex = sourceTripIndex;
			this.vehicleMode = vehicleMode;
			this.failure = failure;
		}
	}
}
