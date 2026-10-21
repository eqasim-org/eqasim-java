package org.eqasim.switzerland.ch_cmdp.utils.routing;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.CommandLine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/** HTTP adapter for {@link MultiModeRouter}; CSV paths refer to files on the server. */
public final class MultiModeRouterServer implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(MultiModeRouterServer.class);
    private final ObjectMapper json = new ObjectMapper();
    private final Supplier<MultiModeRouter> loader;
    private final Set<String> expectedModes;
    private volatile State state;
    private final HttpServer server;
    private final ExecutorService requests;
    private final ExecutorService routingRequests;
    private final ExecutorService initialization;
    private boolean started;
    private boolean closed;

    private record State(String status, MultiModeRouter router, String error) {}

    public static void main(String[] args) throws Exception {
        CommandLine cmd = commandLine(args);
        MultiModeRouter.Settings settings = readSettings(cmd);
        MultiModeRouterServer server = new MultiModeRouterServer(settings, cmd.getOption("host").orElse("127.0.0.1"),
                Integer.parseInt(cmd.getOptionStrict("port")));
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "multi-mode-router-shutdown"));
        server.start();
    }

    static CommandLine commandLine(String[] args) throws CommandLine.ConfigurationException {
        return new CommandLine.Builder(args)
                .requireOptions("network-path", "port")
                .allowOptions("config-path", "events-path", "host", "threads", "batch-size", "chunk-size",
                        "start-time", "end-time", "interval", "departure-time", "routingDistanceUtility",
                        "tolls-value-of-time", "tolls-sigma", "return-links", "compute-cost", "car-cost-model",
                        "marouter", "marouter-increment", "marouter-seed", "marouter-truck-pcu", "truck-max-speed",
                        "marouter-sample-size", "export-routes")
                .build();
    }

    static MultiModeRouter.Settings readSettings(CommandLine cmd) throws CommandLine.ConfigurationException {
        MultiModeRouter.Settings settings = new MultiModeRouter.Settings();
        settings.networkPath = cmd.getOptionStrict("network-path");
        settings.configPath = cmd.getOption("config-path").orElse(null);
        settings.eventsPath = cmd.getOption("events-path").orElse(null);
        settings.threads = cmd.getOption("threads").map(Integer::parseInt).orElse(settings.threads);
        settings.batchSize = cmd.getOption("batch-size").map(Integer::parseInt).orElse(settings.batchSize);
        settings.chunkSize = cmd.getOption("chunk-size").map(Integer::parseInt).orElse(settings.chunkSize);
        settings.startTime = cmd.getOption("start-time").map(Double::parseDouble).orElse(settings.startTime);
        settings.endTime = cmd.getOption("end-time").map(Double::parseDouble).orElse(settings.endTime);
        settings.interval = cmd.getOption("interval").map(Double::parseDouble).orElse(settings.interval);
        settings.departureTime = cmd.getOption("departure-time").map(Double::parseDouble).orElse(settings.departureTime);
        settings.routingDistanceUtility = cmd.getOption("routingDistanceUtility").map(Double::parseDouble)
                .orElse(settings.routingDistanceUtility);
        settings.tollsValueOfTime = cmd.getOption("tolls-value-of-time").map(Double::parseDouble)
                .orElse(settings.tollsValueOfTime);
        settings.tollsSigma = cmd.getOption("tolls-sigma").map(Double::parseDouble).orElse(settings.tollsSigma);
        settings.computeCost = cmd.getOption("compute-cost").map(v -> MultiModeRouter.booleanValue(v, "compute-cost"))
                .orElse(settings.computeCost);
        settings.carCostModel = cmd.getOption("car-cost-model").orElse(settings.carCostModel);
        settings.exportRoutes = cmd.getOption("export-routes")
                .map(v -> MultiModeRouter.booleanValue(v, "export-routes")).orElse(settings.exportRoutes);
        settings.marouter = cmd.getOption("marouter").map(v -> MultiModeRouter.booleanValue(v, "marouter")).orElse(false);
        settings.marouterIncrement = cmd.getOption("marouter-increment").map(Double::parseDouble).orElse(settings.marouterIncrement);
        settings.marouterSeed = cmd.getOption("marouter-seed").map(Long::parseLong).orElse(settings.marouterSeed);
        settings.marouterTruckPcu = cmd.getOption("marouter-truck-pcu").map(Double::parseDouble).orElse(settings.marouterTruckPcu);
        settings.truckMaxSpeed = cmd.getOption("truck-max-speed").map(Double::parseDouble).orElse(settings.truckMaxSpeed);
        settings.marouterSampleSize = cmd.getOption("marouter-sample-size").map(Double::parseDouble).orElse(null);
        settings.vdfTimeOverrides = new java.util.HashSet<>();
        for (String option : List.of("start-time", "end-time", "interval")) {
            if (cmd.hasOption(option)) settings.vdfTimeOverrides.add(option);
        }
        settings.configFallbackOptions = new java.util.HashSet<>();
        for (String option : List.of("threads", "routingDistanceUtility", "tolls-value-of-time", "tolls-sigma",
                "compute-cost", "car-cost-model", "marouter", "marouter-increment", "marouter-seed",
                "marouter-truck-pcu", "truck-max-speed", "marouter-sample-size", "export-routes")) {
            if (!cmd.hasOption(option)) {
                settings.configFallbackOptions.add(option);
            }
        }
        settings.returnLinks = cmd.getOption("return-links").map(Boolean::parseBoolean).orElse(false);
        return settings;
    }

    /** Takes ownership of the router; closing the server closes the router too. */
    public MultiModeRouterServer(MultiModeRouter router, String host, int port) throws IOException {
        this(null, router.getAvailableModes(), new State("ready", router, null), host, port);
    }

    /** Binds HTTP immediately; start() starts eager background data loading. */
    public MultiModeRouterServer(MultiModeRouter.Settings settings, String host, int port) throws IOException {
        this(() -> new MultiModeRouter(settings), settings.configPath == null ? Set.of("car", "truck", "car_passenger") : MultiModeRouter.MODES,
                host, port);
    }

    // Loader injection lets lifecycle tests control startup without timing-dependent sleeps.
    MultiModeRouterServer(Supplier<MultiModeRouter> loader, Set<String> expectedModes, String host, int port)
            throws IOException {
        this(loader, expectedModes, new State("loading", null, null), host, port);
    }

    private MultiModeRouterServer(Supplier<MultiModeRouter> loader, Set<String> expectedModes, State state,
            String host, int port) throws IOException {
        this.loader = loader;
        this.expectedModes = Set.copyOf(expectedModes);
        this.state = state;
        server = HttpServer.create(new InetSocketAddress(host, port), 16);
        // Readiness remains responsive while a CSV request waits for routing workers.
        requests = Executors.newFixedThreadPool(2);
        routingRequests = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(16));
        initialization = Executors.newSingleThreadExecutor(task -> new Thread(task, "multi-mode-router-loader"));
        server.setExecutor(requests);
        server.createContext("/health", exchange -> handleStatus(exchange, false));
        server.createContext("/ready", exchange -> handleStatus(exchange, true));
        for (String endpoint : List.of("route", "cost")) {
            server.createContext("/" + endpoint, exchange -> {
                try {
                    routingRequests.execute(() -> {
                        try {
                            handleCsvRequest(exchange, endpoint.equals("cost"));
                        } catch (IOException e) {
                            logger.debug("Routing response connection closed", e);
                            exchange.close();
                        }
                    });
                } catch (RejectedExecutionException e) {
                    try (exchange) {
                        respond(exchange, 503, Map.of("error", "Routing request queue is full or server is stopping"));
                    }
                }
            });
        }
    }

    public synchronized void start() {
        if (closed || started) {
            throw new IllegalStateException("Server is already started or closed");
        }
        started = true;
        server.start();
        logger.info("Multi-mode router listening on http://{}:{} ({})", server.getAddress().getHostString(),
                getPort(), state.status);
        var codeSource = MultiModeRouterServer.class.getProtectionDomain().getCodeSource();
        logger.info("Routing code source: {}; missing routes return NaN metrics",
                codeSource == null ? "unknown" : codeSource.getLocation());
        if (loader != null) {
            initialization.execute(this::loadRouter);
        }
    }

    private void loadRouter() {
        try {
            MultiModeRouter loaded = loader.get();
            synchronized (this) {
                if (closed) {
                    loaded.close();
                } else {
                    state = new State("ready", loaded, null);
                    logger.info("Routing data loaded; ready modes: {}", loaded.getAvailableModes());
                }
            }
        } catch (Exception e) {
            synchronized (this) {
                if (!closed) {
                    state = new State("failed", null, String.valueOf(e.getMessage()));
                }
            }
            logger.error("Routing initialization failed", e);
        } finally {
            initialization.shutdown();
        }
    }

    private Map<String, Object> statusResponse(State snapshot, String selectedMode) {
        Map<String, Object> modes = new LinkedHashMap<>();
        for (String mode : List.of("car", "truck", "car_passenger", "pt", "walk", "bike")) {
            boolean ready = snapshot.router != null && snapshot.router.getAvailableModes().contains(mode);
            String status = ready ? "ready" : snapshot.status.equals("ready") || !expectedModes.contains(mode)
                    ? "unavailable" : snapshot.status;
            modes.put(mode, Map.of("ready", ready, "status", status));
        }
        Map<String, Object> response = new LinkedHashMap<>();
        boolean ready = snapshot.router != null && (selectedMode == null
                || snapshot.router.getAvailableModes().contains(selectedMode));
        response.put("status", snapshot.status);
        response.put("ready", ready);
        response.put("costReady", snapshot.router != null && snapshot.router.isCostEnabled());
        response.put("marouter", snapshot.router != null && snapshot.router.isMarouterEnabled());
        response.put("modes", modes);
        if (selectedMode != null) {
            response.put("mode", selectedMode);
        }
        if (snapshot.error != null) {
            response.put("error", snapshot.error);
        }
        return response;
    }

    private void handleStatus(HttpExchange exchange, boolean readiness) throws IOException {
        try (exchange) {
            if (!exchange.getRequestMethod().equals("GET")) {
                respond(exchange, 405, Map.of("error", "Use GET"));
                return;
            }
            String mode = null;
            String query = exchange.getRequestURI().getRawQuery();
            if (query != null) {
                for (String parameter : query.split("&")) {
                    String[] parts = parameter.split("=", 2);
                    if (parts[0].equals("mode")) {
                        mode = parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
                    }
                }
            }
            if (mode != null && !MultiModeRouter.MODES.contains(mode)) {
                respond(exchange, 400, Map.of("error", "mode must be car, truck, car_passenger, pt, walk or bike"));
                return;
            }
            Map<String, Object> response = statusResponse(state, mode);
            respond(exchange, readiness && !Boolean.TRUE.equals(response.get("ready")) ? 503 : 200, response);
        }
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public long cost(String inputCsv, String outputCsv, String mode) throws IOException, InterruptedException {
        State snapshot = state;
        if (snapshot.router == null) {
            throw new IllegalStateException("Router is " + snapshot.status);
        }
        return snapshot.router.cost(inputCsv, outputCsv, mode);
    }

    public long cost(String inputCsv, String outputCsv) throws IOException, InterruptedException {
        return cost(inputCsv, outputCsv, null);
    }

    private void handleCsvRequest(HttpExchange exchange, boolean costOnly) throws IOException {
        try (exchange) {
            if (!exchange.getRequestMethod().equals("POST")) {
                respond(exchange, 405, Map.of("error", "Use POST"));
                return;
            }
            State snapshot = state;
            if (snapshot.router == null) {
                respond(exchange, 503, statusResponse(snapshot, null));
                return;
            }
            try {
                JsonNode body = json.readTree(exchange.getRequestBody());
                if (body == null || !body.isObject()) {
                    throw new IllegalArgumentException("Expected a JSON object with inputCsv and outputCsv");
                }
                String input = requiredText(body, "inputCsv");
                String output = requiredText(body, "outputCsv");
                String mode = body.hasNonNull("mode") ? requiredText(body, "mode") : null;
                long count;
                if (costOnly) {
                    count = snapshot.router.cost(input, output, mode);
                } else {
                    boolean includeCost = optionalBoolean(body, "computeCost", snapshot.router.isCostEnabled());
                    count = snapshot.router.route(input, output, mode, includeCost);
                }
                respond(exchange, 200, Map.of("trips", count, "outputCsv", output));
            } catch (IllegalArgumentException | JsonProcessingException e) {
                respond(exchange, 400, Map.of("error", e.getMessage()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                respond(exchange, 503, Map.of("error", "Routing interrupted"));
            } catch (Exception e) {
                logger.error("CSV routing request failed", e);
                respond(exchange, 500, Map.of("error", String.valueOf(e.getMessage())));
            }
        }
    }

    private static boolean optionalBoolean(JsonNode body, String name, boolean fallback) {
        JsonNode value = body.get(name);
        if (value == null || value.isNull()) return fallback;
        if (!value.isBoolean()) throw new IllegalArgumentException(name + " must be a boolean");
        return value.asBoolean();
    }

    private static String requiredText(JsonNode body, String name) {
        JsonNode value = body.get(name);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(name + " must be a non-empty string");
        }
        return value.asText();
    }

    private void respond(HttpExchange exchange, int status, Object response) throws IOException {
        byte[] bytes = json.writeValueAsString(response).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @Override
    public void close() {
        MultiModeRouter loaded;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            loaded = state.router;
            state = new State("stopped", null, null);
        }
        server.stop(1);
        requests.shutdownNow();
        routingRequests.shutdownNow();
        initialization.shutdownNow();
        if (loaded != null) {
            loaded.close();
        }
    }
}
