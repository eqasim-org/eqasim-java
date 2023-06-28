package org.eqasim.examples;

import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.components.headway.HeadwayCalculator;
import org.eqasim.core.components.headway.HeadwayImputerModule;
import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.tools.routing.BatchPublicTransportRouter;
import org.eqasim.core.tools.routing.BatchPublicTransportRouter.LegInformation;
import org.eqasim.core.tools.routing.BatchPublicTransportRouter.TransitRoutingTask;
import org.eqasim.core.tools.routing.BatchPublicTransportRouter.TripInformation;
import org.eqasim.core.tools.routing.BatchRoadRouter;
import org.eqasim.core.tools.routing.BatchRoadRouter.RoadRoutingResult;
import org.eqasim.core.tools.routing.BatchRoadRouter.RoadRoutingTask;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.MatsimVehicleReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import com.google.inject.Provider;

import io.javalin.Javalin;
import io.javalin.http.ContentType;

public class RunRoutingServer {
	public static void main(String[] args) {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("port") //
				.allowOptions("threads", "batch-size", "interval")
				.build();

		Javalin app = Javalin.create(config -> {
			config.enableCorsForAllOrigins();
			config.asyncRequestTimeout = 10_000L;
			config.enforceSsl = true;
		});

		EqasimConfigurator configurator = new EqasimConfigurator();
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configurator.getConfigGroups());
		cmd.applyConfiguration(config);

		// No opportunity scoring
		config.planCalcScore().setPerforming_utils_hr(0.0);
		
		// Load scenario to find transit modes
		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		
		// We only load network, schedule and transit vehicles
		new MatsimNetworkReader(scenario.getNetwork())
				.readURL(ConfigGroup.getInputFileURL(config.getContext(), config.network().getInputFile()));
		new TransitScheduleReader(scenario)
				.readURL(ConfigGroup.getInputFileURL(config.getContext(), config.transit().getTransitScheduleFile()));

		if (config.transit().getVehiclesFile() != null) {
			new MatsimVehicleReader(scenario.getTransitVehicles())
					.readURL(ConfigGroup.getInputFileURL(config.getContext(), config.transit().getVehiclesFile()));
		}
		
		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());
		int batchSize = cmd.getOption("batch-size").map(Integer::parseInt).orElse(100);
		double interval = (double) cmd.getOption("interval").map(Integer::parseInt).orElse(0);

		

		ObjectMapper objectMapper = new ObjectMapper();

		Injector injector = new InjectorBuilder(scenario) //
				.addOverridingModules(configurator.getModules()) //
				.addOverridingModule(new HeadwayImputerModule(numberOfThreads, batchSize, false, interval)).build();

		Provider<TransitRouter> routerProvider = injector.getProvider(TransitRouter.class);
		Provider<HeadwayCalculator> headwayCalculatorProvider = injector.getProvider(HeadwayCalculator.class);
		TransitSchedule schedule = injector.getInstance(TransitSchedule.class);
		Network network = injector.getInstance(Network.class);

		BatchPublicTransportRouter batchRouter = new BatchPublicTransportRouter(routerProvider,
				headwayCalculatorProvider, schedule, network, batchSize, numberOfThreads, interval, transitModes);

		
		BatchPublicTransportRouter transitRouter;
		BatchRoadRouter roadRouter;

		app.get("/transit", ctx -> {
			TransitRequest request = objectMapper.readValue(ctx.body(), TransitRequest.class);
			var result = transitRouter.run(request.tasks);

			TransitResponse response = new TransitResponse();
			response.trips.addAll(result.getLeft());
			response.legs.addAll(result.getRight());

			ctx.contentType(ContentType.JSON);
			ctx.result(objectMapper.writeValueAsString(response));
		});

		app.get("/road", ctx -> {
			RoadRequest request = objectMapper.readValue(ctx.body(), RoadRequest.class);
			var result = roadRouter.run(request.tasks);

			RoadResponse response = new RoadResponse();
			response.results.addAll(result);

			ctx.contentType(ContentType.JSON);
			ctx.result(objectMapper.writeValueAsString(response));
		});

		int port = Integer.parseInt(cmd.getOptionStrict("port"));
		app.start(port);
	}

	static public class TransitRequest {
		public List<TransitRoutingTask> tasks = new LinkedList<>();
	}

	static public class TransitResponse {
		public List<TripInformation> trips = new LinkedList<>();
		public List<LegInformation> legs = new LinkedList<>();
	}

	static public class RoadRequest {
		public List<RoadRoutingTask> tasks = new LinkedList<>();
	}

	static public class RoadResponse {
		public List<RoadRoutingResult> results = new LinkedList<>();
	}
}
