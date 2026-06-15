package org.eqasim.switzerland.ch_cmdp.utils.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.travel_time.RecordedTravelTime;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.matsim.core.config.CommandLine;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

public class TripsRouterServer {
    private static final Logger logger = LogManager.getLogger(TripsRouterServer.class);

    private static final Collection<String> REQUIRED_ARGS = Set.of("network-path", "events-path", "port");
    private static final Collection<String> OPTIONAL_ARGS = Set.of("threads", "start-time", "end-time", "interval",
            "return-links", "batch-size", "departure-time", "host", "routingDistanceUtility");

    private static final String JSON = "application/json";

    public static void main(String[] args) throws Exception {
        CommandLine cmd = buildCommandLine(args);
        Settings settings = readSettings(cmd);
        RuntimeState runtime = loadRuntime(settings);
        HttpServer server = buildServer(settings, runtime);
        server.start();
        logger.info("TripsRouter server listening on http://{}:{}", settings.host, settings.port);
        logger.info("POST /route with JSON array of trips or {\"trips\": [...]}.");
    }

    private static CommandLine buildCommandLine(String[] args) throws CommandLine.ConfigurationException {
        return new CommandLine.Builder(args)
                .requireOptions(REQUIRED_ARGS)
                .allowOptions(OPTIONAL_ARGS)
                .build();
    }

    private static Settings readSettings(CommandLine cmd) throws CommandLine.ConfigurationException {
        int threads = Math.max(1,
                cmd.getOption("threads").map(Integer::parseInt).orElse(Runtime.getRuntime().availableProcessors()));
        int batchSize = Math.max(1, cmd.getOption("batch-size").map(Integer::parseInt).orElse(1000));
        double startTime = Double.parseDouble(cmd.getOption("start-time").orElse("0.0"));
        double endTime = Double.parseDouble(cmd.getOption("end-time").orElse(String.valueOf(24 * 3600.0)));
        double interval = Double.parseDouble(cmd.getOption("interval").orElse("900.0"));
        double defaultDepartureTime = cmd.getOption("departure-time").map(Double::parseDouble).orElse(-1.0);
        double routingDistanceUtility = Double.parseDouble(cmd.getOption("routingDistanceUtility").orElse("0.0"));
        boolean returnLinks = cmd.getOption("return-links").map(Boolean::parseBoolean).orElse(false);
        String host = cmd.getOption("host").orElse("127.0.0.1");
        int port = Integer.parseInt(cmd.getOptionStrict("port"));
        String networkPath = cmd.getOptionStrict("network-path");
        String eventsPath = cmd.getOptionStrict("events-path");
        return new Settings(host, port, threads, batchSize, startTime, endTime, interval,
                defaultDepartureTime, routingDistanceUtility, returnLinks, networkPath, eventsPath);
    }

    private static RuntimeState loadRuntime(Settings settings) {
        logger.info("Loading network...");
        RoadNetwork roadNetwork = new RoadNetwork(TripsRouter.loadNetwork(settings.networkPath));
        logger.info("Loading recorded travel times from events...");
        RecordedTravelTime travelTime = TripsRouter.loadTravelTime(
                settings.eventsPath,
                roadNetwork,
                settings.startTime,
                settings.endTime,
                settings.interval
        );
        return new RuntimeState(roadNetwork, travelTime, settings.threads, settings.batchSize,
                settings.defaultDepartureTime, settings.routingDistanceUtility, settings.returnLinks,
                new ObjectMapper());
    }

