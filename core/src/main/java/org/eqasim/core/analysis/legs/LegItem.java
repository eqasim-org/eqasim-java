package org.eqasim.core.analysis.legs;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class LegItem {
	public Id<Person> personId;
	public int personTripId;
	public int legIndex;
	public Coord origin;
	public Coord destination;
	public double departureTime;
	public double travelTime;
	public double vehicleDistance;
	public double routedDistance;
	public String mode;
	public double euclideanDistance;

	public LegItem(Id<Person> personId, int personTripId, int legIndex, Coord origin, Coord destination,
			double startTime, double travelTime, double vehicleDistance, double routedDistance, String mode,
			double euclideanDistance) {
		this.personId = personId;
		this.personTripId = personTripId;
		this.legIndex = legIndex;
		this.origin = origin;
		this.destination = destination;
		this.departureTime = startTime;
		this.travelTime = travelTime;
		this.vehicleDistance = vehicleDistance;
		this.routedDistance = routedDistance;
		this.mode = mode;
		this.euclideanDistance = euclideanDistance;
	}
}