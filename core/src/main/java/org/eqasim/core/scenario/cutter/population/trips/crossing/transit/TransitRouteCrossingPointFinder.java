package org.eqasim.core.scenario.cutter.population.trips.crossing.transit;

import java.util.List;

import org.matsim.pt.routes.TransitPassengerRoute;

public interface TransitRouteCrossingPointFinder {
	List<TransitRouteCrossingPoint> findCrossingPoints(TransitPassengerRoute route, double departureTime);
}