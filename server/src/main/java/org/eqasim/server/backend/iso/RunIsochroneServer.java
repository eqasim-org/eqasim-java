package org.eqasim.server.backend.iso;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eqasim.server.backend.iso.TransitIsochroneBackend.RequestMode;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.Javalin;

public class RunIsochroneServer {
	private final static Logger logger = LogManager.getLogger(RunIsochroneServer.class);

	static public void main(String[] args)
			throws IOException, ConfigurationException, NoSuchAuthorityCodeException, FactoryException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("network-path", "schedule-path", "port") //
				.build();

		// Read MATSim related data
		logger.info("Reading MATSim data ...");

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		new MatsimNetworkReader(scenario.getNetwork()).readFile(cmd.getOptionStrict("network-path"));
		new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("schedule-path"));

		Network network = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(network, Collections.singleton("car"));

		// Prepare routing
		RoadIsochroneBackend roadBackend = new RoadIsochroneBackend(network, new FreeSpeedTravelTime());
		TransitIsochroneBackend transitBackend = new TransitIsochroneBackend(scenario.getTransitSchedule());

		// Create Javalin application and enable CORS
		Javalin app = Javalin.create(javalinConfig -> {
			javalinConfig.plugins.enableCors(cors -> {
				cors.add(it -> {
					it.anyHost();
				});
			});
		});

		ObjectMapper objectMapper = new ObjectMapper();

		// Road routing endpoint
		app.post("/road", ctx -> {
			RoadRequest request = objectMapper.readValue(ctx.bodyAsBytes(), RoadRequest.class);

			RoadIsochroneBackend.Request roadRequest = new RoadIsochroneBackend.Request(request.x, request.y,
					request.departureTime, request.maximumTravelTime, request.transferDistance, request.segmentLength);

			List<RoadResponse> response = new LinkedList<>();

			for (var destination : roadBackend.process(roadRequest).destinations) {
				response.add(new RoadResponse(destination.x, destination.y, destination.travelTime,
						destination.distance, destination.isOrigin, destination.isRestricted));
			}

			ctx.result(objectMapper.writeValueAsBytes(response));
		});

		// Transit routing endpoint
		app.post("/transit", ctx -> {
			TransitRequest request = objectMapper.readValue(ctx.bodyAsBytes(), TransitRequest.class);

			TransitIsochroneBackend.Request transitRequest = new TransitIsochroneBackend.Request(request.x, request.y,
					request.departureTime, request.maximumTravelTime, request.maximumTransfers, request.requestMode,
					request.transferDistance, 1.0, request.transferSpeed);

			List<TransitResponse> response = new LinkedList<>();

			for (var destination : transitBackend.process(transitRequest).destinations) {
				response.add(new TransitResponse(destination.x, destination.y, destination.travelTime,
						destination.transfers, destination.isOrigin));
			}

			ctx.result(objectMapper.writeValueAsBytes(response));
		});

		// Run API
		int port = Integer.parseInt(cmd.getOptionStrict("port"));
		app.start(port);
	}

	static public class RoadRequest {
		@JsonProperty("x")
		public double y;

		@JsonProperty("y")
		public double x;

		@JsonProperty("departure_time")
		public double departureTime = 8.0 * 3600.0;

		@JsonProperty("transfer_distance")
		public double transferDistance = 600.0;

		@JsonProperty("maximum_travel_time")
		public double maximumTravelTime = 600.0;

		@JsonProperty("segment_length")
		public double segmentLength = Double.NaN;
	}

	static public class TransitRequest {
		@JsonProperty("x")
		public double y;

		@JsonProperty("y")
		public double x;

		@JsonProperty("departure_time")
		public double departureTime = 8.0 * 3600.0;

		@JsonProperty("transfer_distance")
		public double transferDistance = 600.0;

		@JsonProperty("maximum_travel_time")
		public double maximumTravelTime = 600.0;

		@JsonProperty("maximum_transfers")
		public int maximumTransfers = 5;

		@JsonProperty("transfer_speed")
		public double transferSpeed = 2.22 / 1.3;

		@JsonProperty("mode")
		public RequestMode requestMode = RequestMode.Default;
	}

	static public class RoadResponse {
		@JsonProperty("x")
		public final double x;

		@JsonProperty("y")
		public final double y;

		@JsonProperty("travel_time")
		public final double travelTime;

		@JsonProperty("distance")
		public final double distance;

		@JsonProperty("is_origin")
		public final boolean isOrigin;

		@JsonProperty("is_restricted")
		public final boolean isRestricted;

		public RoadResponse(double x, double y, double travelTime, double distance, boolean isOrigin,
				boolean isRestricted) {
			this.x = x;
			this.y = y;
			this.travelTime = travelTime;
			this.distance = distance;
			this.isOrigin = isOrigin;
			this.isRestricted = isRestricted;
		}
	}

	static public class TransitResponse {
		@JsonProperty("x")
		public final double x;

		@JsonProperty("y")
		public final double y;

		@JsonProperty("travel_time")
		public final double travelTime;

		@JsonProperty("transfers")
		public final int transfers;

		@JsonProperty("is_origin")
		public final boolean isOrigin;

		public TransitResponse(double x, double y, double travelTime, int transfers, boolean isOrigin) {
			this.x = x;
			this.y = y;
			this.travelTime = travelTime;
			this.transfers = transfers;
			this.isOrigin = isOrigin;
		}
	}
}
