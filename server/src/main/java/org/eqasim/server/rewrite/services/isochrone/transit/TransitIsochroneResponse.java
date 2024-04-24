package org.eqasim.server.rewrite.services.isochrone.transit;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TransitIsochroneResponse {
	@JsonProperty("request_index")
	public int requestIndex = 0;

	@JsonProperty("stops")
	public List<Stop> stops = new LinkedList<>();

	static public class Stop {
		@JsonProperty("access_stop_id")
		@JsonInclude(Include.NON_NULL)
		public String accessStopId;

		@JsonProperty("egress_stop_id")
		@JsonInclude(Include.NON_NULL)
		public String egressStopId;

		@JsonProperty("access_time_min")
		@JsonInclude(Include.NON_NULL)
		public Double acessTime_min;

		@JsonProperty("transfer_time_min")
		public double transferTime_min;

		@JsonProperty("wait_time_min")
		public double waitTime_min;

		@JsonProperty("in_vehicle_time_min")
		public double inVehicleTime_min;

		@JsonProperty("total_travel_time_min")
		public double totalTravelTime_min;
		
		@JsonProperty("transfers")
		public int transfers;

		@JsonProperty("x")
		public double x;

		@JsonProperty("y")
		public double y;

		@JsonProperty("arrival_time_s")
		public double arrivalTime_s;

		@JsonProperty("is_origin")
		public boolean isOrigin;

		@JsonProperty("geometry")
		@JsonInclude(Include.NON_NULL)
		public String geometry;
	}
}
