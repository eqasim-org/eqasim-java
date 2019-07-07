package org.eqasim.core.scenario.cutter.population.trips.crossing.teleportation;

import java.util.List;

import org.matsim.api.core.v01.Coord;

public interface TeleportationCrossingPointFinder {
	List<TeleportationCrossingPoint> findCrossingPoints(Coord originCoord, Coord destinationCoord,
			double originalTravelTime, double departureTime);
}