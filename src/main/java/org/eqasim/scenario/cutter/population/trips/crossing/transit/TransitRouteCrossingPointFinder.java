package org.eqasim.scenario.cutter.population.trips.crossing.transit;

import java.util.List;

import org.eqasim.components.transit.routing.EnrichedTransitRoute;

public interface TransitRouteCrossingPointFinder {
	List<TransitRouteCrossingPoint> findCrossingPoints(EnrichedTransitRoute route, double departureTime);
}