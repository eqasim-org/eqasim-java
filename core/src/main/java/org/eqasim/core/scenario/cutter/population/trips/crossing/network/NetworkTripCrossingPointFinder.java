package org.eqasim.core.scenario.cutter.population.trips.crossing.network;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

public interface NetworkTripCrossingPointFinder {

	List<NetworkTripCrossingPoint> findCrossingPoints(Id<Person> personId, int firstLegIndex, Coord startCoord,
			List<PlanElement> trip, Coord endCoord);

	boolean isInside(List<PlanElement> trip);
}
