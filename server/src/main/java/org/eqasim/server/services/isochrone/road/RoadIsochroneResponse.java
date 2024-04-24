package org.eqasim.server.services.isochrone.road;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RoadIsochroneResponse {
	@JsonProperty("request_index")
	public int requestIndex = 0;

	@JsonProperty("points")
	public List<Point> points = new LinkedList<>();

	static public class Point {
		@JsonProperty("x")
		public double x;

		@JsonProperty("y")
		public double y;

		@JsonProperty("in_vehicle_time_min")
		public double inVehicleTime_min;

		@JsonProperty("access_time_min")
		@JsonInclude(Include.NON_NULL)
		public Double accessTime_min = null;

		@JsonProperty("total_travel_time_min")
		public double totalTravelTime_min;

		@JsonProperty("arrival_time_s")
		public double arrivalTime_s;

		@JsonProperty("is_origin")
		public boolean isOrigin;

		@JsonProperty("is_restricted")
		@JsonInclude(Include.NON_NULL)
		public Boolean isRestricted = null;

		@JsonProperty("node_id")
		@JsonInclude(Include.NON_NULL)
		public String nodeId = null;

		@JsonProperty("geometry")
		@JsonInclude(Include.NON_NULL)
		public String geometry = null;
	}
}
