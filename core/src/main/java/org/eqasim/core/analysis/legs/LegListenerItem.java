package org.eqasim.core.analysis.legs;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class LegListenerItem extends LegItem {
	public double lastAddedLinkDistance = 0.0;

	public LegListenerItem(Id<Person> personId, int personTripId, int legIndex, Coord origin) {
		super(personId, personTripId, legIndex, origin, null, Double.NaN, Double.NaN, 0.0, 0.0, "unknown", Double.NaN);
	}
}
