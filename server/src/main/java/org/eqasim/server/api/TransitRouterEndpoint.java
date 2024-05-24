package org.eqasim.server.api;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.eqasim.server.services.router.transit.TransitRouterRequest;
import org.eqasim.server.services.router.transit.TransitRouterResponse;
import org.eqasim.server.services.router.transit.TransitRouterService;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.javalin.http.Context;

public class TransitRouterEndpoint extends AbstractEndpoint {
	private final ExecutorService executor;
	private final TransitRouterService service;

	public TransitRouterEndpoint(ExecutorService executor, TransitRouterService service) {
		this.executor = executor;
		this.service = service;
	}

	private Collection<TransitRouterResponse> process(List<TransitRouterRequest> requests)
			throws InterruptedException, ExecutionException {
		List<Callable<TransitRouterResponse>> tasks = new LinkedList<>();
		for (TransitRouterRequest request : requests) {
			tasks.add(() -> service.processRequest(request));
		}

		List<TransitRouterResponse> response = new LinkedList<>();
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
		public TransitRouterRequest request = null;
		public List<TransitRouterRequest> batch = new LinkedList<>();
	}
}
