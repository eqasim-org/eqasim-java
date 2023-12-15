package org.eqasim.core.scenario.cutter.population.trips.crossing.network;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.routes.NetworkRoute;

public interface NetworkRouteCrossingPointFinder {
	List<NetworkRouteCrossingPoint> findCrossingPoints(Id<Person> personId, int legIndex, String mode, NetworkRoute route,
			double departureTime);

	boolean isInside(NetworkRoute route);
}