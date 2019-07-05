package org.eqasim.components.transit.connection;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public interface TransitConnectionFinder {
	TransitConnection findConnection(double departureTime, double totalTravelTime, Id<TransitStopFacility> accessStopId,
			Id<TransitStopFacility> egressStopId, TransitRoute transitRoute) throws NoConnectionFoundException;

	public static class NoConnectionFoundException extends Exception {
		private static final long serialVersionUID = -4938361475494542684L;

		public NoConnectionFoundException(TransitRoute transitRoute, Id<TransitStopFacility> accessStopId,
				Id<TransitStopFacility> egressStopId, double connectionDepartureTime, double totalTravelTime) {
			super(String.format(
					"Route: %s, Access stop: %s, Egress stop: %s, Departure time: %f, Total travel time: %f",
					transitRoute.getId().toString(), accessStopId.toString(), egressStopId.toString(),
					connectionDepartureTime, totalTravelTime));
		}

	}
}
