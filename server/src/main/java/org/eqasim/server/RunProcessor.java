package org.eqasim.server;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.eqasim.core.misc.ParallelProgress;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.server.ServiceBuilder.Services;
import org.eqasim.server.services.isochrone.road.RoadIsochroneRequest;
import org.eqasim.server.services.isochrone.road.RoadIsochroneResponse;
import org.eqasim.server.services.isochrone.transit.TransitIsochroneRequest;
import org.eqasim.server.services.isochrone.transit.TransitIsochroneResponse;
import org.eqasim.server.services.router.road.FreespeedSettings;
import org.eqasim.server.services.router.road.RoadRouterRequest;
import org.eqasim.server.services.router.road.RoadRouterResponse;
import org.eqasim.server.services.router.transit.TransitRouterRequest;
import org.eqasim.server.services.router.transit.TransitRouterResponse;
import org.eqasim.server.services.router.transit.TransitUtilities;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RunProcessor {
	public static void main(String[] args)
			throws ConfigurationException, JsonParseException, JsonMappingException, IOException, InterruptedException,
			ExecutionException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "input-path", "output-path") //
				.allowOptions("threads", "configuration-path", "use-transit", "indent-response", "vdf-path",
						EqasimConfigurator.CONFIGURATOR) //
				.build();

		Services services = new ServiceBuilder().build(cmd);

		int threads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());

		ObjectMapper objectMapper = new ObjectMapper();
		if (cmd.getOption("indent-response").map(Boolean::parseBoolean).orElse(false)) {

		}

		ProcessorInput input = objectMapper.readValue(new File(cmd.getOptionStrict("input-path")),
				ProcessorInput.class);
		ProcessorOutput output = new ProcessorOutput();

		ExecutorService executor = Executors.newFixedThreadPool(threads);

		process(input.roadRouter, output.roadRouter,
				request -> services.roadRouterService().processRequest(request, input.freespeed), "road_router",
				executor);

		process(input.roadIsochrone, output.roadIsochrone, services.roadIsochroneService()::processRequest,
				"road_isochrone", executor);

		if (services.transitRouterService() != null) {
			process(input.transitRouter, output.transitRouter,
					request -> services.transitRouterService().processRequest(request, input.transitUtilities),
					"transit_router", executor);

			process(input.transitIsochrone, output.transitIsochrone, services.transitIsochroneService()::processRequest,
					"transit_isochrone", executor);
		}

		objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(cmd.getOptionStrict("output-path")), output);
	
		executor.shutdown();
	}

	private static class ProcessorInput {
		@JsonProperty("freespeed")
		FreespeedSettings freespeed = null;

		@JsonProperty("road_router")
		List<RoadRouterRequest> roadRouter = new LinkedList<>();

		@JsonProperty("road_isochrone")
		List<RoadIsochroneRequest> roadIsochrone = new LinkedList<>();

		@JsonProperty("transit_utilities")
		TransitUtilities transitUtilities = null;

		@JsonProperty("transit_router")
		List<TransitRouterRequest> transitRouter = new LinkedList<>();

		@JsonProperty("transit_isochrone")
		List<TransitIsochroneRequest> transitIsochrone = new LinkedList<>();
	}

	private static class ProcessorOutput {
		@JsonProperty("road_router")
		List<RoadRouterResponse> roadRouter = new LinkedList<>();

		@JsonProperty("road_isochrone")
		List<RoadIsochroneResponse> roadIsochrone = new LinkedList<>();

		@JsonProperty("transit_router")
		List<TransitRouterResponse> transitRouter = new LinkedList<>();

		@JsonProperty("transit_isochrone")
		List<TransitIsochroneResponse> transitIsochrone = new LinkedList<>();
	}

	private static <Response, Request> void process(List<Request> requests, List<Response> responses,
			Function<Request, Response> service, String serivceName, ExecutorService executor)
			throws InterruptedException, ExecutionException {
		if (requests.size() > 0) {
			ParallelProgress progress = new ParallelProgress("Processing " + serivceName, requests.size());

			List<Callable<Response>> tasks = new LinkedList<>();
			for (Request request : requests) {
				tasks.add(() -> {
					Response response = service.apply(request);
					progress.update();
					return response;
				});
			}

			for (var task : executor.invokeAll(tasks)) {
				responses.add(task.get());
			}

			progress.close();
		}
	}
}
