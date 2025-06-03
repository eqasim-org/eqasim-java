package org.eqasim.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.server.ServiceBuilder.Services;
import org.eqasim.server.api.RoadIsochroneEndpoint;
import org.eqasim.server.api.RoadRouterEndpoint;
import org.eqasim.server.api.TransitIsochroneEndpoint;
import org.eqasim.server.api.TransitRouterEndpoint;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.utils.io.IOUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.javalin.Javalin;

public class RunServer {
	public static void main(String[] args)
			throws ConfigurationException, JsonParseException, JsonMappingException, IOException {

		List<String> filteredArgs = new ArrayList<>();
        Map<String, String> raptorPenaltyOptions = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
			String arg = args[i];

			if (arg.startsWith("--raptorPenalties")) {
				String key, value;

				if (arg.contains("=")) {
					// Example: --raptorPenalties.transfer_bus_to_rail=60
					String[] parts = arg.substring(2).split("=", 2);
					key = parts[0];  
					value = parts[1]; 
				} else {
					// Example: --raptorPenalties.transfer_bus_to_rail 60
					key = arg.substring(2); 

					if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
						value = args[i + 1];
						i++; 
					} else {
						value = ""; 
					}
				}

				raptorPenaltyOptions.put(key, value);
			} else {
				filteredArgs.add(arg);
			}
		}
		
		CommandLine cmd = new CommandLine.Builder(filteredArgs.toArray(new String[0])) //
				.requireOptions("config-path") //
				.allowOptions("port", "threads", "configuration-path", "use-transit", "vdf-path", "port-path",
						EqasimConfigurator.CONFIGURATOR) //
				.build();

		Services services = new ServiceBuilder().build(cmd, raptorPenaltyOptions);

		int threads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());

		// Create Javalin application and enable CORS
		Javalin app = Javalin.create(config -> {
			config.bundledPlugins.enableCors(cors -> {
				cors.addRule(it -> {
					it.anyHost();
				});
			});
		});

		ExecutorService executor = Executors.newFixedThreadPool(threads);

		RoadRouterEndpoint roadRouterEndpoint = new RoadRouterEndpoint(executor, services.roadRouterService());
		app.post("/router/road", roadRouterEndpoint::post);

		RoadIsochroneEndpoint roadIsochroneEndpoint = new RoadIsochroneEndpoint(executor,
				services.roadIsochroneService());
		app.post("/isochrone/road", roadIsochroneEndpoint::post);

		if (services.transitRouterService() != null) {
			TransitRouterEndpoint transitRouterEndpoint = new TransitRouterEndpoint(executor,
					services.transitRouterService());
			app.post("/router/transit", transitRouterEndpoint::post);

			TransitIsochroneEndpoint transitIsochroneEndpoint = new TransitIsochroneEndpoint(executor,
					services.transitIsochroneService());
			app.post("/isochrone/transit", transitIsochroneEndpoint::post);
		}

		// Run API
		int port = cmd.getOption("port").map(Integer::parseInt).orElse(0);
		app.start(port);

		if (cmd.hasOption("port-path")) {
			BufferedWriter writer = IOUtils.getBufferedWriter(cmd.getOptionStrict("port-path"));
			writer.write(String.valueOf(app.jettyServer().port()));
			writer.close();
		}
	}
}
