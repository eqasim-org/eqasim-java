package org.eqasim.switzerland.ch_cmdp.mode_choice.constraints;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.components.utils.LocationUtils;
import org.matsim.contribs.discrete_mode_choice.components.utils.home_finder.HomeFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraint;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.VehicleTourConstraintConfigGroup;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Inject;

/**
 * Extends the usual vehicle continuity logic to intermodal PT routes. A private
 * vehicle can be used to reach a PT stop only if it is currently at the trip
 * origin, and it can be used after PT only if it was left at the alighting stop.
 */
public class IntermodalVehicleTourConstraint implements TourConstraint {
	static public final String NAME = "IntermodalVehicleTourConstraint";

	private final Collection<String> vehicleModes;
	private final LocationKey homeLocation;

	public IntermodalVehicleTourConstraint(Collection<String> vehicleModes,
			Id<? extends BasicLocation> homeLocationId) {
		this.vehicleModes = vehicleModes;
		this.homeLocation = homeLocationId == null ? null : LocationKey.activity(homeLocationId);
	}

	@Override
	public boolean validateBeforeEstimation(List<DiscreteModeChoiceTrip> tour, List<String> modes,
			List<List<String>> previousModes) {
		for (String vehicleMode : vehicleModes) {
			boolean locationKnown = true;
			LocationKey vehicleLocation = homeLocation;

			for (int i = 0; i < modes.size(); i++) {
				String mode = modes.get(i);

				if (vehicleMode.equals(mode)) {
					LocationKey origin = getOriginLocation(tour.get(i));
					if (locationKnown && !origin.equals(vehicleLocation)) {
						return false;
					}
					vehicleLocation = getDestinationLocation(tour.get(i));
					locationKnown = true;
				} else if (TransportMode.pt.equals(mode)) {
					// PT routes may contain intermodal access/egress vehicle legs, but
					// their stops are only known after routing.
					locationKnown = false;
				}
			}

			if (locationKnown && !Objects.equals(homeLocation, vehicleLocation)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean validateAfterEstimation(List<DiscreteModeChoiceTrip> tour, TourCandidate candidate,
			List<TourCandidate> previousCandidates) {
		// At the start of each tour, every restricted private vehicle is assumed
		// to be at home.
		Map<String, LocationKey> vehicleLocations = new HashMap<>();
		for (String vehicleMode : vehicleModes) {
			vehicleLocations.put(vehicleMode, homeLocation);
		}

		List<TripCandidate> tripCandidates = candidate.getTripCandidates();
		for (int i = 0; i < tripCandidates.size(); i++) {
			TripCandidate tripCandidate = tripCandidates.get(i);
			for (String vehicleMode : vehicleModes) {
				// A direct car/bike trip moves that vehicle from the activity origin
				// to the activity destination.
				if (vehicleMode.equals(tripCandidate.getMode())) {
					LocationKey origin = getOriginLocation(tour.get(i));
					if (!origin.equals(vehicleLocations.get(vehicleMode))) {
						return false;
					}
					vehicleLocations.put(vehicleMode, getDestinationLocation(tour.get(i)));
					continue;
				}

				// For PT/intermodal candidates, inspect the routed legs to see whether
				// the private vehicle is used before the first PT leg or after the last
				// PT leg.
				if (!(tripCandidate instanceof RoutedTripCandidate routedTripCandidate)) {
					continue;
				}

				List<? extends PlanElement> elements = routedTripCandidate.getRoutedPlanElements();
				IntermodalVehicleUse use = IntermodalVehicleUse.from(elements, vehicleMode);
				if (!use.isUsed()) {
					continue;
				}

				if (use.accessStopId == null || use.egressStopId == null) {
					return false;
				}

				// Access by private vehicle means the vehicle is left at the boarding
				// stop and must be picked up from there later.
				if (use.usesAccess) {
					LocationKey origin = getOriginLocation(tour.get(i));
					if (!origin.equals(vehicleLocations.get(vehicleMode))) {
						return false;
					}
					vehicleLocations.put(vehicleMode, LocationKey.stop(use.accessStopId));
				}

				// Egress by private vehicle means the vehicle must already be waiting
				// at the alighting stop.
				if (use.usesEgress) {
					LocationKey egressStop = LocationKey.stop(use.egressStopId);
					if (!egressStop.equals(vehicleLocations.get(vehicleMode))) {
						return false;
					}
					vehicleLocations.put(vehicleMode, getDestinationLocation(tour.get(i)));
				}
			}
		}

		// The tour is only feasible if all tracked vehicles have returned home by
		// the end of the tour.
		for (LocationKey vehicleLocation : vehicleLocations.values()) {
			if (!Objects.equals(homeLocation, vehicleLocation)) {
				return false;
			}
		}

		return true;
	}

	static private LocationKey getOriginLocation(DiscreteModeChoiceTrip trip) {
		return LocationKey.activity(LocationUtils.getLocationId(trip.getOriginActivity()));
	}

	static private LocationKey getDestinationLocation(DiscreteModeChoiceTrip trip) {
		return LocationKey.activity(LocationUtils.getLocationId(trip.getDestinationActivity()));
	}

	static private class IntermodalVehicleUse {
		private final boolean usesAccess;
		private final boolean usesEgress;
		private final Id<TransitStopFacility> accessStopId;
		private final Id<TransitStopFacility> egressStopId;

		private IntermodalVehicleUse(boolean usesAccess, boolean usesEgress, Id<TransitStopFacility> accessStopId,
				Id<TransitStopFacility> egressStopId) {
			this.usesAccess = usesAccess;
			this.usesEgress = usesEgress;
			this.accessStopId = accessStopId;
			this.egressStopId = egressStopId;
		}

		static private IntermodalVehicleUse from(List<? extends PlanElement> elements, String vehicleMode) {
			// The first and last transit passenger routes define the PT boarding and
			// alighting stops for the whole routed PT part of the trip.
			int firstPtIndex = -1;
			int lastPtIndex = -1;
			Id<TransitStopFacility> accessStopId = null;
			Id<TransitStopFacility> egressStopId = null;

			for (int i = 0; i < elements.size(); i++) {
				if (elements.get(i) instanceof Leg leg && leg.getRoute() instanceof TransitPassengerRoute route) {
					if (firstPtIndex < 0) {
						firstPtIndex = i;
						accessStopId = route.getAccessStopId();
					}
					lastPtIndex = i;
					egressStopId = route.getEgressStopId();
				}
			}

			if (firstPtIndex < 0) {
				return new IntermodalVehicleUse(false, false, null, null);
			}

			boolean usesAccess = false;
			boolean usesEgress = false;

			// Vehicle legs before the first PT leg are access legs.
			for (int i = 0; i < firstPtIndex; i++) {
				if (elements.get(i) instanceof Leg leg && vehicleMode.equals(leg.getMode())) {
					usesAccess = true;
				}
			}

			// Vehicle legs after the last PT leg are egress legs.
			for (int i = lastPtIndex + 1; i < elements.size(); i++) {
				if (elements.get(i) instanceof Leg leg && vehicleMode.equals(leg.getMode())) {
					usesEgress = true;
				}
			}

			return new IntermodalVehicleUse(usesAccess, usesEgress, accessStopId, egressStopId);
		}

		private boolean isUsed() {
			return usesAccess || usesEgress;
		}
	}

	static private class LocationKey {
		private final String type;
		private final String id;

		// Activity location ids and transit stop ids can have the same text, so
		// keep them in separate namespaces.
		private LocationKey(String type, Object id) {
			this.type = type;
			this.id = id.toString();
		}

		static private LocationKey activity(Id<? extends BasicLocation> locationId) {
			return new LocationKey("activity", locationId);
		}

		static private LocationKey stop(Id<TransitStopFacility> stopId) {
			return new LocationKey("stop", stopId);
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof LocationKey otherLocation)) {
				return false;
			}
			return type.equals(otherLocation.type) && id.equals(otherLocation.id);
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, id);
		}
	}

	static public class Factory implements TourConstraintFactory {
		private final Collection<String> vehicleModes;
		private final HomeFinder homeFinder;

		@Inject
		public Factory(DiscreteModeChoiceConfigGroup dmcConfig, HomeFinder homeFinder) {
			// Reuse the same restricted modes as MATSim's vehicle tour continuity
			// constraint, typically car and bike.
			VehicleTourConstraintConfigGroup config = dmcConfig.getVehicleTourConstraintConfig();
			this.vehicleModes = config.getRestrictedModes();
			this.homeFinder = homeFinder;
		}

		public Factory(Collection<String> vehicleModes, HomeFinder homeFinder) {
			this.vehicleModes = vehicleModes;
			this.homeFinder = homeFinder;
		}

		@Override
		public TourConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
				Collection<String> availableModes) {
			return new IntermodalVehicleTourConstraint(vehicleModes, homeFinder.getHomeLocationId(planTrips));
		}
	}
}
