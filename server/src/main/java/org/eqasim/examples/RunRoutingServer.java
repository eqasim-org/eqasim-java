package org.eqasim.examples;

import java.util.Collections;
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
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.config.TransitRouterConfigGroup;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.MatsimVehicleReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorParametersForPerson;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig.RaptorOptimization;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import io.javalin.Javalin;
import io.javalin.http.ContentType;

public class RunRoutingServer {
	public static void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "port") //
				.allowOptions("threads", "batch-size", "interval").build();

		Javalin app = Javalin.create(config -> {
			config.plugins.enableCors(cors -> {
				cors.add(it -> {
					it.anyHost();
				});
			});
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

		SwissRailRaptorConfigGroup srrConfig = (SwissRailRaptorConfigGroup) config.getModules().get(SwissRailRaptorConfigGroup.GROUP);
		TransitRouterConfigGroup routerConfig = (TransitRouterConfigGroup) config.getModules().get(TransitRouterConfigGroup.GROUP_NAME);
		
		routerConfig.setDirectWalkFactor(100.0);
		
		RaptorStaticConfig staticConfig = RaptorUtils.createStaticConfig(config);
		staticConfig.setOptimization(RaptorOptimization.OneToAllRouting);
		staticConfig.setBeelineWalkConnectionDistance(1000.0);

		ObjectMapper objectMapper = new ObjectMapper();

		Injector injector = new InjectorBuilder(scenario) //
				.addOverridingModules(configurator.getModules()) //
				.addOverridingModule(new HeadwayImputerModule(numberOfThreads, batchSize, false, interval)) //
				.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						bind(RaptorStaticConfig.class).toInstance(staticConfig);
					}
				}).addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
					}

					@Provides
					@Singleton
					@Named("car")
					public Network provideCarNetwork(Network network) {
						Network carNetwork = NetworkUtils.createNetwork();
						new TransportModeNetworkFilter(network).filter(carNetwork, Collections.singleton("car"));
						new NetworkCleaner().run(carNetwork);
						return carNetwork;
					}
				}).build();

		Provider<TransitRouter> routerProvider = injector.getProvider(TransitRouter.class);
		Provider<HeadwayCalculator> headwayCalculatorProvider = injector.getProvider(HeadwayCalculator.class);
		TransitSchedule schedule = injector.getInstance(TransitSchedule.class);
		Network network = injector.getInstance(Network.class);

		BatchPublicTransportRouter transitRouter = new BatchPublicTransportRouter(routerProvider,
				headwayCalculatorProvider, schedule, network, batchSize, numberOfThreads, interval);

		Network roadNetwork = injector.getInstance(Key.get(Network.class, Names.named("car")));

		BatchRoadRouter roadRouter = new BatchRoadRouter(injector.getProvider(LeastCostPathCalculatorFactory.class),
				roadNetwork, batchSize, numberOfThreads, true);

		app.post("/transit", ctx -> {
			TransitRequest request = objectMapper.readValue(ctx.body(), TransitRequest.class);
			var result = transitRouter.run(request.tasks);

			TransitResponse response = new TransitResponse();
			response.trips.addAll(result.getLeft());
			response.legs.addAll(result.getRight());

			ctx.contentType(ContentType.JSON);
			ctx.result(objectMapper.writeValueAsString(response));
		});

		app.post("/road", ctx -> {
			RoadRequest request = objectMapper.readValue(ctx.body(), RoadRequest.class);
			var result = roadRouter.run(request.tasks);

			RoadResponse response = new RoadResponse();
			response.results.addAll(result);

			ctx.contentType(ContentType.JSON);
			ctx.result(objectMapper.writeValueAsString(response));
		});

		IdSet<TransitStopFacility> relevantIds = new IdSet<>(TransitStopFacility.class);
		for (TransitLine transitLine : schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				// if (transitRoute.getTransportMode().equals("rail")) {
				for (TransitRouteStop stop : transitRoute.getStops()) {
					relevantIds.add(stop.getStopFacility().getId());
				}
				// }
			}
		}

		TransitStopResponse stopResponse = new TransitStopResponse();
		for (TransitStopFacility stopFacility : schedule.getFacilities().values()) {
			if (relevantIds.contains(stopFacility.getId())) {
				TransitStopInfo info = new TransitStopInfo();
				info.stopId = stopFacility.getId().toString();
				info.name = stopFacility.getName();
				info.areaId = stopFacility.getStopAreaId().toString();
				info.x = stopFacility.getCoord().getX();
				info.y = stopFacility.getCoord().getY();
				stopResponse.stops.add(info);
			}
		}

		app.get("/stops", ctx -> {
			ctx.contentType(ContentType.JSON);
			ctx.result(objectMapper.writeValueAsString(stopResponse));
		});

		Provider<RaptorParametersForPerson> parametersProvider = injector.getProvider(RaptorParametersForPerson.class);

		app.post("/transit_tree", ctx -> {
			TransitTreeRequest request = objectMapper.readValue(ctx.body(), TransitTreeRequest.class);
			TransitTreeResponse response = new TransitTreeResponse();
			response.id = request.id;

			TransitRouter router = routerProvider.get();
			SwissRailRaptor srr = (SwissRailRaptor) router;

			RaptorParameters parameters = parametersProvider.get().getRaptorParameters(null);

			/*
			 * TransitStopFacility facility = schedule.getFacilities()
			 * .get(Id.create(request.id, TransitStopFacility.class));
			 */

			List<TransitStopFacility> facilities = new LinkedList<>();

			for (TransitStopFacility candidate : schedule.getFacilities().values()) {
				if (candidate.getStopAreaId().toString().equals(request.id)) {
					facilities.add(candidate);
				}
			}

			for (var entry : srr.calcTree(facilities, request.departureTime, parameters, null).entrySet()) {
				ConnectionInfo connectionInfo = new ConnectionInfo();
				connectionInfo.id = entry.getKey().toString();

				var connection = entry.getValue();

				connectionInfo.travelTime = connection.ptTravelTime;
				connectionInfo.initialWaitTime = connection.ptDepartureTime - request.departureTime;
				connectionInfo.transfers = connection.transferCount;

				TransitStopFacility destination = schedule.getFacilities().get(entry.getKey());
				connectionInfo.x = destination.getCoord().getX();
				connectionInfo.y = destination.getCoord().getY();

				response.connections.add(connectionInfo);
			}

			ctx.contentType(ContentType.JSON);
			ctx.result(objectMapper.writeValueAsString(response));
		});

		double _totalX = 0.0;
		double _totalY = 0.0;

		for (TransitStopFacility stopFacility : schedule.getFacilities().values()) {
			_totalX += stopFacility.getCoord().getX();
			_totalY += stopFacility.getCoord().getY();
		}

		_totalX /= schedule.getFacilities().size();
		_totalY /= schedule.getFacilities().size();

		final double totalX = _totalX;
		final double totalY = _totalY;

		app.get("/center", ctx -> {
			CenterResponse response = new CenterResponse();

			response.x = totalX;
			response.y = totalY;

			ctx.contentType(ContentType.JSON);
			ctx.result(objectMapper.writeValueAsString(response));
		});

		int port = Integer.parseInt(cmd.getOptionStrict("port"));
		app.start(port);
	}

	static public class CenterResponse {
		public double x;
		public double y;
	}

	static public class ConnectionInfo {
		public String id;
		public double x;
		public double y;

		public double travelTime;
		public double initialWaitTime;
		public int transfers;
	}

	static public class TransitTreeResponse {
		public String id;
		public List<ConnectionInfo> connections = new LinkedList<>();
	}

	static public class TransitTreeRequest {
		public String id;
		public double departureTime;
	}

	static public class TransitStopInfo {
		public String stopId;
		public String areaId;

		public double x;
		public double y;

		public String name;
	}

	static public class TransitStopResponse {
		public List<TransitStopInfo> stops = new LinkedList<>();
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
