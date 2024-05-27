package org.eqasim.server.services.router.road;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RoadRouterRequest {
	@JsonProperty("request_index")
	public int requestIndex = 0;

	@JsonProperty("origin_x")
	public double originX;

	@JsonProperty("origin_y")
	public double originY;

	@JsonProperty("destination_x")
	public double destinationX;

	@JsonProperty("destination_y")
	public double destinationY;

	@JsonProperty("departure_time_s")
	public double departureTime_s;

	@JsonProperty("provide_links")
	public boolean provideLinks = false;

	@JsonProperty("provide_geometry")
	public boolean provideGeometry = false;

	@JsonProperty("access_egress_radius_km")
	public Double accessEgressRadius_km = null;

	@JsonProperty("freespeed")
	public FreespeedSettings freespeed = null;
}
