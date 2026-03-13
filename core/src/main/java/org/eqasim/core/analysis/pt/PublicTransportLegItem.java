package org.eqasim.core.analysis.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.*;

public class PublicTransportLegItem {
	final public Id<Person> personId;
	final public int personTripId;
	final public int legIndex;

	public Id<TransitStopFacility> accessStopId;
	public Id<TransitStopFacility> egressStopId;

	public Id<TransitLine> transitLineId;
	public Id<TransitRoute> transitRouteId;

	public Id<TransitStopArea> accessAreaId;
	public Id<TransitStopArea> egressAreaId;
	public Id<Departure> departureId;

	public String transitMode;

	public double boardingTime;

	public PublicTransportLegItem(Id<Person> personId, int personTripId, int legIndex) {
		this.personId = personId;
		this.personTripId = personTripId;
		this.legIndex = legIndex;
	}
}