    private static HttpServer buildServer(Settings settings, RuntimeState runtime) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(settings.host, settings.port), 0);
        server.setExecutor(Executors.newFixedThreadPool(runtime.threads));
        server.createContext("/health", TripsRouterServer::handleHealth);
        server.createContext("/config", exchange -> handleConfig(exchange, runtime));
        server.createContext("/route", exchange -> handleRoute(exchange, runtime));
        return server;
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "text/plain", "Method Not Allowed");
            return;
        }
        respond(exchange, 200, "text/plain", "ok");
    }

    private static void handleRoute(HttpExchange exchange, RuntimeState runtime) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "text/plain", "Method Not Allowed");
            return;
        }

        try {
            List<TripsRouter.RoutedTrip> routedTrips = TripsRouter.routeTripsFromJsonRequest(
                    runtime.mapper,
                    exchange.getRequestBody(),
                    runtime.network,
                    runtime.travelTime,
                    runtime.threads,
                    runtime.batchSize,
                    runtime.defaultDepartureTime,
                    runtime.returnLinks,
                    runtime.routingDistanceUtility
            );
            respondJson(exchange, 200, runtime.mapper.writeValueAsBytes(routedTrips));
        } catch (IllegalArgumentException e) {
            respond(exchange, 400, "text/plain", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Routing interrupted", e);
            respond(exchange, 500, "text/plain", "Routing interrupted");
        } catch (Exception e) {
            logger.error("Routing request failed", e);
            respond(exchange, 500, "text/plain", e.getMessage());
        }
    }

    private static void handleConfig(HttpExchange exchange, RuntimeState runtime) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            respondJson(exchange, 200, runtime.mapper.writeValueAsBytes(runtime.configResponse()));
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "text/plain", "Method Not Allowed");
            return;
        }

        try {
            JsonNode body = runtime.mapper.readTree(exchange.getRequestBody());
            if (body == null || !body.isObject()) {
                throw new IllegalArgumentException("Body must be a JSON object.");
            }

            if (body.has("return_links")) {
                runtime.returnLinks = body.get("return_links").asBoolean();
            }
            if (body.has("returnLinks")) {
                runtime.returnLinks = body.get("returnLinks").asBoolean();
            }
            if (body.has("routing_distance_utility")) {
                runtime.routingDistanceUtility = body.get("routing_distance_utility").asDouble();
            }
            if (body.has("routingDistanceUtility")) {
                runtime.routingDistanceUtility = body.get("routingDistanceUtility").asDouble();
            }

            respondJson(exchange, 200, runtime.mapper.writeValueAsBytes(runtime.configResponse()));
        } catch (IllegalArgumentException e) {
            respond(exchange, 400, "text/plain", e.getMessage());
        } catch (Exception e) {
            logger.error("Config update failed", e);
            respond(exchange, 500, "text/plain", e.getMessage());
        }
    }

    private static void respondJson(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", JSON);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] response = body.getBytes();
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, response.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(response);
        }
    }

    private static class Settings {
        final String host;
        final int port;
        final int threads;
        final int batchSize;
        final double startTime;
        final double endTime;
        final double interval;
        final double defaultDepartureTime;
        final boolean returnLinks;
        final String networkPath;
        final String eventsPath;
        final double routingDistanceUtility;

        Settings(String host, int port, int threads, int batchSize, double startTime, double endTime,
                double interval, double defaultDepartureTime, double routingDistanceUtility,
                 boolean returnLinks, String networkPath, String eventsPath) {
            this.host = host;
            this.port = port;
            this.threads = threads;
            this.batchSize = batchSize;
            this.startTime = startTime;
            this.endTime = endTime;
            this.interval = interval;
            this.defaultDepartureTime = defaultDepartureTime;
            this.returnLinks = returnLinks;
            this.networkPath = networkPath;
            this.eventsPath = eventsPath;
            this.routingDistanceUtility = routingDistanceUtility;
        }
    }

    private static class RuntimeState {
        final RoadNetwork network;
        final RecordedTravelTime travelTime;
        final int threads;
        final int batchSize;
        final double defaultDepartureTime;
        final ObjectMapper mapper;
        volatile double routingDistanceUtility;
        volatile boolean returnLinks;

        RuntimeState(RoadNetwork network, RecordedTravelTime travelTime, int threads, int batchSize,
                double defaultDepartureTime, double routingDistanceUtility, boolean returnLinks,
                ObjectMapper mapper) {
            this.network = network;
            this.travelTime = travelTime;
            this.threads = threads;
            this.batchSize = batchSize;
            this.defaultDepartureTime = defaultDepartureTime;
            this.mapper = mapper;
            this.routingDistanceUtility = routingDistanceUtility;
            this.returnLinks = returnLinks;
        }

        ConfigResponse configResponse() {
            return new ConfigResponse(returnLinks, routingDistanceUtility);
        }
    }

    private static class ConfigResponse {
        public final boolean return_links;
        public final double routing_distance_utility;

        ConfigResponse(boolean returnLinks, double routingDistanceUtility) {
            this.return_links = returnLinks;
            this.routing_distance_utility = routingDistanceUtility;
        }
    }
}
