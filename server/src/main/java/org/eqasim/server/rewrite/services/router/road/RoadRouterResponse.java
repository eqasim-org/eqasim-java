package org.eqasim.server.rewrite.services.router.road;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RoadRouterResponse {
	@JsonProperty("request_index")
	public int requestIndex = 0;

	@JsonProperty("in_vehicle_distance_km")
	public double inVehicleDistance_km;

	@JsonProperty("in_vehicle_time_min")
	public double inVehicleTime_min;

	@JsonProperty("access_time_min")
	public double accessTime_min;

	@JsonProperty("egress_time_min")
	public double egressTime_min;

	@JsonProperty("access_distance_km")
	public double accessDistance_km;

	@JsonProperty("egress_distance_km")
	public double egressDistance_km;

	@JsonProperty("arrivalTime_s")
	public double arrivalTime_s;

	@JsonProperty("total_travel_time_min")
	public double totalTravelTime_min;

	@JsonProperty("origin_link_id")
	@JsonInclude(Include.NON_NULL)
	public String originId = null;

	@JsonProperty("destination_link_id")
	@JsonInclude(Include.NON_NULL)
	public String destinationId = null;

	@JsonProperty("links")
	@JsonInclude(Include.NON_NULL)
	public List<LinkRecord> links = null;

	static public class LinkRecord {
		@JsonProperty("id")
		public String id;

		@JsonProperty("enter_time_s")
		public double enterTime_s;

		@JsonProperty("exit_time_s")
		public double exitTime_s;
	}

	@JsonProperty("access_x")
	@JsonInclude(Include.NON_NULL)
	public Double accessX = null;

	@JsonProperty("access_y")
	@JsonInclude(Include.NON_NULL)
	public Double accessY = null;

	@JsonProperty("egress_x")
	@JsonInclude(Include.NON_NULL)
	public Double egressX = null;

	@JsonProperty("egress_y")
	@JsonInclude(Include.NON_NULL)
	public Double egressY = null;

	@JsonProperty("road_geometry")
	@JsonInclude(Include.NON_NULL)
	public String roadGeometry = null;

	@JsonProperty("access_geometry")
	@JsonInclude(Include.NON_NULL)
	public String accessGeometry = null;

	@JsonProperty("egress_geometry")
	@JsonInclude(Include.NON_NULL)
	public String egressGeometry = null;
}
