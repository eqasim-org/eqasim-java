package org.eqasim.server.api.grid;

import java.util.List;

import org.eqasim.server.api.AbstractApi;
import org.eqasim.server.backend.grid.RoadGridBackend;
import org.eqasim.server.backend.grid.RoadGridBackend.RoadCell;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.javalin.http.Context;

public class RoadGridApi extends AbstractApi {
	private final RoadGridBackend roadGridBuilder;

	public RoadGridApi(RoadGridBackend roadGridBuilder) {
		this.roadGridBuilder = roadGridBuilder;
	}

	public void postRoadGrid(Context ctx) throws JsonProcessingException {
		RoadGridRequest request = readRequest(ctx, RoadGridRequest.class);

		RoadGridResponse response = new RoadGridResponse();
		response.requestIndex = request.requestIndex;
		response.grid = roadGridBuilder.build(request.originX, request.originY, request.departureTime);

		writeResponse(ctx, response);
	}

	static public class RoadGridRequest {
		public int requestIndex;

		public double originX;
		public double originY;

		public double departureTime;
	}

	static public class RoadGridResponse {
		public int requestIndex;
		public List<RoadCell> grid;
	}
}
