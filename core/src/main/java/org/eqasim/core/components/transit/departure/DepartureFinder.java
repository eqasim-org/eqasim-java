package org.eqasim.core.components.transit.departure;

import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

/**
 * For a given original departure time at the access stop, find the
 * corresponding Departure in the schedule
 */
public interface DepartureFinder {
	Departure findDeparture(TransitRoute route, TransitRouteStop accessStop, double departureTime)
			throws NoDepartureFoundException;

	static public class NoDepartureFoundException extends Exception {
		private static final long serialVersionUID = -7437914556322222223L;
	}
}
