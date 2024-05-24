package org.eqasim.server.api;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.eqasim.server.services.isochrone.road.RoadIsochroneRequest;
import org.eqasim.server.services.isochrone.road.RoadIsochroneResponse;
import org.eqasim.server.services.isochrone.road.RoadIsochroneService;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.javalin.http.Context;

public class RoadIsochroneEndpoint extends AbstractEndpoint {
	private final ExecutorService executor;
	private final RoadIsochroneService service;

	public RoadIsochroneEndpoint(ExecutorService executor, RoadIsochroneService service) {
		this.executor = executor;
		this.service = service;
	}

	private Collection<RoadIsochroneResponse> process(List<RoadIsochroneRequest> requests)
			throws InterruptedException, ExecutionException {
		List<Callable<RoadIsochroneResponse>> tasks = new LinkedList<>();
		for (RoadIsochroneRequest request : requests) {
			tasks.add(() -> service.processRequest(request));
		}

		List<RoadIsochroneResponse> response = new LinkedList<>();
		for (var task : executor.invokeAll(tasks)) {
			response.add(task.get());
		}

		return response;
	}

	public void post(Context ctx) throws JsonProcessingException, InterruptedException, ExecutionException {
		Request request = readRequest(ctx, Request.class);

		if (request.request != null) {
			writeResponse(ctx, process(Collections.singletonList(request.request)).iterator().next());
		} else {
			writeResponse(ctx, process(request.batch));
		}
	}

	static public class Request {
		public RoadIsochroneRequest request = null;
		public List<RoadIsochroneRequest> batch = new LinkedList<>();
	}
}
