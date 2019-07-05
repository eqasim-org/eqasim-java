package org.eqasim.components.transit.routing;

import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.AbstractRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DefaultEnrichedTransitRoute extends AbstractRoute implements EnrichedTransitRoute {
	final public static String ROUTE_TYPE = "enriched_pt";

	private RouteDescription routeDescription = null;

	public DefaultEnrichedTransitRoute(final Id<Link> startLinkId, final Id<Link> endLinkId) {
		super(startLinkId, endLinkId);
	}

	public DefaultEnrichedTransitRoute(final Id<Link> startLinkId, final Id<Link> endLinkId, double distance,
			double inVehicleTime, double transferTime, int accessStopIndex, int egressStopIndex,
			Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId, Id<Departure> departureId) {
		super(startLinkId, endLinkId);

		setDistance(distance);
		setTravelTime(inVehicleTime + transferTime);

		this.routeDescription = new RouteDescription();
		routeDescription.inVehicleTime = inVehicleTime;
		routeDescription.transferTime = transferTime;
		routeDescription.accessStopIndex = accessStopIndex;
		routeDescription.egressStopindex = egressStopIndex;
		routeDescription.transitLineId = transitLineId;
		routeDescription.transitRouteId = transitRouteId;
		routeDescription.departureId = departureId;
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
			this.routeDescription = new ObjectMapper().readValue(routeDescription, RouteDescription.class);
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
	public Id<TransitLine> getTransitLineId() {
		return routeDescription.transitLineId;
	}

	@Override
	public Id<TransitRoute> getTransitRouteId() {
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
				getWaitingTime(), getAccessStopIndex(), getEgressStopIndex(), getTransitLineId(), getTransitRouteId(),
				getDepartureId());
	}

	public static class RouteDescription {
		public double inVehicleTime;
		public double transferTime;

		public int accessStopIndex;
		public int egressStopindex;

		public Id<TransitLine> transitLineId;
		public Id<TransitRoute> transitRouteId;
		public Id<Departure> departureId;

		@JsonProperty("transitLineId")
		public String getTransitLineId() {
			return transitLineId.toString();
		}

		@JsonProperty("transitRouteId")
		public String getRouteLineId() {
			return transitRouteId.toString();
		}

		@JsonProperty("departureId")
		public String getDepartureId() {
			return departureId.toString();
		}

		@JsonProperty("transitLineId")
		public void setTransitLineId(String transitLineId) {
			this.transitLineId = Id.create(transitLineId, TransitLine.class);
		}

		@JsonProperty("transitRouteId")
		public void setRouteLineId(String transitRouteId) {
			this.transitRouteId = Id.create(transitRouteId, TransitRoute.class);
		}

		@JsonProperty("departureId")
		public void setDepartureId(String departureId) {
			this.departureId = Id.create(departureId, Departure.class);
		}
	}
}
