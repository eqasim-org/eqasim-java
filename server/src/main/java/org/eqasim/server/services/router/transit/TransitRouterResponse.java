package org.eqasim.server.services.router.transit;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TransitRouterResponse {
	@JsonProperty("request_index")
	public int requestIndex = 0;

	@JsonProperty("in_vehicle_travel_time")
	public double inVehicleTravelTime_min;

	@JsonProperty("in_vehicle_travel_time_by_mode_min")
	public Map<String, Double> inVehicleTravelTimeByMode_min = new HashMap<>();

	@JsonProperty("in_vehicle_distance_km")
	public double inVehicleDistance_km;

	@JsonProperty("in_vehicle_distance_by_mode_km")
	public Map<String, Double> inVehicleDistanceByMode_km = new HashMap<>();

	@JsonProperty("vehicle_legs_by_mode")
	public Map<String, Integer> vehicleLegsByMode = new HashMap<>();

	@JsonProperty("access_walk_time_min")
	public double accessWalkTime_min;

	@JsonProperty("egress_walk_time_min")
	public double egressWalkTime_min;

	@JsonProperty("transfer_walk_time_min")
	public double transferWalkTime_min;

	@JsonProperty("access_walk_distance_km")
	public double accessWalkDistance_km;

	@JsonProperty("egress_walk_distance_km")
	public double egressWalkDistance_km;

	@JsonProperty("transfer_walk_distance_km")
	public double transferWalkDistance_km;

	@JsonProperty("initial_wait_time_min")
	public double initialWaitTime_min;

	@JsonProperty("transfer_wait_time_min")
	public double transferWaitTime_min;

	@JsonProperty("transfers")
	public int transfers;

	@JsonProperty("total_travel_time_min")
	public double totalTravelTime_min;

	@JsonProperty("arrivalTime_s")
	public double arrivalTime_s;

	@JsonProperty("is_only_walk")
	public boolean isOnlyWalk;

	@JsonProperty("itinerary")
	@JsonInclude(Include.NON_NULL)
	public Itinerary itinerary = null;

	static public class Itinerary {
		@JsonProperty("stops")
		public List<ItineraryStop> stops = new LinkedList<>();

		static public class ItineraryStop {
			@JsonProperty("id")
			public String id;

			@JsonProperty("name")
			public String name;

			@JsonProperty("x")
			public double x;

			@JsonProperty("y")
			public double y;

			@JsonProperty("arrival_time_s")
			public double arrivalTime_s;

			@JsonProperty("departure_time_s")
			public double departureTime_s;

			@JsonProperty("wait_time_s")
			public double waitTime_min;

			@JsonProperty("geometry")
			@JsonInclude(Include.NON_NULL)
			public String geometry = null;
		}

		@JsonProperty("legs")
		public List<ItineraryLeg> legs = new LinkedList<>();

		static public class ItineraryLeg {
			@JsonProperty("type")
			public Type type;

			public enum Type {
				access, egress, transfer, vehicle
			}

			@JsonProperty("mode")
			public String mode;

			@JsonProperty("departure_time_s")
			public double departureTime_s;

			@JsonProperty("arrival_time_s")
			public double arrivalTime_s;

			@JsonProperty("travel_time_min")
			public double travelTime_min;

			@JsonProperty("distance_km")
			public double distance_km;

			@JsonProperty("in_vehicle_time_min")
			@JsonInclude(Include.NON_NULL)
			public Double inVehicleTime_min = null;

			@JsonProperty("wait_time_min")
			@JsonInclude(Include.NON_NULL)
			public Double waitTime_min = null;

			@JsonProperty("line_id")
			@JsonInclude(Include.NON_NULL)
			public String lineId = null;

			@JsonProperty("route_id")
			@JsonInclude(Include.NON_NULL)
			public String routeId = null;

			@JsonProperty("line_name")
			@JsonInclude(Include.NON_NULL)
			public String lineName = null;

			@JsonProperty("geometry")
			@JsonInclude(Include.NON_NULL)
			public String geometry = null;
		}
	}
}
