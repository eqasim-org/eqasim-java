package org.eqasim.core.components.transit.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class PublicTransitEvent extends GenericEvent implements HasPersonId {
	final public static String TYPE = "pt_transit";

	final private Id<Person> personId;
	final private Id<TransitLine> transitLineId;
	final private Id<TransitRoute> transitRouteId;
	final private Id<TransitStopFacility> accessStopId;
	final private Id<TransitStopFacility> egressStopId;
	final private double vehicleDepartureTime;
	final private double travelDistance;

	public PublicTransitEvent(double arrivalTime, Id<Person> personId, Id<TransitLine> transitLineId,
			Id<TransitRoute> transitRouteId, Id<TransitStopFacility> accessStopId, Id<TransitStopFacility> egressStopId,
			double vehicleDepartureTime, double travelDistance) {
		super(TYPE, arrivalTime);

		this.personId = personId;
		this.transitLineId = transitLineId;
		this.transitRouteId = transitRouteId;
		this.accessStopId = accessStopId;
		this.egressStopId = egressStopId;
		this.vehicleDepartureTime = vehicleDepartureTime;
		this.travelDistance = travelDistance;
	}

	public PublicTransitEvent(double now, PublicTransitEvent delegate) {
		this(now, delegate.getPersonId(), delegate.getTransitLineId(), delegate.getTransitRouteId(),
				delegate.getAccessStopId(), delegate.getEgressStopId(), delegate.getVehicleDepartureTime(),
				delegate.getTravelDistance());
	}

	public Id<TransitLine> getTransitLineId() {
		return transitLineId;
	}

	public Id<TransitRoute> getTransitRouteId() {
		return transitRouteId;
	}

	public Id<TransitStopFacility> getAccessStopId() {
		return accessStopId;
	}

	public Id<TransitStopFacility> getEgressStopId() {
		return egressStopId;
	}

	public double getVehicleDepartureTime() {
		return vehicleDepartureTime;
	}

	public double getTravelDistance() {
		return travelDistance;
	}

	@Override
	public Id<Person> getPersonId() {
		return personId;
	}

	@Override
	public String getEventType() {
		return TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();
		attributes.put("person", personId.toString());
		attributes.put("line", transitLineId.toString());
		attributes.put("route", transitRouteId.toString());
		attributes.put("accessStop", accessStopId.toString());
		attributes.put("egressStop", egressStopId.toString());
		attributes.put("vehicleDepartureTime", String.valueOf(vehicleDepartureTime));
		attributes.put("travelDistance", String.valueOf(travelDistance));
		return attributes;
	}
}
