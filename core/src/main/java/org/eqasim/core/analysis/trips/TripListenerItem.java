package org.eqasim.core.analysis.trips;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class TripListenerItem extends TripItem {
	public double lastAddedLinkDistance = 0.0;

	public TripListenerItem(Id<Person> personId, int personTripId, Coord origin, double startTime,
			String startPurpose) {
		super(personId, personTripId, origin, null, startTime, Double.NaN, 0.0, 0.0, "unknown", startPurpose, "unknown",
				false, Double.NaN);
	}
}
