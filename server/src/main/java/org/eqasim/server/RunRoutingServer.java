package org.eqasim.server;

import java.io.File;

import org.eqasim.server.api.RoadNetworkApi;
import org.eqasim.server.api.TransitNetworkApi;
import org.eqasim.server.api.grid.ModalGridApi;
import org.eqasim.server.api.grid.RoadGridApi;
import org.eqasim.server.api.grid.TransitGridApi;
import org.eqasim.server.api.routing.RoadRoutingApi;
import org.eqasim.server.api.routing.TransitRoutingApi;
import org.eqasim.server.backend.BackendScenario;
import org.eqasim.server.backend.grid.ModalGridBackend;
import org.eqasim.server.backend.grid.RoadGridBackend;
import org.eqasim.server.backend.grid.TransitGridBackend;
import org.eqasim.server.backend.network.RoadNetworkBackend;
import org.eqasim.server.backend.routing.RoadRouterBackend;
import org.eqasim.server.backend.routing.TransitRouterBackend;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import io.javalin.Javalin;

public class RunRoutingServer {
	public static void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "port") //
				.build();

		// Create Javalin application and enable CORS
		Javalin app = Javalin.create(config -> {
			config.plugins.enableCors(cors -> {
				cors.add(it -> {
					it.anyHost();
				});
			});
		});

		// Load scenario data
		BackendScenario scenario = BackendScenario.create(new File(cmd.getOptionStrict("config-path")));

		// Build up backend
		RoadNetworkBackend roadNetworkBuilder = RoadNetworkBackend.create(scenario);
		RoadRouterBackend roadRouterBackend = RoadRouterBackend.create(scenario);
		TransitRouterBackend transitRouterBackend = TransitRouterBackend.create(scenario);

		RoadGridBackend roadGridBackend = new RoadGridBackend(scenario);
		TransitGridBackend transitGridBackend = new TransitGridBackend(scenario, transitRouterBackend);
		ModalGridBackend modalGridBackend = new ModalGridBackend(scenario, roadGridBackend, transitGridBackend);

		// Build up API
		RoadNetworkApi roadNetworkApi = new RoadNetworkApi(roadNetworkBuilder);
		app.post("/road/network", roadNetworkApi::postNetwork);

		TransitNetworkApi transitNetworkApi = new TransitNetworkApi(scenario);
		app.post("/transit/stops", transitNetworkApi::postStops);

		RoadRoutingApi roadRoutingApi = new RoadRoutingApi(roadRouterBackend);
		app.post("/route/road", roadRoutingApi::postRoute);

		TransitRoutingApi transitRoutingApi = new TransitRoutingApi(transitRouterBackend);
		app.post("/route/transit", transitRoutingApi::postRoute);

		RoadGridApi roadGridApi = new RoadGridApi(roadGridBackend);
		app.post("/grid/road", roadGridApi::postRoadGrid);

		TransitGridApi transitGridApi = new TransitGridApi(transitGridBackend);
		app.post("/grid/transit", transitGridApi::postTransitGrid);

		ModalGridApi modalGridApi = new ModalGridApi(modalGridBackend);
		app.post("/grid/modal", modalGridApi::postModalGrid);

		// Run API
		int port = Integer.parseInt(cmd.getOptionStrict("port"));
		app.start(port);
	}
}
