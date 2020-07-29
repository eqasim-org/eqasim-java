package org.eqasim.core.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class LegItem extends TripItem {
	public int legId;

	public LegItem(Id<Person> personId, int tripId, int legId, Coord origin, Coord destination,
				   double startTime, double travelTime, double networkDistance,
				   String mode, String preceedingPurpose, String followingPurpose, boolean returning,
				   double crowflyDistance) {
		super(personId, tripId, origin, destination, startTime, travelTime, networkDistance, mode,
				preceedingPurpose, followingPurpose, returning, crowflyDistance);
		this.legId = legId;
	}
}