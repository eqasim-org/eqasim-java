package org.eqasim.switzerland.ch_cmdp.utils.routing;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.eqasim.core.components.raptor.EqasimRaptorConfigGroup;
import org.eqasim.core.components.raptor.EqasimRaptorUtils;
import org.eqasim.core.components.traffic.bike.BikeDisutilities;
import org.eqasim.core.components.traffic.bike.BikeGradientBasedLinkSpeedCalculator;
import org.eqasim.core.components.travel_disutility.EqasimTravelDisutilityFactory;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.eqasim.core.simulation.policies.routing.ZeroRoutingPenalty;
import org.eqasim.switzerland.ch_cmdp.tolls.EqasimTollsTravelDisutility;
import org.eqasim.switzerland.ch_cmdp.tolls.MarginalCostOfTolls;
import org.eqasim.switzerland.ch_cmdp.tolls.Tolls;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.Vehicle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.OccupancyData;
import ch.sbb.matsim.routing.pt.raptor.ConfigurableRaptorRouteSelector;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;

/**
 * Reusable CSV router. All scenario data, spatial indices, ALT landmarks and
 * worker-local routing algorithms are prepared once in the constructor.
 * Requests share a fixed worker pool and are serialized to bound memory use.
 */
public final class MultiModeRouter implements AutoCloseable {
    public static final Set<String> MODES = Set.of("car", "pt", "walk", "bike");
    private final Map<String, ModeRouter>[] workers;
    private final ThreadLocal<Map<String, ModeRouter>> workerRouters = new ThreadLocal<>();
    private final ExecutorService executor;
    private final ReentrantLock requestLock = new ReentrantLock(true);
    private final CsvMapper csv = new CsvMapper();
    private final int batchSize;
    private final int chunkSize;
    private final double departureTime;
    private final Set<String> availableModes;
    private boolean closed;

    public static final class Settings {
        public String networkPath;
        public String configPath;
        public String eventsPath;
        public int threads = Runtime.getRuntime().availableProcessors();
        public int batchSize = 128;
        public int chunkSize = 65_536;
        public double startTime = 0;
        public double endTime = 86_400;
        public double interval = 900;
        /** Negative means use the CSV departure_time (default zero if absent). */
        public double departureTime = -1;
        public double routingDistanceUtility = 0;
        /** Population-average monetary toll conversion, in currency/hour. */
        public double valueOfTime = 15;
        public boolean returnLinks = false;

        private void validate() {
            if (networkPath == null || networkPath.isBlank()) {
                throw new IllegalArgumentException("networkPath is required");
            }
            if (threads <= 0 || batchSize <= 0 || chunkSize <= 0) {
                throw new IllegalArgumentException("threads, batchSize and chunkSize must be positive");
            }
            if (!Double.isFinite(startTime) || !Double.isFinite(endTime) || !Double.isFinite(interval)
                    || startTime < 0 || interval <= 0 || endTime - startTime < interval) {
                throw new IllegalArgumentException("Events time range must contain at least one positive interval");
            }
            if (!Double.isFinite(departureTime) || !Double.isFinite(routingDistanceUtility)
                    || routingDistanceUtility < 0 || !Double.isFinite(valueOfTime) || valueOfTime <= 0) {
                throw new IllegalArgumentException("Invalid departureTime, routingDistanceUtility or valueOfTime");
            }
        }
    }

