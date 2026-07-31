package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eqasim.core.simulation.mode_choice.utilities.EqasimUtilityEstimator;
import org.eqasim.switzerland.ch_cmdp.config.SwissIntermodalAccessEgressConfigGroup;
import org.eqasim.switzerland.ch_cmdp.routing.IntermodalVehicleRoutingAttributes;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.VehicleTourConstraintConfigGroup;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;

import com.google.inject.Inject;

/**
 * Wraps the normal Eqasim trip estimator and adds routing-time instructions for
 * PT trips that need to retrieve a private vehicle left at a transit stop.
 */
public class IntermodalVehicleContinuityTripEstimator implements TripEstimator {
	private final TripEstimator delegate;
	private final Collection<String> vehicleModes;
	private final SwissIntermodalAccessEgressConfigGroup config;
	private final Map<PtCacheKey, TripCandidate> ptCache = new HashMap<>();

	@Inject
	public IntermodalVehicleContinuityTripEstimator(EqasimUtilityEstimator delegate,
			DiscreteModeChoiceConfigGroup dmcConfig, SwissIntermodalAccessEgressConfigGroup config) {
		this(delegate, dmcConfig.getVehicleTourConstraintConfig(), config);
	}

	IntermodalVehicleContinuityTripEstimator(TripEstimator delegate, VehicleTourConstraintConfigGroup vehicleConfig,
			SwissIntermodalAccessEgressConfigGroup config) {
		this(delegate, vehicleConfig.getRestrictedModes(), config);
	}

	IntermodalVehicleContinuityTripEstimator(TripEstimator delegate, Collection<String> vehicleModes,
			SwissIntermodalAccessEgressConfigGroup config) {
		this.delegate = delegate;
		this.vehicleModes = List.copyOf(vehicleModes);
		this.config = config;
	}

