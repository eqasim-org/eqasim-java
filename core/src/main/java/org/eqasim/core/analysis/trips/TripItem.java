package org.eqasim.core.analysis.trips;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class TripItem {
	public Id<Person> personId;
	public int personTripId;
	public Coord origin;
	public Coord destination;
	public double departureTime;
	public double travelTime;
	public double vehicleDistance;
	public double routedDistance;
	public String mode;
	public String followingPurpose;
	public String precedingPurpose;
	public boolean returning;
	public double euclideanDistance;

	public String originScope = "";
	public String destinationScope = "";
	public String tripScope = "";

	public TripItem(Id<Person> personId, int personTripId, Coord origin, Coord destination, double startTime,
			double travelTime, double vehicleDistance, double routedDistance, String mode, String precedingPurpose,
			String followingPurpose, boolean returning, double euclideanDistance, String originScope, String destinationScope, String tripScope) {
		this.personId = personId;
		this.personTripId = personTripId;
		this.origin = origin;
		this.destination = destination;
		this.departureTime = startTime;
		this.travelTime = travelTime;
		this.vehicleDistance = vehicleDistance;
		this.routedDistance = routedDistance;
		this.mode = mode;
		this.followingPurpose = followingPurpose;
		this.precedingPurpose = precedingPurpose;
		this.returning = returning;
		this.euclideanDistance = euclideanDistance;
		this.originScope = originScope;
		this.destinationScope = destinationScope;
		this.tripScope = tripScope;
	}
}