    @SuppressWarnings("unchecked")
    public MultiModeRouter(Settings settings) {
        settings.validate();
        batchSize = settings.batchSize;
        chunkSize = settings.chunkSize;
        departureTime = settings.departureTime;
        Network network = TripsRouter.loadNetwork(settings.networkPath);
        Config config = settings.configPath == null ? null
                : ConfigUtils.loadConfig(settings.configPath, new SwissRailRaptorConfigGroup());
        Map<String, java.util.function.Supplier<ModeRouter>> factories = new LinkedHashMap<>();

        RoadNetwork carNetwork = modeNetwork(network, "car");
        TravelTime freeflow = new FreeSpeedTravelTime();
        TravelTime carTime = settings.eventsPath == null ? freeflow
                : TripsRouter.loadTravelTime(settings.eventsPath, carNetwork,
                        settings.startTime, settings.endTime, settings.interval);
        TravelDisutility carBaseCost = new EqasimTravelDisutilityFactory(new ZeroRoutingPenalty(),
                settings.routingDistanceUtility).createTravelDisutility(carTime);
        if (settings.eventsPath != null) {
            carBaseCost = recordedCost(carNetwork, carTime, carBaseCost, settings);
        }
        TravelDisutility carCost = new EqasimTollsTravelDisutility(carBaseCost,
                new Tolls(), new MarginalCostOfTolls(0, settings.valueOfTime));
        factories.put("car", networkFactory(carNetwork, carTime, carCost, settings.returnLinks));

        if (config != null) {
            for (String mode : List.of("walk", "bike")) {
                if (config.routing().getNetworkModes().contains(mode)) {
                    RoadNetwork modeNetwork = modeNetwork(network, mode);
                    TravelTime time;
                    TravelDisutility cost;
                    if (mode.equals("bike")) {
                        time = bikeTime(modeNetwork);
                        cost = bikeCost(modeNetwork, time);
                    } else {
                        var params = config.routing().getTeleportedModeParams().get(mode);
                        double speed = params == null || params.getTeleportedModeSpeed() == null
                                ? new TransitRouterConfig(config).getBeelineWalkSpeed() : params.getTeleportedModeSpeed();
                        positive(speed, "walk speed");
                        time = (link, t, person, vehicle) -> link.getLength() / Math.min(speed, link.getFreespeed(t));
                        cost = new EqasimTravelDisutilityFactory(new ZeroRoutingPenalty(), 0)
                                .createTravelDisutility(time);
                    }
                    factories.put(mode, networkFactory(modeNetwork, time, cost, settings.returnLinks));
                } else {
                    var params = config.routing().getTeleportedModeParams().get(mode);
                    if (params == null) {
                        continue;
                    }
                    double factor = params.getBeelineDistanceFactor();
                    positive(factor, mode + " beeline distance factor");
                    if (params.getTeleportedModeSpeed() != null) {
                        double speed = params.getTeleportedModeSpeed();
                        positive(speed, mode + " teleported speed");
                        factories.put(mode, () -> trip -> {
                            double distance = factor * CoordUtils.calcEuclideanDistance(origin(trip), destination(trip));
                            return result(trip, mode, distance / speed, distance);
                        });
                    } else if (params.getTeleportedModeFreespeedFactor() != null) {
                        double speedFactor = params.getTeleportedModeFreespeedFactor();
                        positive(speedFactor, mode + " freespeed factor");
                        TravelTime cappedFreeflow = (link, t, person, vehicle) -> link.getLength()
                                / Math.min(link.getFreespeed(t), params.getTeleportedModeFreespeedLimit());
                        SpeedyALTFactory factory = new SpeedyALTFactory();
                        TravelDisutility cost = new EqasimTravelDisutilityFactory(new ZeroRoutingPenalty(), 0)
                                .createTravelDisutility(cappedFreeflow);
                        NetworkUtils.getNearestLink(carNetwork, carNetwork.getNodes().values().iterator().next().getCoord());
                        factories.put(mode, () -> {
                            var module = DefaultRoutingModules.createPseudoTransitRouter(mode,
                                    PopulationUtils.getFactory(), carNetwork,
                                    factory.createPathCalculator(carNetwork, cost, cappedFreeflow), params);
                            return planRouter(module::calcRoute, carNetwork, mode);
                        });
                    } else {
                        throw new IllegalArgumentException("No teleportation speed configured for " + mode);
                    }
                }
            }
            if (config.transit().isUseTransit() && config.transit().getTransitScheduleFile() != null) {
                Scenario scenario = ScenarioUtils.createScenario(config);
                // Only load transit data here; RAPTOR receives the already loaded network below.
                new TransitScheduleReader(scenario).readURL(ConfigGroup.getInputFileURL(config.getContext(),
                        config.transit().getTransitScheduleFile()));
                if (config.transit().getVehiclesFile() != null) {
                    new MatsimVehicleReader(scenario.getTransitVehicles()).readURL(ConfigGroup.getInputFileURL(
                            config.getContext(), config.transit().getVehiclesFile()));
                }
                RaptorParameters parameters = config.getModules().containsKey(EqasimRaptorConfigGroup.GROUP_NAME)
                        ? EqasimRaptorUtils.createParameters(config,
                                ConfigUtils.addOrGetModule(config, EqasimRaptorConfigGroup.class),
                                scenario.getTransitSchedule(), true)
                        : RaptorUtils.createParameters(config);
                SwissRailRaptorData data = SwissRailRaptorData.create(scenario.getTransitSchedule(),
                        scenario.getTransitVehicles(), RaptorUtils.createStaticConfig(config), network, new OccupancyData());
                NetworkUtils.getNearestLink(network, network.getNodes().values().iterator().next().getCoord());
                factories.put("pt", () -> {
                    var builder = new SwissRailRaptor.Builder(data, config).with(person -> parameters);
                    if (ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class).isUseRangeQuery()) {
                        builder.with(new ConfigurableRaptorRouteSelector());
                    }
                    return transitRouter(builder.build(), network);
                });
            }
        }
        availableModes = Set.copyOf(factories.keySet());
        workers = new Map[settings.threads];
        // SpeedyALTFactory initializes graphs/landmarks lazily; prepare sequentially to
        // avoid duplicate preprocessing races. Each worker has its own mutable search state.
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new HashMap<>();
            for (var entry : factories.entrySet()) {
                workers[i].put(entry.getKey(), entry.getValue().get());
            }
        }
        AtomicInteger workerIndex = new AtomicInteger();
        var idleWorkers = new ConcurrentLinkedQueue<>(java.util.Arrays.asList(workers));
        executor = Executors.newFixedThreadPool(workers.length, task -> {
            int index = workerIndex.getAndIncrement();
            return new Thread(() -> {
                Map<String, ModeRouter> context = idleWorkers.remove();
                workerRouters.set(context);
                try {
                    task.run();
                } finally {
                    workerRouters.remove();
                    idleWorkers.add(context);
                }
            }, "multi-mode-router-" + index);
        });
        ((ThreadPoolExecutor) executor).prestartAllCoreThreads();
    }

    public Set<String> getAvailableModes() {
        return availableModes;
    }

    public long route(String inputCsv, String outputCsv, String mode) throws IOException, InterruptedException {
        return route(Path.of(inputCsv), Path.of(outputCsv), mode);
    }

    public long route(String inputCsv, String outputCsv) throws IOException, InterruptedException {
        return route(inputCsv, outputCsv, null);
    }

    public long route(Path inputCsv, Path outputCsv) throws IOException, InterruptedException {
        return route(inputCsv, outputCsv, null);
    }

    /** Returns the number of written trips. A failed request leaves existing output intact. */
    public long route(Path inputCsv, Path outputCsv, String mode) throws IOException, InterruptedException {
        requestLock.lockInterruptibly();
        Path temporary = null;
        try {
            if (closed) {
                throw new IllegalStateException("Router is closed");
            }
            if (mode != null) {
                requireMode(mode);
            }
            Path target = outputCsv.toAbsolutePath().normalize();
            if (inputCsv.toAbsolutePath().normalize().equals(target)
                    || (Files.exists(target) && Files.isSameFile(inputCsv, target))) {
                throw new IllegalArgumentException("Input and output CSV must be different files");
            }
            temporary = Files.createTempFile(target.getParent(), ".routing-", ".csv");
            long count = 0;
            CsvSchema outputSchema = csv.typedSchemaFor(RoutedTrip.class).withHeader();
            try (MappingIterator<Map<String, String>> iterator = csv.readerFor(
                    csv.getTypeFactory().constructMapType(Map.class, String.class, String.class))
                    .with(CsvSchema.emptySchema().withHeader()).readValues(inputCsv.toFile());
                    SequenceWriter writer = csv.writerFor(RoutedTrip.class).with(outputSchema)
                            .writeValues(temporary.toFile())) {
                while (iterator.hasNextValue()) {
                    List<Trip> trips = new ArrayList<>(Math.min(chunkSize, 100_000));
                    while (trips.size() < chunkSize && iterator.hasNextValue()) {
                        trips.add(parseTrip(iterator.nextValue(), mode, count + trips.size() + 2));
                    }
                    writer.writeAll(routeChunk(trips));
                    count += trips.size();
                }
            }
            // Jackson writes the header on the first row; handle header-only inputs too.
            if (count == 0) {
                List<String> names = new ArrayList<>();
                outputSchema.forEach(column -> names.add(column.getName()));
                Files.writeString(temporary, String.join(",", names) + "\n");
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            temporary = null;
            return count;
        } finally {
            try {
                if (temporary != null) {
                    Files.deleteIfExists(temporary);
                }
            } finally {
                requestLock.unlock();
            }
        }
    }

    private List<RoutedTrip> routeChunk(List<Trip> trips) throws InterruptedException {
        RoutedTrip[] results = new RoutedTrip[trips.size()];
        AtomicInteger next = new AtomicInteger();
        AtomicBoolean stop = new AtomicBoolean();
        List<Future<?>> tasks = new ArrayList<>();
        int taskCount = Math.min(workers.length, 1 + (trips.size() - 1) / batchSize);
        for (int i = 0; i < taskCount; i++) {
            tasks.add(executor.submit(() -> {
                try {
                    int start;
                    while (!stop.get() && (start = next.getAndAdd(batchSize)) < trips.size()) {
                        int end = (int) Math.min(trips.size(), (long) start + batchSize);
                        for (int j = start; j < end && !stop.get(); j++) {
                            Trip trip = trips.get(j);
                            results[j] = workerRouters.get().get(trip.mode).route(trip);
                        }
                    }
                } catch (RuntimeException | Error e) {
                    stop.set(true);
                    throw e;
                }
            }));
        }
        Throwable failure = null;
        boolean interrupted = false;
        // Drain workers even on failure/interruption before releasing the request and its data.
        for (Future<?> task : tasks) {
            boolean done = false;
            while (!done) {
                try {
                    task.get();
                    done = true;
                } catch (InterruptedException e) {
                    interrupted = true;
                    stop.set(true);
                } catch (ExecutionException e) {
                    if (failure == null) {
                        failure = e.getCause();
                    }
                    done = true;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
            throw new InterruptedException("CSV routing interrupted");
        }
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (failure != null) {
            throw new IllegalStateException("Routing failed", failure);
        }
        return java.util.Arrays.asList(results);
    }

    private Trip parseTrip(Map<String, String> row, String mode, long line) {
        Trip trip = new Trip();
        try {
            trip.identifier = row.getOrDefault("identifier", "");
            trip.originX = number(row, "origin_x");
            trip.originY = number(row, "origin_y");
            trip.destinationX = number(row, "destination_x");
            trip.destinationY = number(row, "destination_y");
            trip.departureTime = departureTime >= 0 ? departureTime
                    : row.containsKey("departure_time") ? number(row, "departure_time") : 0;
            if (trip.departureTime < 0) {
                throw new IllegalArgumentException("departure_time must be non-negative");
            }
            trip.mode = mode == null ? row.get("mode") : mode;
            requireMode(trip.mode);
            return trip;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("CSV row " + line + ": " + e.getMessage(), e);
        }
    }

    private void requireMode(String mode) {
        if (mode == null || !MODES.contains(mode)) {
            throw new IllegalArgumentException("mode must be car, pt, walk or bike");
        }
        if (!availableModes.contains(mode)) {
            throw new IllegalArgumentException("Mode " + mode + " requires a config with routing parameters"
                    + (mode.equals("pt") ? " and an enabled transit schedule" : ""));
        }
    }

    private static double number(Map<String, String> row, String name) {
        String value = row.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing " + name);
        }
        double number = Double.parseDouble(value);
        if (!Double.isFinite(number)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        return number;
    }

    private static void positive(double number, String name) {
        if (!Double.isFinite(number) || number <= 0) {
            throw new IllegalArgumentException(name + " must be positive and finite");
        }
    }

    private static RoadNetwork modeNetwork(Network network, String mode) {
        RoadNetwork filtered = new RoadNetwork(network, Set.of(mode));
        if (filtered.getLinks().isEmpty()) {
            throw new IllegalArgumentException("Network has no links allowing " + mode);
        }
        NetworkUtils.getNearestNode(filtered, filtered.getNodes().values().iterator().next().getCoord());
        return filtered;
    }

    private static java.util.function.Supplier<ModeRouter> networkFactory(Network network, TravelTime time,
            TravelDisutility cost, boolean returnLinks) {
        SpeedyALTFactory factory = new SpeedyALTFactory();
        return () -> {
            LeastCostPathCalculator calculator = factory.createPathCalculator(network, cost, time);
            return trip -> {
                var from = NetworkUtils.getNearestNode(network, origin(trip));
                var to = NetworkUtils.getNearestNode(network, destination(trip));
                var path = calculator.calcLeastCostPath(from, to, trip.departureTime, null, null);
                if (path == null) {
                    throw new IllegalArgumentException("No " + trip.mode + " path for trip " + trip.identifier);
                }
                RoutedTrip result = result(trip, trip.mode, path.travelTime,
                        path.links.stream().mapToDouble(Link::getLength).sum());
                result.accessDistance = CoordUtils.calcEuclideanDistance(origin(trip), from.getCoord());
                result.egressDistance = CoordUtils.calcEuclideanDistance(destination(trip), to.getCoord());
                result.linkCount = path.links.size();
                result.links = returnLinks ? String.join("-", path.links.stream()
                        .map(link -> link.getId().toString()).toList()) : "";
                return result;
            };
        };
    }

    private static int arraySize(Network network) {
        return network.getLinks().keySet().stream().mapToInt(id -> id.index()).max().orElseThrow() + 1;
    }

    private static TravelTime bikeTime(Network network) {
        var speed = new BikeGradientBasedLinkSpeedCalculator((link, t, vehicleId) -> 0);
        double[] times = new double[arraySize(network)];
        for (Link link : network.getLinks().values()) {
            times[link.getId().index()] = link.getLength() / speed.getMaximumBikeVelocity(null, link, 0);
        }
        return (link, t, person, vehicle) -> times[link.getId().index()];
    }

    private static TravelDisutility bikeCost(Network network, TravelTime time) {
        // The network is fixed: cache all gradient/road-type calculations, shared by workers.
        double[] costs = new double[arraySize(network)];
        for (Link link : network.getLinks().values()) {
            double tt = time.getLinkTravelTime(link, 0, null, null);
            costs[link.getId().index()] = tt + BikeDisutilities.linkTypeDisutility(link, tt)
                    + BikeDisutilities.gradientDisutility(link, tt) + BikeDisutilities.distanceDisutility(link, tt);
        }
        return new TravelDisutility() {
            @Override
            public double getLinkTravelDisutility(Link link, double t, Person person, Vehicle vehicle) {
                return costs[link.getId().index()];
            }

            @Override
            public double getLinkMinimumTravelDisutility(Link link) {
                return costs[link.getId().index()];
            }
        };
    }

    private static TravelDisutility recordedCost(Network network, TravelTime time, TravelDisutility delegate,
            Settings settings) {
        // Observed times can be faster than freeflow; ALT must use a valid lower
        // bound over every recorded bin, including the clamped out-of-range bins.
        double[] minimums = new double[arraySize(network)];
        int bins = (int) Math.floor((settings.endTime - settings.startTime) / settings.interval);
        for (Link link : network.getLinks().values()) {
            double minimum = Double.POSITIVE_INFINITY;
            for (int bin = 0; bin < bins; bin++) {
                minimum = Math.min(minimum,
                        time.getLinkTravelTime(link, settings.startTime + (bin + 0.5) * settings.interval, null, null));
            }
            minimums[link.getId().index()] = minimum + settings.routingDistanceUtility * link.getLength() / 10;
        }
        return new TravelDisutility() {
            @Override
            public double getLinkTravelDisutility(Link link, double t, Person person, Vehicle vehicle) {
                return delegate.getLinkTravelDisutility(link, t, person, vehicle);
            }

            @Override
            public double getLinkMinimumTravelDisutility(Link link) {
                return minimums[link.getId().index()];
            }
        };
    }

    private static ModeRouter transitRouter(TransitRouter router, Network network) {
        return planRouter(router::calcRoute, network, "pt");
    }

    private static ModeRouter planRouter(java.util.function.Function<org.matsim.core.router.RoutingRequest,
            List<? extends org.matsim.api.core.v01.population.PlanElement>> router, Network network, String mode) {
        return trip -> {
            var from = new LinkWrapperFacility(NetworkUtils.getNearestLink(network, origin(trip)));
            var to = new LinkWrapperFacility(NetworkUtils.getNearestLink(network, destination(trip)));
            var elements = router.apply(DefaultRoutingRequest.withoutAttributes(from, to, trip.departureTime, null));
            if (elements == null) {
                throw new IllegalArgumentException("No " + mode + " path for trip " + trip.identifier);
            }
            double arrival = trip.departureTime;
            double distance = 0;
            for (var element : elements) {
                if (element instanceof Leg leg) {
                    double start = leg.getDepartureTime().isDefined() ? leg.getDepartureTime().seconds() : arrival;
                    arrival = Math.max(arrival, start) + leg.getTravelTime().seconds();
                    distance += leg.getRoute().getDistance();
                }
            }
            return result(trip, mode, arrival - trip.departureTime, distance);
        };
    }

    private static Coord origin(Trip trip) {
        return new Coord(trip.originX, trip.originY);
    }

    private static Coord destination(Trip trip) {
        return new Coord(trip.destinationX, trip.destinationY);
    }

    private static RoutedTrip result(Trip trip, String mode, double time, double distance) {
        RoutedTrip result = new RoutedTrip();
        result.identifier = trip.identifier;
        result.originX = trip.originX;
        result.originY = trip.originY;
        result.destinationX = trip.destinationX;
        result.destinationY = trip.destinationY;
        result.departureTime = trip.departureTime;
        result.mode = mode;
        result.travelTime = time;
        result.travelDistance = distance;
        result.links = "";
        return result;
    }

    @Override
    public void close() {
        requestLock.lock();
        try {
            closed = true;
            executor.shutdown();
        } finally {
            requestLock.unlock();
        }
    }

    private interface ModeRouter {
        RoutedTrip route(Trip trip);
    }

    private static final class Trip extends TripsRouter.Trip {
        String mode;
    }

    public static final class RoutedTrip extends TripsRouter.RoutedTrip {
        @JsonProperty("mode")
        public String mode;
    }
}
