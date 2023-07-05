package org.eqasim.server.api.grid;

import java.util.List;

import org.eqasim.server.api.AbstractApi;
import org.eqasim.server.backend.grid.TransitGridBackend;
import org.eqasim.server.backend.grid.TransitGridBackend.TransitCell;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.javalin.http.Context;

public class TransitGridApi extends AbstractApi {
	private final TransitGridBackend transitGridBuilder;

	public TransitGridApi(TransitGridBackend transitGridBuilder) {
		this.transitGridBuilder = transitGridBuilder;
	}

	public void postTransitGrid(Context ctx) throws JsonProcessingException {
		TransitGridRequest request = readRequest(ctx, TransitGridRequest.class);

		TransitGridResponse response = new TransitGridResponse();
		response.requestIndex = request.requestIndex;
		response.grid = transitGridBuilder.build(request.originX, request.originY, request.departureTime);

		writeResponse(ctx, response);
	}

	static public class TransitGridRequest {
		public int requestIndex;

		public double originX;
		public double originY;

		public double departureTime;
	}

	static public class TransitGridResponse {
		public int requestIndex;
		public List<TransitCell> grid;
	}
}
