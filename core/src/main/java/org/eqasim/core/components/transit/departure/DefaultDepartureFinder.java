package org.eqasim.core.components.transit.departure;

import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Singleton;

/**
 * TODO: this is sorting all the time. We can make it efficient by creating a
 * sorted list of deaprtures (or departure times per route)
 */
@Singleton
public class DefaultDepartureFinder implements DepartureFinder {
	@Override
	public StopDeparture findNextDeparture(TransitRoute route, Id<TransitStopFacility> accessStopId,
			double departureTime) throws NoDepartureFoundException {
		List<TransitRouteStop> stopCandidates = route.getStops().stream()
				.filter(s -> s.getStopFacility().getId().equals(accessStopId)).collect(Collectors.toList());

		if (stopCandidates.size() == 0) {
			throw new NoDepartureFoundException();
		}

		StopDeparture result = null;

		for (Departure departure : route.getDepartures().values()) {
			for (TransitRouteStop stopCandidate : stopCandidates) {
				double stopOffset = stopCandidate.getDepartureOffset().seconds();
				double candidateWaitingTime = (departure.getDepartureTime() + stopOffset) - departureTime;

				if (candidateWaitingTime >= 0.0) {
					if (result == null || (result.waitingTime > candidateWaitingTime)) {
						result = new StopDeparture(departure, stopCandidate, candidateWaitingTime);
						break;
					}
				}
			}
		}

		if (result == null) {
			throw new NoDepartureFoundException();
		}

		return result;
	}
}
