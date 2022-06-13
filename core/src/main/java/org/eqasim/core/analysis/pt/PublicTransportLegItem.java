package org.eqasim.core.analysis.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class PublicTransportLegItem {
	public Id<Person> personId;
	public int personTripId;
	public int legIndex;

	public Id<TransitStopFacility> accessStopId;
	public Id<TransitStopFacility> egressStopId;

	public Id<TransitLine> transitLineId;
	public Id<TransitRoute> transitRouteId;

	public PublicTransportLegItem(Id<Person> personId, int personTripId, int legIndex,
			Id<TransitStopFacility> accessStopId, Id<TransitStopFacility> egressStopId, Id<TransitLine> transitLineId,
			Id<TransitRoute> transitRouteId) {
		this.personId = personId;
		this.personTripId = personTripId;
		this.legIndex = legIndex;

		this.accessStopId = accessStopId;
		this.egressStopId = egressStopId;

		this.transitLineId = transitLineId;
		this.transitRouteId = transitRouteId;
	}
}