package org.eqasim.server.api.routing;

import org.eqasim.server.api.AbstractApi;
import org.eqasim.server.backend.routing.TransitRouterBackend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.javalin.http.Context;

public class TransitRoutingApi extends AbstractApi {
	private final TransitRouterBackend backend;

	public TransitRoutingApi(TransitRouterBackend backend) {
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

		public TransitRouterBackend.Request request;
		public TransitRouterBackend.Configuration configuration = new TransitRouterBackend.Configuration();
	}

	static public class Response {
		public int requestIndex;
		public TransitRouterBackend.Route route;
	}
}
