package org.eqasim.server.rewrite.services.isochrone.transit;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TransitIsochroneRequest {
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

	@JsonProperty("maximum_travel_time_min")
	public Double maximumTravelTime_min = null;

	@JsonProperty("maximum_transfers")
	public Integer maximumTransfers = null;

	@JsonProperty("allowed_modes")
	public List<String> allowedModes = null;

	@JsonProperty("restricted_modes")
	public List<String> restrictedModes = null;

	@JsonProperty("maximum_access_distance_km")
	public double maximumAccessDistance_km = 0.8;

	@JsonProperty("maximum_transfer_distance_km")
	public double maximumTransferDistance_km = 0.4;

	@JsonProperty("consider_access")
	public boolean considerAccess = false;

	@JsonProperty("provide_stops")
	public boolean provideStops = false;

	@JsonProperty("provide_geometry")
	public boolean provideGeometry = false;
}
