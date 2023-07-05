package org.eqasim.server.api;

import org.eqasim.server.backend.network.NetworkData;
import org.eqasim.server.backend.network.RoadNetworkBackend;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.javalin.http.Context;

public class RoadNetworkApi extends AbstractApi {
	private final RoadNetworkBackend roadNetworkBuilder;

	public RoadNetworkApi(RoadNetworkBackend roadNetworkBuilder) {
		this.roadNetworkBuilder = roadNetworkBuilder;
	}

	public void postNetwork(Context ctx) throws JsonProcessingException {
		RoadNetworkRequest request = readRequest(ctx, RoadNetworkRequest.class);

		RoadNetworkResponse response = new RoadNetworkResponse();
		response.requestIndex = request.requestIndex;
		response.network = roadNetworkBuilder.build();

		writeResponse(ctx, response);
	}

	static public class RoadNetworkRequest {
		public int requestIndex;
	}

	static public class RoadNetworkResponse {
		public int requestIndex;
		public NetworkData network;
	}
}
