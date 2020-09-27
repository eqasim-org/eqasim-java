package org.eqasim.core.components.transit.connection;

import org.eqasim.core.components.transit.departure.DepartureFinder;
import org.eqasim.core.components.transit.departure.DepartureFinder.NoDepartureFoundException;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DefaultTransitConnectionFinder implements TransitConnectionFinder {
	final private DepartureFinder departureFinder;

	@Inject
	public DefaultTransitConnectionFinder(DepartureFinder departureFinder) {
		this.departureFinder = departureFinder;
	}

	private int updateAccessStopIndex(TransitRoute transitRoute, Id<TransitStopFacility> stopFacilityId,
			double minimumDepartureTime, int accessStopIndex, int egressStopIndex) {
		for (int i = egressStopIndex - 1; i > accessStopIndex; i--) {
			TransitRouteStop stop = transitRoute.getStops().get(i);

			if (stop.getStopFacility().getId().equals(stopFacilityId)) {
				try {
					departureFinder.findDeparture(transitRoute, stop, minimumDepartureTime);
					return i; // Return if a departure is found
				} catch (NoDepartureFoundException e) {
				}
			}
		}

		return accessStopIndex;
	}

	private int findStopIndex(TransitRoute transitRoute, Id<TransitStopFacility> stopFacilityId,
			double minimumDepartureTime, int minimumIndex) {

		for (int i = minimumIndex; i < transitRoute.getStops().size(); i++) {
			TransitRouteStop stop = transitRoute.getStops().get(i);

			if (stop.getStopFacility().getId().equals(stopFacilityId)) {
				try {
					departureFinder.findDeparture(transitRoute, stop, minimumDepartureTime);
					return i; // Return if a departure is found
				} catch (NoDepartureFoundException e) {
				}
			}
		}

		TransitRouteStop initialStop = transitRoute.getStops().get(minimumIndex);

		throw new IllegalStateException("Cannot find stop facility " + stopFacilityId + " on route "
				+ transitRoute.getId() + " after " + Time.writeTime(minimumDepartureTime) + " and stop facility "
				+ initialStop.getStopFacility().getId() + " (Index " + minimumIndex + ")");
	}

	@Override
	public TransitConnection findConnection(double connectionDepartureTime, double totalTravelTime,
			Id<TransitStopFacility> accessStopId, Id<TransitStopFacility> egressStopId, TransitRoute transitRoute)
			throws NoConnectionFoundException {
		try {
			// Recovering the correct access and egress stops from the informaton given in
			// the ExperimentalTransitRoutes is tricky. Please refer to
			// https://matsim.atlassian.net/browse/MATSIM-790 to understand why this is so
			// complicated here.

			double waitingTime = 0.0;
			double inVehicleTime = 0.0;

			Departure routeDeparture = null;
			TransitRouteStop accessStop;
			TransitRouteStop egressStop;

			int minimumAccessStopIndex = 0;

			do {
				// Find the first stop with the given access stop id with a departure after the
				// leg departure time
				int accessStopIndex = findStopIndex(transitRoute, accessStopId, connectionDepartureTime,
						minimumAccessStopIndex);
				accessStop = transitRoute.getStops().get(accessStopIndex);

				// Find the corresponding departure
				routeDeparture = departureFinder.findDeparture(transitRoute, accessStop, connectionDepartureTime);
				double vehicleDepartureTime = accessStop.getDepartureOffset().seconds()
						+ routeDeparture.getDepartureTime();

				// Find the stop with the given egress stop id that comes after the access stop
				// and after the vehicle departure time
				int egressStopIndex = findStopIndex(transitRoute, egressStopId, vehicleDepartureTime, accessStopIndex);
				egressStop = transitRoute.getStops().get(egressStopIndex);

				// Compute waiting time
				inVehicleTime = egressStop.getArrivalOffset().seconds() - accessStop.getDepartureOffset().seconds();
				waitingTime = totalTravelTime - inVehicleTime;

				while (waitingTime < 0.0) {
					// It may happen that the route has a loop. A good indicator for that is that
					// the waiting time is negative. In that case we can try to recover the actual
					// access stop (which must come after the one that we initially found and before
					// the egress stop).

					int updatedAccessStopIndex = updateAccessStopIndex(transitRoute, accessStopId,
							connectionDepartureTime, accessStopIndex, egressStopIndex);

					if (updatedAccessStopIndex != accessStopIndex) {
						accessStopIndex = updatedAccessStopIndex;
						accessStop = transitRoute.getStops().get(accessStopIndex);

						// Find the corresponding departure
						routeDeparture = departureFinder.findDeparture(transitRoute, accessStop,
								connectionDepartureTime);
						vehicleDepartureTime = accessStop.getDepartureOffset().seconds()
								+ routeDeparture.getDepartureTime();

						// Hopefully, now the waiting time fits, otherwise we have to do another round.
					} else {
						// We were not able to find a better access stop.
						break;
					}

					// Update in-vehicle time and waiting time
					inVehicleTime = egressStop.getArrivalOffset().seconds() - accessStop.getDepartureOffset().seconds();
					waitingTime = totalTravelTime - inVehicleTime;
				}

				// At this point waiting time may still be negative. This can happen if there is
				// another connection between the access stop id and the egress stop id on the
				// given route. This means we have to do another search round with access stops
				// that are AFTER the current egress stop.
				// If there is a bug somewhere, the functions above should not be able to find
				// another connection at some point and raise an error.

				minimumAccessStopIndex = egressStopIndex;
			} while (waitingTime < 0.0);

			return new DefaultTransitConnection(routeDeparture, accessStop, egressStop, inVehicleTime, waitingTime);
		} catch (NoDepartureFoundException e) {
			throw new NoConnectionFoundException(transitRoute, accessStopId, egressStopId, connectionDepartureTime,
					totalTravelTime);
		}
	}
}
