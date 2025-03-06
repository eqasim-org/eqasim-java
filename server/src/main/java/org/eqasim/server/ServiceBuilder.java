package org.eqasim.server;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.eqasim.core.components.raptor.EqasimRaptorConfigGroup;
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
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ServiceBuilder {
	public record Services(
			RoadRouterService roadRouterService,
			RoadIsochroneService roadIsochroneService,
			TransitRouterService transitRouterService,
			TransitIsochroneService transitIsochroneService) {
	}

	public Services build(CommandLine cmd)
			throws JsonParseException, JsonMappingException, IOException, ConfigurationException {
		int threads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());

		ServiceConfiguration configuration = new ServiceConfiguration();

		if (cmd.hasOption("configuration-path")) {
			ObjectMapper objectMapper = new ObjectMapper();
			configuration = objectMapper.readValue(new File(cmd.getOptionStrict("configuration-path")),
					ServiceConfiguration.class);
		}

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				new EqasimRaptorConfigGroup());
		Scenario scenario = ScenarioUtils.createScenario(config);

		new MatsimNetworkReader(scenario.getNetwork())
				.readURL(ConfigGroup.getInputFileURL(config.getContext(),
						config.network().getInputFile()));

		boolean useTransit = cmd.getOption("use-transit").map(Boolean::parseBoolean).orElse(true);
		if (useTransit) {
			new TransitScheduleReader(scenario).readURL(
					ConfigGroup.getInputFileURL(config.getContext(),
							config.transit().getTransitScheduleFile()));
		}

		Network roadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(roadNetwork, Collections.singleton("car"));
		new NetworkCleaner().run(roadNetwork);

		final RoadRouterService roadRouterService;
		final RoadIsochroneService roadIsochroneService;
		final TransitRouterService transitRouterService;
		final TransitIsochroneService transitIsochroneService;

		roadRouterService = RoadRouterService.create(config, roadNetwork, configuration.walk,
				threads);

		roadIsochroneService = RoadIsochroneService.create(config, roadNetwork,
				configuration.walk);

		if (useTransit) {
			transitRouterService = TransitRouterService.create(config, scenario.getNetwork(),
					scenario.getTransitSchedule(), configuration.transit, configuration.walk);

			transitIsochroneService = TransitIsochroneService.create(config,
					scenario.getTransitSchedule(), configuration.transit, configuration.walk);
		} else {
			transitRouterService = null;
			transitIsochroneService = null;
		}

		return new Services(roadRouterService, roadIsochroneService, transitRouterService, transitIsochroneService);
	}
}
