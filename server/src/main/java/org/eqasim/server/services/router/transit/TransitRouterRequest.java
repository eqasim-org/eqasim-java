package org.eqasim.server.services.router.transit;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TransitRouterRequest {
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

	@JsonProperty("provide_itinerary")
	public boolean provideItinerary = false;

	@JsonProperty("provide_geometry")
	public boolean provideGeometry = false;

	@JsonProperty("utilities")
	public Utilities utilities = null;

	public class Utilities {
		public Double rail_u_h = null;
		public Double subway_u_h = null;
		public Double bus_u_h = null;
		public Double tram_u_h = null;
		public Double other_u_h = null;
		public Double wait_u_h = null;
		public Double walk_u_h = null;
		public Double transfer_u = null;
	}
}
