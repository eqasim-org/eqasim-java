package org.eqasim.server.api;

import java.util.LinkedList;
import java.util.List;

import org.eqasim.server.backend.BackendScenario;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.javalin.http.Context;

public class TransitNetworkApi extends AbstractApi {
	private final TransitSchedule schedule;

	public TransitNetworkApi(BackendScenario scenario) {
		this.schedule = scenario.getSchedule();
	}

	public void postStops(Context ctx) throws JsonMappingException, JsonProcessingException {
		StopRequest request = readRequest(ctx, StopRequest.class);
		StopResponse response = new StopResponse();

		IdSet<TransitStopFacility> facilityIds = new IdSet<>(TransitStopFacility.class);
		for (TransitLine transitLine : schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				if (request.modes.size() == 0 || request.modes.contains(transitRoute.getTransportMode())) {
					transitRoute.getStops().forEach(stop -> facilityIds.add(stop.getStopFacility().getId()));
				}
			}
		}

		for (Id<TransitStopFacility> facilityId : facilityIds) {
			TransitStopFacility facility = schedule.getFacilities().get(facilityId);

			StopData stopData = new StopData();

			stopData.x = facility.getCoord().getX();
			stopData.y = facility.getCoord().getY();

			stopData.stopId = facility.getId().toString();
			stopData.areaId = facility.getStopAreaId().toString();
			stopData.name = facility.getName();

			response.stops.add(stopData);
		}

		writeResponse(ctx, response);
	}

	static public class StopRequest {
		public int requestIndex;
		public List<String> modes = new LinkedList<>();
	}

	static public class StopResponse {
		public int requestIndex;
		public List<StopData> stops = new LinkedList<>();
	}

	static public class StopData {
		public double x;
		public double y;

		public String stopId;
		public String areaId;
		public String name;
	}
}