	@Override
	public TripCandidate estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips) {
		// The regular estimator is used unless the opt-in switch is enabled, the
		// current trip returns to the configured home activity type, and a previous
		// trip left a restricted vehicle at a PT stop.
		if (!config.enforceIntermodalVehicleContinuityDuringRouting()
				|| !isHomeActivity(trip.getDestinationActivity())) {
			return estimateDelegate(person, mode, trip, previousTrips);
		}

		ParkedVehicle parkedVehicle = findParkedVehicle(previousTrips);
		if (parkedVehicle == null) {
			return estimateDelegate(person, mode, trip, previousTrips);
		}

		if (!TransportMode.pt.equals(mode)) {
			return estimateDelegate(person, mode, trip, previousTrips);
		}

		// TripRouter passes trip attributes down into SwissRailRaptor. We add a
		// temporary access ban and egress requirement so the return PT leg cannot
		// use the parked vehicle before transit, and must pick it up afterwards.
		Attributes attributes = trip.getTripAttributes();
		Object previousForbiddenAccess = attributes.putAttribute(
				IntermodalVehicleRoutingAttributes.FORBIDDEN_ACCESS_MODE, parkedVehicle.mode);
		Object previousMode = attributes.putAttribute(IntermodalVehicleRoutingAttributes.REQUIRED_EGRESS_MODE,
				parkedVehicle.mode);
		Object previousStop = attributes.putAttribute(IntermodalVehicleRoutingAttributes.REQUIRED_EGRESS_STOP_ID,
				parkedVehicle.stopId.toString());

		try {
			return estimateDelegate(person, mode, trip, previousTrips);
		} finally {
			// Restore attributes because the same DiscreteModeChoiceTrip object can be
			// reused for other candidate branches.
			restoreAttribute(attributes, IntermodalVehicleRoutingAttributes.FORBIDDEN_ACCESS_MODE,
					previousForbiddenAccess);
			restoreAttribute(attributes, IntermodalVehicleRoutingAttributes.REQUIRED_EGRESS_MODE, previousMode);
			restoreAttribute(attributes, IntermodalVehicleRoutingAttributes.REQUIRED_EGRESS_STOP_ID, previousStop);
		}
	}

	private TripCandidate estimateDelegate(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips) {
		if (!TransportMode.pt.equals(mode)) {
			return delegate.estimateTrip(person, mode, trip, previousTrips);
		}

		// PT routing is relatively expensive, so cache it by the attributes that
		// affect access/egress stop filtering. The same trip may be estimated both
		// unconstrained and with a required vehicle retrieval stop.
		PtCacheKey key = PtCacheKey.from(trip);
		TripCandidate candidate = ptCache.get(key);
		if (candidate == null) {
			candidate = delegate.estimateTrip(person, mode, trip, previousTrips);
			ptCache.put(key, candidate);
		}
		return candidate;
	}

	private ParkedVehicle findParkedVehicle(List<TripCandidate> previousTrips) {
		// Track the stop where each restricted vehicle is currently parked. Null
		// means the vehicle is not waiting at a transit stop.
		Map<String, Id<TransitStopFacility>> parkedStops = new LinkedHashMap<>();
		for (String vehicleMode : vehicleModes) {
			parkedStops.put(vehicleMode, null);
		}

		for (TripCandidate previousTrip : previousTrips) {
			for (String vehicleMode : vehicleModes) {
				// A direct car/bike trip takes the vehicle out of the PT-stop parking
				// state because it is no longer left at a transit stop.
				if (vehicleMode.equals(previousTrip.getMode())) {
					parkedStops.put(vehicleMode, null);
					continue;
				}

				if (!(previousTrip instanceof RoutedTripCandidate routedTripCandidate)) {
					continue;
				}

				IntermodalVehicleUse use = IntermodalVehicleUse.from(routedTripCandidate.getRoutedPlanElements(),
						vehicleMode);

				// Access by restricted vehicle parks it at the first PT boarding stop.
				if (use.usesAccess) {
					parkedStops.put(vehicleMode, use.accessStopId);
				}

				// Egress by restricted vehicle means it has been picked up again.
				if (use.usesEgress) {
					parkedStops.put(vehicleMode, null);
				}
			}
		}

		for (Map.Entry<String, Id<TransitStopFacility>> entry : parkedStops.entrySet()) {
			if (entry.getValue() != null) {
				return new ParkedVehicle(entry.getKey(), entry.getValue());
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

			// Legs before the first PT leg are access to transit.
			for (int i = 0; i < firstPtIndex; i++) {
				if (elements.get(i) instanceof Leg leg && vehicleMode.equals(leg.getMode())) {
					usesAccess = true;
				}
			}

			// Legs after the last PT leg are egress from transit.
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
		private final Id<TransitStopFacility> stopId;

		private ParkedVehicle(String mode, Id<TransitStopFacility> stopId) {
			this.mode = mode;
			this.stopId = stopId;
		}
	}

	static private record PtCacheKey(DiscreteModeChoiceTrip trip, double departureTime, String forbiddenAccessMode,
			String forbiddenEgressMode, String requiredAccessMode, String requiredAccessStopId, String requiredEgressMode,
			String requiredEgressStopId) {
		static private PtCacheKey from(DiscreteModeChoiceTrip trip) {
			// Routing attributes are temporary branch state, but they determine which
			// access/egress stops SwissRailRaptor may use, so they must be part of
			// the cache key to avoid reusing the wrong PT route.
			Attributes attributes = trip.getTripAttributes();
			return new PtCacheKey(trip, getDepartureTime(trip),
					getAttribute(attributes, IntermodalVehicleRoutingAttributes.FORBIDDEN_ACCESS_MODE),
					getAttribute(attributes, IntermodalVehicleRoutingAttributes.FORBIDDEN_EGRESS_MODE),
					getAttribute(attributes, IntermodalVehicleRoutingAttributes.REQUIRED_ACCESS_MODE),
					getAttribute(attributes, IntermodalVehicleRoutingAttributes.REQUIRED_ACCESS_STOP_ID),
					getAttribute(attributes, IntermodalVehicleRoutingAttributes.REQUIRED_EGRESS_MODE),
					getAttribute(attributes, IntermodalVehicleRoutingAttributes.REQUIRED_EGRESS_STOP_ID));
		}

		static private double getDepartureTime(DiscreteModeChoiceTrip trip) {
			try {
				return trip.getDepartureTime();
			} catch (NoSuchElementException e) {
				return Double.NaN;
			}
		}

		static private String getAttribute(Attributes attributes, String name) {
			Object value = attributes.getAttribute(name);
			return value == null ? null : value.toString();
		}
	}
}
