package org.eqasim.server;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eqasim.core.components.raptor.EqasimRaptorConfigGroup;
import org.eqasim.server.api.RoadIsochroneEndpoint;
import org.eqasim.server.api.RoadRouterEndpoint;
import org.eqasim.server.api.TransitIsochroneEndpoint;
import org.eqasim.server.api.TransitRouterEndpoint;
import org.eqasim.server.services.ServiceConfiguration;
import org.eqasim.server.services.isochrone.road.RoadIsochroneService;
import org.eqasim.server.services.isochrone.transit.TransitIsochroneService;
import org.eqasim.server.services.router.road.RoadRouterService;
import org.eqasim.server.services.router.transit.TransitRouterService;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.Javalin;

public class RunServer {
	public static void main(String[] args)
			throws ConfigurationException, JsonParseException, JsonMappingException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "port") //
				.allowOptions("threads", "configuration-path") //
				.build();

		int threads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());

		ServiceConfiguration configuration = new ServiceConfiguration();

		if (cmd.hasOption("configuration-path")) {
			ObjectMapper objectMapper = new ObjectMapper();
			configuration = objectMapper.readValue(new File(cmd.getOptionStrict("configuration-path")),
					ServiceConfiguration.class);
		}

		// Create Javalin application and enable CORS
		Javalin app = Javalin.create(config -> {
			config.plugins.enableCors(cors -> {
				cors.add(it -> {
					it.anyHost();
				});
			});
		});

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), new EqasimRaptorConfigGroup());
		Scenario scenario = ScenarioUtils.createScenario(config);

		new MatsimNetworkReader(scenario.getNetwork())
				.readURL(ConfigGroup.getInputFileURL(config.getContext(), config.network().getInputFile()));

		// new TransitScheduleReader(scenario)
		// .readURL(ConfigGroup.getInputFileURL(config.getContext(),
		// config.transit().getTransitScheduleFile()));

		ExecutorService executor = Executors.newFixedThreadPool(threads);

		Network roadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(roadNetwork, Collections.singleton("car"));
		new NetworkCleaner().run(roadNetwork);

		RoadRouterService roadRouterService = RoadRouterService.create(config, roadNetwork, configuration.walk,
				threads);
		RoadRouterEndpoint roadRouterEndpoint = new RoadRouterEndpoint(executor, roadRouterService);
		app.post("/router/road", roadRouterEndpoint::post);

		RoadIsochroneService roadIsochroneService = RoadIsochroneService.create(config, roadNetwork,
				configuration.walk);
		RoadIsochroneEndpoint roadIsochroneEndpoint = new RoadIsochroneEndpoint(executor, roadIsochroneService);
		app.post("/isochrone/road", roadIsochroneEndpoint::post);

		TransitRouterService transitRouterService = TransitRouterService.create(config, scenario.getNetwork(),
				scenario.getTransitSchedule(), configuration.transit, configuration.walk);
		TransitRouterEndpoint transitRouterEndpoint = new TransitRouterEndpoint(executor, transitRouterService);
		app.post("/router/transit", transitRouterEndpoint::post);

		TransitIsochroneService transitIsochroneService = TransitIsochroneService.create(config,
				scenario.getTransitSchedule(), configuration.transit, configuration.walk);
		TransitIsochroneEndpoint transitIsochroneEndpoint = new TransitIsochroneEndpoint(executor,
				transitIsochroneService);
		app.post("/isochrone/transit", transitIsochroneEndpoint::post);

		// Run API
		int port = Integer.parseInt(cmd.getOptionStrict("port"));
		app.start(port);
	}
}
