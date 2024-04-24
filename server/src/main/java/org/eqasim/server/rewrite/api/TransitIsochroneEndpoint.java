package org.eqasim.server.rewrite.api;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.eqasim.server.rewrite.services.isochrone.transit.TransitIsochroneRequest;
import org.eqasim.server.rewrite.services.isochrone.transit.TransitIsochroneResponse;
import org.eqasim.server.rewrite.services.isochrone.transit.TransitIsochroneService;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.javalin.http.Context;

public class TransitIsochroneEndpoint extends AbstractEndpoint {
	private final ExecutorService executor;
	private final TransitIsochroneService service;

	public TransitIsochroneEndpoint(ExecutorService executor, TransitIsochroneService service) {
		this.executor = executor;
		this.service = service;
	}

	private Collection<TransitIsochroneResponse> process(List<TransitIsochroneRequest> requests)
			throws InterruptedException, ExecutionException {
		List<Callable<TransitIsochroneResponse>> tasks = new LinkedList<>();
		for (TransitIsochroneRequest request : requests) {
			tasks.add(() -> service.processRequest(request));
		}

		List<TransitIsochroneResponse> response = new LinkedList<>();
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
		public TransitIsochroneRequest request = null;
		public List<TransitIsochroneRequest> batch = new LinkedList<>();
	}
}
