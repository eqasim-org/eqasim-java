package org.eqasim.server.api.grid;

import java.util.List;

import org.eqasim.server.api.AbstractApi;
import org.eqasim.server.backend.grid.ModalGridBackend;
import org.eqasim.server.backend.grid.ModalGridBackend.ModalCell;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.javalin.http.Context;

public class ModalGridApi extends AbstractApi {
	private final ModalGridBackend modalGridBuilder;

	public ModalGridApi(ModalGridBackend modalGridBuilder) {
		this.modalGridBuilder = modalGridBuilder;
	}

	public void postModalGrid(Context ctx) throws JsonProcessingException {
		ModalGridRequest request = readRequest(ctx, ModalGridRequest.class);

		ModalGridResponse response = new ModalGridResponse();
		response.requestIndex = request.requestIndex;
		response.grid = modalGridBuilder.build(request.originX, request.originY, request.departureTime);

		writeResponse(ctx, response);
	}

	static public class ModalGridRequest {
		public int requestIndex;

		public double originX;
		public double originY;

		public double departureTime;
	}

	static public class ModalGridResponse {
		public int requestIndex;
		public List<ModalCell> grid;
	}
}
