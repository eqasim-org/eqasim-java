package org.eqasim.server.rewrite.services.isochrone.road;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RoadIsochroneRequest {
	@JsonProperty("request_index")
	public int requestIndex = 0;

	@JsonProperty("origin_x")
	public double originX;

	@JsonProperty("origin_y")
	public double originY;

	@JsonProperty("departure_time_s")
	public double departureTime_s;

	@JsonProperty("maximum_travel_time_min")
	public double maximumTravelTime_min;

	@JsonProperty("origin_radius_km")
	public Double originRadius_km = null;

	@JsonProperty("segment_length_km")
	public Double segmentLength_km = null;

	@JsonProperty("osm_restrictions")
	public Set<String> osmRestrictions = null;

	@JsonProperty("provide_nodes")
	public boolean provideNodes = false;

	@JsonProperty("provide_geometry")
	public boolean provideGeometry = false;

	@JsonProperty("consider_access")
	public boolean considerAccess = false;
}
