package org.eqasim.components.transit.routing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Route;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

public interface EnrichedTransitRoute extends Route {
	double getInVehicleTime();

	double getWaitingTime();

	Id<TransitLine> getTransitLineId();

	Id<TransitRoute> getTransitRouteId();

	Id<Departure> getDepartureId();

	int getAccessStopIndex();

	int getEgressStopIndex();
}
