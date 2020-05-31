package org.eqasim.core.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class TripItem {
	public Id<Person> personId;
	public int personTripId;
	public Coord origin;
	public Coord destination;
	public double startTime;
	public double travelTime;
	public double vehicleDistance;
	public double routedDistance;
	public String mode;
	public String followingPurpose;
	public String preceedingPurpose;
	public boolean returning;
	public double crowflyDistance;

	public TripItem(Id<Person> personId, int personTripId, Coord origin, Coord destination, double startTime,
			double travelTime, double networkDistance, double routedDistance, String mode, String preceedingPurpose,
			String followingPurpose, boolean returning, double crowflyDistance) {
		this.personId = personId;
		this.personTripId = personTripId;
		this.origin = origin;
		this.destination = destination;
		this.startTime = startTime;
		this.travelTime = travelTime;
		this.vehicleDistance = networkDistance;
		this.routedDistance = routedDistance;
		this.mode = mode;
		this.followingPurpose = followingPurpose;
		this.preceedingPurpose = preceedingPurpose;
		this.returning = returning;
		this.crowflyDistance = crowflyDistance;
	}
}