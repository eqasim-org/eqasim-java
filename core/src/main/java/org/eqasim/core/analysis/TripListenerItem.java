package org.eqasim.core.analysis;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

public class TripListenerItem extends TripItem {
	public List<PlanElement> elements = new LinkedList<>();
	public List<Id<Link>> route = new LinkedList<>();

	public TripListenerItem(Id<Person> personId, int personTripId, Coord origin, double startTime,
			String startPurpose) {
		super(personId, personTripId, origin, null, startTime, Double.NaN, Double.NaN, "unknown", startPurpose,
				"unknown", false, Double.NaN);
	}
}
