package org.eqasim.server.rewrite.api;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.eqasim.server.rewrite.services.router.road.RoadRouterRequest;
import org.eqasim.server.rewrite.services.router.road.RoadRouterResponse;
import org.eqasim.server.rewrite.services.router.road.RoadRouterService;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.javalin.http.Context;

public class RoadRouterEndpoint extends AbstractEndpoint {
	private final ExecutorService executor;
	private final RoadRouterService service;

	public RoadRouterEndpoint(ExecutorService executor, RoadRouterService service) {
		this.executor = executor;
		this.service = service;
	}

	private Collection<RoadRouterResponse> process(List<RoadRouterRequest> requests)
			throws InterruptedException, ExecutionException {
		List<Callable<RoadRouterResponse>> tasks = new LinkedList<>();
		for (RoadRouterRequest request : requests) {
			tasks.add(() -> service.processRequest(request));
		}

		List<RoadRouterResponse> response = new LinkedList<>();
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
		public RoadRouterRequest request = null;
		public List<RoadRouterRequest> batch = new LinkedList<>();
	}
}
