package org.eqasim.core.components.transit.departure;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * For a given original departure time at the access stop, find the
 * corresponding Departure in the schedule
 */
public interface DepartureFinder {
	StopDeparture findNextDeparture(TransitRoute route, Id<TransitStopFacility> accessStopId, Id<TransitStopFacility> egressStopId, double departureTime)
			throws NoDepartureFoundException;

	static public class NoDepartureFoundException extends Exception {
		private static final long serialVersionUID = -7437914556322222223L;
	}

	static public class StopDeparture {
		public Departure departure;
		public TransitRouteStop stop;
		public double waitingTime;

		public StopDeparture(Departure departure, TransitRouteStop stop, double waitingTime) {
			this.departure = departure;
			this.stop = stop;
			this.waitingTime = waitingTime;
		}
	}
}
