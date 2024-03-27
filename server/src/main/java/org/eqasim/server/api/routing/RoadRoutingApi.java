package org.eqasim.server.api.routing;

import org.eqasim.server.api.AbstractApi;
import org.eqasim.server.backend.routing.RoadRouterBackend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.javalin.http.Context;

public class RoadRoutingApi extends AbstractApi {
	private final RoadRouterBackend backend;

	public RoadRoutingApi(RoadRouterBackend backend) {
		this.backend = backend;
	}

	public void postRoute(Context ctx) throws JsonMappingException, JsonProcessingException {
		Request request = readRequest(ctx, Request.class);

		Response response = new Response();
		response.requestIndex = request.requestIndex;
		response.route = backend.route(request.request, request.configuration);

		writeResponse(ctx, response);
	}

	static public class Request {
		public int requestIndex;

		public RoadRouterBackend.Request request;
		public RoadRouterBackend.Configuration configuration = new RoadRouterBackend.Configuration();
	}

	static public class Response {
		public int requestIndex;
		public RoadRouterBackend.Route route;
	}
}
