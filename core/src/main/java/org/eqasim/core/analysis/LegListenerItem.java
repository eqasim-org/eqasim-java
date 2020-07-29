package org.eqasim.core.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.util.LinkedList;
import java.util.List;

public class LegListenerItem extends LegItem {
	public List<Id<Link>> route = new LinkedList<>();

	public LegListenerItem(Id<Person> personId, int tripId, int legId, Coord origin, double startTime,
						   String startPurpose) {
		super(personId, tripId, legId, origin, null, startTime, Double.NaN, Double.NaN,
				"unknown", startPurpose, "unknown", false, Double.NaN);
	}
}
