package org.eqasim.server;

import static org.junit.Assert.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.core.config.CommandLine;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestInterfaceServer {
	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void realServicesWithAndWithoutTransit() throws Exception {
		// The legacy fixture labels its Lambert-93 coordinates as Atlantis.
		// Correct the test copy only; production metadata must respect the loaded network.
		for (String file : new String[] { "config.xml", "network.xml.gz", "transit_schedule.xml.gz" }) {
			var path = temporary.getRoot().toPath().resolve(file);
			try (var input = getClass().getResourceAsStream("/melun/" + file)) {
				if (file.endsWith(".gz")) {
					try (var gzip = new GZIPInputStream(input); var output = new GZIPOutputStream(Files.newOutputStream(path))) {
						output.write(new String(gzip.readAllBytes(), StandardCharsets.UTF_8).replace("Atlantis", "EPSG:2154").getBytes(StandardCharsets.UTF_8));
					}
				} else Files.writeString(path, new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("Atlantis", "EPSG:2154"));
			}
		}
		String config = temporary.getRoot().toPath().resolve("config.xml").toString();
		var mapper = new ObjectMapper();
		var client = HttpClient.newHttpClient();
		for (boolean transit : new boolean[] { true, false }) {
			var cmd = new CommandLine.Builder(new String[] { "--config-path", config,
					"--threads", "2", "--provide-interface", "true", "--use-transit", String.valueOf(transit),
					"--eqasim-configurator", ServerTestConfigurator.class.getName() })
					.allowOptions("config-path", "threads", "provide-interface", "use-transit", "eqasim-configurator").build();
			var services = new ServiceBuilder().build(cmd);
			var app = RunServer.createApplication(services, 2, true).start(0);
			try {
				String base = "http://localhost:" + app.port();
				var page = client.send(HttpRequest.newBuilder(URI.create(base + "/interface")).build(), HttpResponse.BodyHandlers.ofString());
				assertEquals(200, page.statusCode());
				assertTrue(page.headers().firstValue("content-type").orElse("").startsWith("text/html"));
				assertTrue(page.body().contains("Scenario explorer"));
				var metadata = mapper.readTree(client.send(HttpRequest.newBuilder(URI.create(base + "/interface/metadata")).build(), HttpResponse.BodyHandlers.ofString()).body());
				assertEquals("EPSG:2154", metadata.get("crs").asText());
				assertEquals(services.interfaceMetadata().scheduleDayCount(), metadata.get("schedule_day_count").asLong());
				if (!transit) assertEquals(1, metadata.get("schedule_day_count").asLong());
				var available = mapper.readTree(client.send(HttpRequest.newBuilder(URI.create(base + "/services")).build(), HttpResponse.BodyHandlers.ofString()).body()).get("services");
				assertEquals(transit ? 4 : 2, available.size());
				for (var operation : available) {
					boolean route = operation.asText().startsWith("router/");
					String request = "{\"request\":{\"origin_x\":673335,\"origin_y\":6826738,\"departure_time_s\":28800,"
							+ (route ? "\"destination_x\":676489,\"destination_y\":6825926,\"provide_geometry\":true"
									+ (operation.asText().endsWith("transit") ? ",\"provide_itinerary\":true" : "") : "\"maximum_travel_time_min\":30") + "}}";
					var response = client.send(HttpRequest.newBuilder(URI.create(base + "/" + operation.asText()))
							.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(request)).build(), HttpResponse.BodyHandlers.ofString());
					assertEquals(response.body(), 200, response.statusCode());
					var body = mapper.readTree(response.body());
					assertTrue(body.isObject());
					assertTrue(body.has(route ? "total_travel_time_min" : operation.asText().endsWith("transit") ? "stops" : "points"));
				}
			} finally { app.stop(); }
			var optOut = RunServer.createApplication(services, 2, false).start(0);
			try {
				for (String path : new String[] { "/interface", "/interface/metadata" }) {
					assertEquals(404, client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + optOut.port() + path)).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
				}
			} finally { optOut.stop(); }
		}
	}
}
