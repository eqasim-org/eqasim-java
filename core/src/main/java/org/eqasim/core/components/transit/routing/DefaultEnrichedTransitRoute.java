package org.eqasim.core.components.transit.routing;

import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.AbstractRoute;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.routes.DefaultTransitPassengerRoute.RouteDescription;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DefaultEnrichedTransitRoute extends AbstractRoute implements EnrichedTransitRoute {
	final public static String ROUTE_TYPE = "enriched_pt";

	private EnrichedRouteDescription routeDescription = null;

	public DefaultEnrichedTransitRoute(final Id<Link> startLinkId, final Id<Link> endLinkId) {
		super(startLinkId, endLinkId);
	}

	public DefaultEnrichedTransitRoute(final Id<Link> startLinkId, final Id<Link> endLinkId, double distance,
			double inVehicleTime, double transferTime, int accessStopIndex, int egressStopIndex,
			Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId, Id<Departure> departureId,
			Id<TransitStopFacility> accessFacilityId, Id<TransitStopFacility> egressFacilityId,
			OptionalTime boardingTime) {
		super(startLinkId, endLinkId);

		setDistance(distance);
		setTravelTime(inVehicleTime + transferTime);

		this.routeDescription = new EnrichedRouteDescription();
		routeDescription.inVehicleTime = inVehicleTime;
		routeDescription.transferTime = transferTime;
		routeDescription.accessStopIndex = accessStopIndex;
		routeDescription.egressStopindex = egressStopIndex;
		routeDescription.transitLineId = transitLineId;
		routeDescription.transitRouteId = transitRouteId;
		routeDescription.departureId = departureId;
		routeDescription.accessFacilityId = accessFacilityId;
		routeDescription.egressFacilityId = egressFacilityId;
		routeDescription.boardingTime = boardingTime;
	}

	@Override
	public String getRouteType() {
		return ROUTE_TYPE;
	}

	@Override
	public String getRouteDescription() {
		try {
			return new ObjectMapper().writeValueAsString(routeDescription);
		} catch (JsonProcessingException e) {
			return "Error while creating route description: " + e.toString();
		}
	}

	@Override
	public void setRouteDescription(String routeDescription) {
		try {
			this.routeDescription = new ObjectMapper().readValue(routeDescription, EnrichedRouteDescription.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public double getInVehicleTime() {
		return routeDescription.inVehicleTime;
	}

	@Override
	public double getWaitingTime() {
		return routeDescription.transferTime;
	}

	@Override
	public Id<TransitLine> getLineId() {
		return routeDescription.transitLineId;
	}

	@Override
	public Id<TransitRoute> getRouteId() {
		return routeDescription.transitRouteId;
	}

	@Override
	public Id<Departure> getDepartureId() {
		return routeDescription.departureId;
	}

	@Override
	public int getAccessStopIndex() {
		return routeDescription.accessStopIndex;
	}

	@Override
	public int getEgressStopIndex() {
		return routeDescription.egressStopindex;
	}

	@Override
	public DefaultEnrichedTransitRoute clone() {
		return new DefaultEnrichedTransitRoute(getStartLinkId(), getEndLinkId(), getDistance(), getInVehicleTime(),
				getWaitingTime(), getAccessStopIndex(), getEgressStopIndex(), getLineId(), getRouteId(),
				getDepartureId(), getAccessStopId(), getEgressStopId(), getBoardingTime());
	}

	public static class EnrichedRouteDescription extends RouteDescription {
		public double inVehicleTime;
		public double transferTime;

		public int accessStopIndex;
		public int egressStopindex;

		public Id<Departure> departureId;

		@JsonProperty("departureId")
		public String getDepartureId() {
			return departureId.toString();
		}

		@JsonProperty("departureId")
		public void setDepartureId(String departureId) {
			this.departureId = Id.create(departureId, Departure.class);
		}
	}

	@Override
	public Id<TransitStopFacility> getAccessStopId() {
		return routeDescription.accessFacilityId;
	}

	@Override
	public Id<TransitStopFacility> getEgressStopId() {
		return routeDescription.egressFacilityId;
	}

	@Override
	public OptionalTime getBoardingTime() {
		return routeDescription.boardingTime;
	}
}
