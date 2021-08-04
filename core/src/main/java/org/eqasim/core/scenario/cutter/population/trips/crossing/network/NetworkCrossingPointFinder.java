package org.eqasim.core.scenario.cutter.population.trips.crossing.network;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.routes.NetworkRoute;

public interface NetworkCrossingPointFinder {
	List<NetworkCrossingPoint> findCrossingPoints(Person person, int tripIndex, String mode, NetworkRoute route, double departureTime);

	boolean isInside(NetworkRoute route);
}