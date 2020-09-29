package org.eqasim.core.components.transit.routing;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;

public interface EnrichedTransitRoute extends TransitPassengerRoute {
	double getInVehicleTime();

	double getWaitingTime();

	Id<Departure> getDepartureId();

	int getAccessStopIndex();

	int getEgressStopIndex();
}
