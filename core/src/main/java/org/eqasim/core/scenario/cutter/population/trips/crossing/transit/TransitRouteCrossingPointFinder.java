package org.eqasim.core.scenario.cutter.population.trips.crossing.transit;

import java.util.List;

import org.eqasim.core.components.transit.routing.EnrichedTransitRoute;

public interface TransitRouteCrossingPointFinder {
	List<TransitRouteCrossingPoint> findCrossingPoints(EnrichedTransitRoute route, double departureTime);
}