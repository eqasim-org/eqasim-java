package org.eqasim.examples.zurich_adpt.scenario;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.AbstractRoute;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class AdPTRoute extends AbstractRoute {
	final static String AdPT_ROUTE = "adpt";

	private double inVehicleTime;
	private double inVehicleDistance;
	private String originZone;
	private String destinationZone;

	public AdPTRoute(Id<Link> startLinkId, Id<Link> endLinkId) {
		super(startLinkId, endLinkId);
	}

	private void interpretAttributes(Map<String, Object> attributes) {
		String startZoneId = (String) attributes.get("startZone");
		String endZoneId = (String) attributes.get("endZone");
		Double inVehicleTime = (Double) attributes.get("inVehicleTime");

		this.originZone = startZoneId;
		this.destinationZone = endZoneId;

		this.inVehicleTime = inVehicleTime;
	}

	private Map<String, Object> buildAttributes() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("startZone", this.originZone);
		attributes.put("endZone", this.destinationZone);
		attributes.put("inVehicleTime", inVehicleTime);

		return attributes;
	}

	private final ObjectMapper mapper = new ObjectMapper();
	private final MapType mapType = TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class,
			Object.class);

	@Override
	public String getRouteDescription() {
		try {
			return new ObjectMapper().writeValueAsString(buildAttributes());
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setRouteDescription(String routeDescription) {
		try {
			Map<String, Object> attributes = mapper.readValue(routeDescription, mapType);
			interpretAttributes(attributes);
		} catch (IOException e) {
			new RuntimeException(e);
		}
	}

	public double getInVehicleTime() {
		return inVehicleTime;
	}

	public void setInVehicleTime(double inVehicleTime) {
		this.inVehicleTime = inVehicleTime;
	}

	public double getInVehicleDistance() {
		return inVehicleDistance;
	}

	public void setInVehicleDistance(double inVehicleDistance) {
		this.inVehicleDistance = inVehicleDistance;
	}

	@Override
	public String getRouteType() {
		return this.AdPT_ROUTE;
	}

	public String getOriginZone() {
		return originZone;
	}

	public void setOriginZone(String originZone) {
		this.originZone = originZone;
	}

	public String getDestinationZone() {
		return destinationZone;
	}

	public void setDestinationZone(String destinationZone) {
		this.destinationZone = destinationZone;
	}

}
