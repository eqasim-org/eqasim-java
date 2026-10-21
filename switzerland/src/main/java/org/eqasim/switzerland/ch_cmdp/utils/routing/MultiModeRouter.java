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
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.raptor.EqasimRaptorUtils;
import org.eqasim.core.components.traffic.bike.BikeDisutilities;
import org.eqasim.core.components.traffic.bike.BikeGradientBasedLinkSpeedCalculator;
import org.eqasim.core.components.travel_disutility.EqasimTravelDisutilityFactory;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.eqasim.core.scenario.routing.ReusableRoute;
import org.eqasim.core.simulation.policies.routing.ZeroRoutingPenalty;
import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.eqasim.switzerland.ch_cmdp.tolls.EqasimTollsTravelDisutility;
import org.eqasim.switzerland.ch_cmdp.tolls.MarginalCostOfTolls;
import org.eqasim.switzerland.ch_cmdp.tolls.Tolls;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPredictorUtils;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.RunComputeTransitPrices.CSVRequest;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
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
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.LinkWrapperFacility;
import org.eqasim.core.components.routing.speedy.MemoryEfficientSpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.OccupancyData;
import ch.sbb.matsim.routing.pt.raptor.ConfigurableRaptorRouteSelector;
import ch.sbb.matsim.routing.pt.raptor.ModeSpecificTransferCostCalculator;
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
    public static final Set<String> MODES = Set.of("car", "truck", "car_passenger", "pt", "walk", "bike");
    private final Map<String, ModeRouter>[] workers;
    private final ThreadLocal<Map<String, ModeRouter>> workerRouters = new ThreadLocal<>();
    private final ExecutorService executor;
    private final ReentrantLock requestLock = new ReentrantLock(true);
    private final CsvMapper csv = new CsvMapper();
    private final int batchSize;
    private final int chunkSize;
    private final double departureTime;
    private final boolean computeCost;
    private final boolean exportRoutes;
    private final MarouterAssignment marouter;
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
        public double tollsValueOfTime = 15;
        /** Zero keeps toll sensitivity at the population average. */
        public double tollsSigma = 0;
        public boolean returnLinks = false;
        public boolean exportRoutes = false;
        public boolean computeCost = false;
        public String carCostModel = "simple";
        public boolean marouter = false;
        public double marouterIncrement = 0.05;
        public long marouterSeed = 4711;
        public double marouterTruckPcu = 2;
        public double truckMaxSpeed = 80.0 / 3.6;
        /** Null inherits eqasim.sampleSize, then 1. */
        public Double marouterSampleSize;
        public VDFConfigGroup vdfConfig = new VDFConfigGroup();
        Set<String> vdfTimeOverrides = Set.of();
        // CLI options omitted by the caller may inherit values from the config.
        Set<String> configFallbackOptions = Set.of();

        void applyConfigDefaults(Config config) {
            if (config == null) {
                return;
            }
            if (configFallbackOptions.contains("threads")) {
                threads = config.global().getNumberOfThreads();
            }
            if (configFallbackOptions.contains("routingDistanceUtility")) {
                routingDistanceUtility = configValue(config, EqasimConfigGroup.GROUP_NAME,
                        "routingDistanceUtility", routingDistanceUtility);
            }
            if (configFallbackOptions.contains("tolls-value-of-time")) {
                tollsValueOfTime = configValue(config, NetworkCalibrationConfigGroup.GROUP_NAME,
                        "tollsValueOfTime", tollsValueOfTime);
            }
            if (configFallbackOptions.contains("tolls-sigma")) {
                tollsSigma = configValue(config, NetworkCalibrationConfigGroup.GROUP_NAME, "tollsSigma", tollsSigma);
            }
            ConfigGroup routerConfig = config.getModules().get("multiModeRouter");
            if (configFallbackOptions.contains("export-routes") && routerConfig != null
                    && routerConfig.getParams().containsKey("exportRoutes"))
                exportRoutes = booleanValue(routerConfig.getParams().get("exportRoutes"), "exportRoutes");
            if (routerConfig != null) {
                var values = routerConfig.getParams();
                if (configFallbackOptions.contains("marouter") && values.containsKey("marouter"))
                    marouter = booleanValue(values.get("marouter"), "marouter");
                if (configFallbackOptions.contains("marouter-increment"))
                    marouterIncrement = configValue(config, "multiModeRouter", "marouterIncrement", marouterIncrement);
                if (configFallbackOptions.contains("marouter-truck-pcu"))
                    marouterTruckPcu = configValue(config, "multiModeRouter", "marouterTruckPcu", marouterTruckPcu);
                if (configFallbackOptions.contains("truck-max-speed"))
                    truckMaxSpeed = configValue(config, "multiModeRouter", "truckMaxSpeed", truckMaxSpeed);
                if (configFallbackOptions.contains("marouter-sample-size") && values.containsKey("marouterSampleSize"))
                    marouterSampleSize = Double.parseDouble(values.get("marouterSampleSize"));
            }
            if (configFallbackOptions.contains("marouter-seed")) {
                String seed = routerConfig == null ? null : routerConfig.getParams().get("marouterSeed");
                marouterSeed = seed == null ? config.global().getRandomSeed() : Long.parseLong(seed);
            }
            if (configFallbackOptions.contains("compute-cost") && routerConfig != null
                    && routerConfig.getParams().containsKey("computeCost")) {
                computeCost = booleanValue(routerConfig.getParams().get("computeCost"), "computeCost");
            }
            if (configFallbackOptions.contains("car-cost-model")) {
                String model = routerConfig == null ? null : routerConfig.getParams().get("carCostModel");
                if (model != null) {
                    carCostModel = model;
                } else {
                    ConfigGroup eqasim = config.getModules().get(EqasimConfigGroup.GROUP_NAME);
                    if (eqasim != null) {
                        for (ConfigGroup costModel : eqasim.getParameterSets("cost_model")) {
                            if ("car".equals(costModel.getParams().get("mode"))) {
                                String name = costModel.getParams().get("model");
                                carCostModel = "SwissCarCostModel".equals(name) ? "simple"
                                        : "SwissWeissCarCostModel".equals(name) ? "weiss" : name;
                            }
                        }
                    }
                }
            }
        }

        private static double configValue(Config config, String moduleName, String parameter, double fallback) {
            ConfigGroup module = config.getModules().get(moduleName);
            String value = module == null ? null : module.getParams().get(parameter);
            return value == null ? fallback : Double.parseDouble(value);
        }

        private void validate() {
            positive(truckMaxSpeed, "truck-max-speed");
            if (computeCost && (carCostModel == null || !Set.of("simple", "weiss").contains(carCostModel))) {
                throw new IllegalArgumentException("car-cost-model must be simple or weiss");
            }
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
                    || routingDistanceUtility < 0 || !Double.isFinite(tollsValueOfTime) || tollsValueOfTime <= 0
                    || !Double.isFinite(tollsSigma) || tollsSigma < 0) {
                throw new IllegalArgumentException("Invalid departureTime, routingDistanceUtility, tollsValueOfTime or tollsSigma");
            }
        }
    }

    @SuppressWarnings("unchecked")
    public MultiModeRouter(Settings settings) {
        Config config = loadConfig(settings);
        settings.applyConfigDefaults(config);
        settings.validate();
        exportRoutes = settings.exportRoutes;
        batchSize = settings.batchSize;
        chunkSize = settings.chunkSize;
        departureTime = settings.departureTime;
        computeCost = settings.computeCost;
        var costParameters = computeCost ? RouterCosts.parameters(config) : null;
        var carCostEstimator = computeCost ? RouterCosts.carModel(settings.carCostModel, costParameters) : null;
        Network network = TripsRouter.loadNetwork(settings.networkPath);
        boolean useEvents = !settings.marouter && settings.eventsPath != null;
        TravelTime freeflow = new CalibratedFreeSpeedTravelTime(network);
        if (settings.marouter && settings.eventsPath != null) {
            TripsRouter.logger.info("MARouter is enabled; ignoring events file {}", settings.eventsPath);
        }
        if (settings.marouter) {
            VDFConfigGroup vdf = config != null && config.getModules().containsKey(VDFConfigGroup.GROUP_NAME)
                    ? ConfigUtils.addOrGetModule(config, VDFConfigGroup.class) : settings.vdfConfig;
            if (settings.vdfTimeOverrides.contains("start-time")) vdf.setStartTime(settings.startTime);
            if (settings.vdfTimeOverrides.contains("end-time")) vdf.setEndTime(settings.endTime);
            if (settings.vdfTimeOverrides.contains("interval")) vdf.setInterval(settings.interval);
            double sampleSize = settings.marouterSampleSize != null ? settings.marouterSampleSize
                    : config == null ? 1 : Settings.configValue(config, EqasimConfigGroup.GROUP_NAME, "sampleSize", 1);
            marouter = new MarouterAssignment(network, vdf, sampleSize, settings.marouterIncrement,
                    settings.marouterSeed, settings.marouterTruckPcu, settings.truckMaxSpeed, freeflow);
        } else {
            marouter = null;
        }
        Map<String, java.util.function.Supplier<ModeRouter>> factories = new LinkedHashMap<>();

        RoadNetwork carNetwork = modeNetwork(network, "car");
        TravelTime carTime = marouter != null ? marouter.travelTime(false) : !useEvents ? freeflow
                : TripsRouter.loadTravelTime(settings.eventsPath, carNetwork,
                        settings.startTime, settings.endTime, settings.interval);
        TravelDisutility carBaseCost = new EqasimTravelDisutilityFactory(new ZeroRoutingPenalty(),
                settings.routingDistanceUtility).createTravelDisutility(carTime);
        if (useEvents) {
            carBaseCost = recordedCost(carNetwork, carTime, carBaseCost, settings);
        } else {
            carBaseCost = withMinimumCost(carBaseCost, link -> freeflow.getLinkTravelTime(link, 0, null, null)
                    + settings.routingDistanceUtility * link.getLength() / 10);
        }
        TravelDisutility carCost = new EqasimTollsTravelDisutility(carBaseCost,
                new Tolls(), new MarginalCostOfTolls(settings.tollsSigma, settings.tollsValueOfTime));
        var carFactory = networkFactory(carNetwork, carTime, carCost, settings.returnLinks, settings.tollsSigma > 0,
                marouter != null);
        factories.put("car", () -> {
            ModeRouter carRouter = carFactory.get();
            return new ModeRouter() {
                public RoutedTrip route(Trip trip) {
                    RoutedTrip result = carRouter.route(trip);
                    if (trip.includeCost && Double.isFinite(result.travelDistance)) {
                        result.cost = carCostEstimator.applyAsDouble(result.travelDistance / 1000.0);
                    }
                    return result;
                }

                public double cost(Trip trip) {
                    return trip.savedDistance == null ? route(trip).cost
                            : carCostEstimator.applyAsDouble(trip.savedDistance / 1000.0);
                }
            };
        });

        if (network.getLinks().values().stream().anyMatch(link -> link.getAllowedModes().contains("truck"))) {
            Network truckNetwork = modeNetwork(network, "truck");
            TravelTime truckBaseTime = marouter == null ? carTime : marouter.travelTime(true);
            TravelTime truckTime = (link, time, person, vehicle) -> Math.max(
                    truckBaseTime.getLinkTravelTime(link, time, person, vehicle), link.getLength() / settings.truckMaxSpeed);
            TravelDisutility truckBaseCost = new EqasimTravelDisutilityFactory(new ZeroRoutingPenalty(),
                    settings.routingDistanceUtility).createTravelDisutility(truckTime);
            if (useEvents) truckBaseCost = recordedCost(truckNetwork, truckTime, truckBaseCost, settings);
            else truckBaseCost = withMinimumCost(truckBaseCost,
                    link -> Math.max(freeflow.getLinkTravelTime(link, 0, null, null), link.getLength() / settings.truckMaxSpeed)
                            + settings.routingDistanceUtility * link.getLength() / 10);
            TravelDisutility truckCost = new EqasimTollsTravelDisutility(truckBaseCost,
                    new Tolls(), new MarginalCostOfTolls(settings.tollsSigma, settings.tollsValueOfTime));
            var truckFactory = networkFactory(truckNetwork, truckTime, truckCost, settings.returnLinks,
                    settings.tollsSigma > 0, marouter != null);
            factories.put("truck", () -> {
                ModeRouter router = truckFactory.get();
                return new ModeRouter() {
                    public RoutedTrip route(Trip trip) {
                        RoutedTrip result = router.route(trip);
                        if (trip.includeCost) result.cost = Double.NaN; // No Swiss truck monetary cost model.
                        return result;
                    }

                };
            });
        }

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
                        MemoryEfficientSpeedyALTFactory factory = new MemoryEfficientSpeedyALTFactory();
                        TravelDisutility cost = new EqasimTravelDisutilityFactory(new ZeroRoutingPenalty(), 0)
                                .createTravelDisutility(cappedFreeflow);
                        NetworkUtils.getNearestLink(carNetwork, carNetwork.getNodes().values().iterator().next().getCoord());
                        factories.put(mode, () -> {
                            var module = createFreespeedFactorRouter(mode, carNetwork, factory, cost, cappedFreeflow, params);
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
                var pricing = computeCost ? RouterCosts.ptPricing(config, costParameters) : null;
                var raptorConfig = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
                boolean useModeSpecificTransferCosts = !raptorConfig.getModeToModeTransferPenaltyParameterSets().isEmpty();
                factories.put("pt", () -> {
                    var builder = new SwissRailRaptor.Builder(data, config).with(person -> parameters);
                    if (useModeSpecificTransferCosts) {
                        // Match SwissRailRaptorModule: the default calculator deliberately rejects
                        // configs containing mode-to-mode transfer penalties.
                        builder.with(new ModeSpecificTransferCostCalculator());
                    }
                    if (raptorConfig.isUseRangeQuery()) {
                        builder.with(new ConfigurableRaptorRouteSelector());
                    }
                    return planRouter(builder.build()::calcRoute, network, "pt",
                            pricing == null ? null : pricing.pricer(scenario.getTransitSchedule()));
                });
            }
        }
        var modes = new java.util.HashSet<>(factories.keySet());
        modes.add("car_passenger");
        availableModes = Set.copyOf(modes);
        workers = new Map[settings.threads];
        // SpeedyALTFactory initializes graphs/landmarks lazily; prepare sequentially to
        // avoid duplicate preprocessing races. Each worker has its own mutable search state.
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new HashMap<>();
            for (var entry : factories.entrySet()) {
                workers[i].put(entry.getKey(), entry.getValue().get());
            }
            ModeRouter carRouter = workers[i].get("car");
            workers[i].put("car_passenger", trip -> {
                RoutedTrip result = carRouter.route(trip);
                result.mode = "car_passenger";
                if (trip.includeCost && Double.isFinite(result.travelTime)) {
                    result.cost = 0.0;
                }
                return result;
            });
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

    public boolean isCostEnabled() {
        return computeCost;
    }

    public boolean isMarouterEnabled() { return marouter != null; }

    static Config loadConfig(Settings settings) {
        if (settings.configPath == null) {
            return null;
        }
        Config config = ConfigUtils.createConfig(new SwissRailRaptorConfigGroup());
        // Seed only the fallback: reading an explicit global.numberOfThreads replaces it.
        config.global().setNumberOfThreads(settings.threads);
        // Keep Eqasim modules generic here so absent XML parameters retain Settings defaults.
        ConfigUtils.loadConfig(config, settings.configPath);
        config.setContext(org.matsim.core.utils.io.IOUtils.resolveFileOrResource(settings.configPath));
        return config;
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
        return route(inputCsv, outputCsv, mode, computeCost);
    }

    public long route(String inputCsv, String outputCsv, String mode, boolean includeCost) throws IOException, InterruptedException {
        return route(Path.of(inputCsv), Path.of(outputCsv), mode, includeCost);
    }

    public long route(Path inputCsv, Path outputCsv, String mode, boolean includeCost) throws IOException, InterruptedException {
        return processCsv(inputCsv, outputCsv, mode, includeCost, false);
    }

    public long cost(String inputCsv, String outputCsv, String mode) throws IOException, InterruptedException {
        return cost(Path.of(inputCsv), Path.of(outputCsv), mode);
    }

    public long cost(String inputCsv, String outputCsv) throws IOException, InterruptedException {
        return cost(inputCsv, outputCsv, null);
    }

    public long cost(Path inputCsv, Path outputCsv) throws IOException, InterruptedException {
        return cost(inputCsv, outputCsv, null);
    }

    /** Price saved routes directly, or route raw OD requests and write only costs. */
    public long cost(Path inputCsv, Path outputCsv, String mode) throws IOException, InterruptedException {
        return processCsv(inputCsv, outputCsv, mode, true, true);
    }

    private long processCsv(Path inputCsv, Path outputCsv, String mode, boolean includeCost, boolean costOnly)
            throws IOException, InterruptedException {
        requestLock.lockInterruptibly();
        Path temporary = null;
        try {
            if (closed) {
                throw new IllegalStateException("Router is closed");
            }
            if (includeCost && !computeCost) {
                throw new IllegalArgumentException("Cost estimation is disabled; start the server with --compute-cost true");
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
            Class<?> resultType = costOnly ? CostResult.class : RoutedTrip.class;
            CsvSchema outputSchema = csv.typedSchemaFor(resultType).withHeader();
            try (MappingIterator<Map<String, String>> iterator = csv.readerFor(
                    csv.getTypeFactory().constructMapType(Map.class, String.class, String.class))
                    .with(CsvSchema.emptySchema().withHeader()).readValues(inputCsv.toFile());
                    SequenceWriter writer = csv.writerFor(resultType).with(outputSchema)
                            .writeValues(temporary.toFile())) {
                if (marouter != null) {
                    count = assignCsv(iterator, writer, mode, includeCost, costOnly, target.getParent());
                } else while (iterator.hasNextValue()) {
                    List<Trip> trips = new ArrayList<>(Math.min(chunkSize, 100_000));
                    while (trips.size() < chunkSize && iterator.hasNextValue()) {
                        trips.add(parseTrip(iterator.nextValue(), mode, count + trips.size() + 2, includeCost, costOnly));
                    }
                    List<RoutedTrip> results = routeChunk(trips, costOnly);
                    if (costOnly) {
                        for (RoutedTrip result : results) {
                            writer.write(new CostResult(result.identifier, result.mode, result.cost));
                        }
                    } else {
                        writer.writeAll(results);
                    }
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

    private long assignCsv(MappingIterator<Map<String, String>> iterator, SequenceWriter writer, String mode,
            boolean includeCost, boolean costOnly, Path parent) throws IOException, InterruptedException {
        try (var spool = new AssignmentSpool(parent)) {
            long count = 0;
            List<Trip> ordinary = new ArrayList<>();
            List<Long> ordinaryIndices = new ArrayList<>();
            while (iterator.hasNextValue()) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Assignment interrupted");
                Map<String, String> row = iterator.nextValue();
                Trip trip = parseTrip(row, mode, count + 2, includeCost, costOnly);
                boolean road = trip.mode.equals("car") || trip.mode.equals("truck") || trip.mode.equals("car_passenger");
                boolean defer = road && (!costOnly || trip.mode.equals("car") && trip.savedDistance == null);
                if (defer) {
                    spool.add(marouter.bin(trip.departureTime), trip.mode.equals("car_passenger"), count, row);
                } else {
                    ordinary.add(trip);
                    ordinaryIndices.add(count);
                    if (ordinary.size() == chunkSize) {
                        storeChunk(spool, ordinaryIndices, ordinary, costOnly);
                        ordinary.clear(); ordinaryIndices.clear();
                    }
                }
                count++;
            }
            if (!ordinary.isEmpty()) storeChunk(spool, ordinaryIndices, ordinary, costOnly);
            for (var entry : spool.bins.entrySet()) {
                marouter.begin(entry.getKey());
                AssignmentSpool.Bin bin = entry.getValue();
                TripsRouter.logger.info("MARouter departure bin {}: {} drivers, {} passengers", entry.getKey(),
                        bin.drivers.count, bin.passengers.count);
                long[] order = bin.drivers.shuffled(marouter.seed ^ ((long) entry.getKey() * 0x9e3779b97f4a7c15L));
                int rounds = (int) Math.ceil(1 / marouter.increment);
                int start = 0;
                for (int round = 0; round < rounds; round++) {
                    int end = round == rounds - 1 ? order.length
                            : (int) Math.ceil(Math.min(1, (round + 1) * marouter.increment) * order.length);
                    int assigned = start;
                    while (start < end) {
                        List<Trip> trips = new ArrayList<>();
                        List<Long> indices = new ArrayList<>();
                        int until = (int) Math.min(end, (long) start + chunkSize);
                        for (; start < until; start++) {
                            var input = bin.drivers.at(order[start], AssignmentSpool.Input.class);
                            trips.add(parseTrip(input.row(), mode, input.index() + 2, includeCost, costOnly));
                            indices.add(input.index());
                        }
                        // Cost-only raw car requests still need actual assigned paths, not a projected cost result.
                        List<RoutedTrip> results = routeChunk(trips, false);
                        for (int i = 0; i < results.size(); i++) {
                            RoutedTrip result = results.get(i);
                            boolean truck = trips.get(i).mode.equals("truck");
                            marouter.add(result.assignmentPath, truck);
                            spool.put(indices.get(i), result, truck);
                        }
                    }
                    // No workers are running: publish the next immutable-for-search travel-time snapshot.
                    if (start > assigned) marouter.update();
                }
                // Every route, including the first increment, reports the final full-demand path time.
                for (long i = 0; i < bin.drivers.count; i++) {
                    var input = bin.drivers.row(i, AssignmentSpool.Input.class);
                    var output = spool.get(input.index());
                    output.result().travelTime = marouter.finalTime(output.path(), output.truck());
                    if (output.result().route != null)
                        output.result().route = ReusableRoute.withTravelTime(output.result().route, output.result().travelTime);
                    spool.put(input.index(), output.result(), output.truck());
                }
                // Passengers see final road times but never add another vehicle to demand.
                for (long i = 0; i < bin.passengers.count;) {
                    List<Trip> trips = new ArrayList<>();
                    List<Long> indices = new ArrayList<>();
                    for (; i < bin.passengers.count && trips.size() < chunkSize; i++) {
                        var input = bin.passengers.row(i, AssignmentSpool.Input.class);
                        trips.add(parseTrip(input.row(), mode, input.index() + 2, includeCost, costOnly));
                        indices.add(input.index());
                    }
                    storeChunk(spool, indices, trips, false);
                }
                marouter.reset();
            }
            for (long i = 0; i < count; i++) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Assignment interrupted");
                RoutedTrip result = spool.get(i).result();
                if (costOnly) writer.write(new CostResult(result.identifier, result.mode, result.cost));
                else writer.write(result);
            }
            return count;
        } finally {
            // Success, failed input, cancellation and client errors must not contaminate another request.
            marouter.reset();
        }
    }

    private void storeChunk(AssignmentSpool spool, List<Long> indices, List<Trip> trips, boolean costOnly)
            throws IOException, InterruptedException {
        var results = routeChunk(trips, costOnly);
        for (int i = 0; i < results.size(); i++) spool.put(indices.get(i), results.get(i), trips.get(i).mode.equals("truck"));
    }

    private List<RoutedTrip> routeChunk(List<Trip> trips, boolean costOnly) throws InterruptedException {
        RoutedTrip[] results = new RoutedTrip[trips.size()];
        AtomicInteger next = new AtomicInteger();
        AtomicBoolean stop = new AtomicBoolean();
        int batch = TripsRouter.routingBatchSize(trips.size(), workers.length, batchSize);
        List<Future<?>> tasks = new ArrayList<>();
        int taskCount = Math.min(workers.length, 1 + (trips.size() - 1) / batch);
        for (int i = 0; i < taskCount; i++) {
            tasks.add(executor.submit(() -> {
                try {
                    int start;
                    while (!stop.get() && (start = next.getAndAdd(batch)) < trips.size()) {
                        int end = (int) Math.min(trips.size(), (long) start + batch);
                        for (int j = start; j < end && !stop.get(); j++) {
                            Trip trip = trips.get(j);
                            if (costOnly && trip.savedDistance != null && Double.isNaN(trip.savedDistance)) {
                                results[j] = missingRoute(trip);
                            } else if (costOnly) {
                                results[j] = result(trip, trip.mode, 0, 0);
                                results[j].cost = trip.mode.equals("car") || trip.mode.equals("pt")
                                        ? workerRouters.get().get(trip.mode).cost(trip)
                                        : trip.mode.equals("truck") ? Double.NaN : 0.0;
                            } else {
                                results[j] = workerRouters.get().get(trip.mode).route(trip);
                                if (trip.exportRoute && results[j].route == null && Double.isFinite(results[j].travelTime)
                                        && (trip.mode.equals("walk") || trip.mode.equals("bike"))) {
                                    var factory = PopulationUtils.getFactory();
                                    var leg = factory.createLeg(trip.mode);
                                    var route = factory.getRouteFactories().createRoute(org.matsim.api.core.v01.population.Route.class,
                                            trip.originLink == null ? null : Id.createLinkId(trip.originLink),
                                            trip.destinationLink == null ? null : Id.createLinkId(trip.destinationLink));
                                    route.setDistance(results[j].travelDistance); route.setTravelTime(results[j].travelTime);
                                    leg.setRoute(route); leg.setDepartureTime(trip.departureTime); leg.setTravelTime(results[j].travelTime);
                                    results[j].route = ReusableRoute.encode(trip.mode, trip.departureTime, List.of(leg));
                                }
                            }
                            if (trip.includeCost && !trip.mode.equals("car") && !trip.mode.equals("pt") && !trip.mode.equals("truck")
                                    && Double.isFinite(results[j].travelTime)) {
                                results[j].cost = 0.0;
                            }
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

    private Trip parseTrip(Map<String, String> row, String mode, long line, boolean includeCost, boolean costOnly) {
        Trip trip = new Trip();
        try {
            trip.identifier = row.getOrDefault("identifier", "");
            trip.personId = optional(row, "person_id");
            trip.tripId = optional(row, "trip_id");
            trip.tripIndex = optionalIndex(row, "trip_index");
            trip.originLink = optional(row, "origin_link_id");
            trip.destinationLink = optional(row, "destination_link_id");
            if ((trip.originLink == null) != (trip.destinationLink == null))
                throw new IllegalArgumentException("Provide both origin_link_id and destination_link_id");
            trip.exportRoute = exportRoutes && !costOnly;
            trip.mode = mode == null ? row.get("mode") : mode;
            trip.includeCost = includeCost;
            trip.costOnly = costOnly;
            // A saved unreachable route is also unpriceable, for every mode.
            if (costOnly && MODES.contains(trip.mode == null ? "" : trip.mode)
                    && "NaN".equals(row.get("travel_distance"))) {
                trip.savedDistance = Double.NaN;
                return trip;
            }
            if (costOnly && trip.mode != null && MODES.contains(trip.mode)
                    && !trip.mode.equals("car") && !trip.mode.equals("pt")) {
                return trip;
            }
            requireMode(trip.mode);
            if (costOnly) {
                if (!trip.mode.equals("car") && !trip.mode.equals("pt")) {
                    return trip;
                }
                if (trip.mode.equals("car") && row.containsKey("travel_distance") && !row.get("travel_distance").isBlank()) {
                    trip.savedDistance = number(row, "travel_distance");
                    if (trip.savedDistance < 0) {
                        throw new IllegalArgumentException("travel_distance must be non-negative");
                    }
                    return trip;
                }
                if (trip.mode.equals("pt") && row.containsKey("pt_cost_data") && !row.get("pt_cost_data").isBlank()) {
                    trip.savedPtRoute = RouterCosts.read(row.get("pt_cost_data"));
                    if (trip.savedPtRoute == null) {
                        throw new IllegalArgumentException("pt_cost_data must be a JSON object");
                    }
                    return trip;
                }
            }
            trip.originX = number(row, "origin_x");
            trip.originY = number(row, "origin_y");
            trip.destinationX = number(row, "destination_x");
            trip.destinationY = number(row, "destination_y");
            trip.departureTime = departureTime >= 0 ? departureTime
                    : row.containsKey("departure_time") ? number(row, "departure_time") : 0;
            if (trip.departureTime < 0) {
                throw new IllegalArgumentException("departure_time must be non-negative");
            }
            if (includeCost && trip.mode.equals("pt")) {
                int age = row.containsKey("age") && !row.get("age").isBlank() ? Integer.parseInt(row.get("age")) : 30;
                if (age < 0) {
                    throw new IllegalArgumentException("age must be non-negative");
                }
                // Use the same subscription attribute and decoder as mode choice.
                // Legacy boolean columns remain supported when subscriptions is absent.
                boolean canonical = row.containsKey("subscriptions");
                String subscriptions = row.get("subscriptions");
                boolean ga = canonical ? SwissPredictorUtils.hasGeneralSubscription(subscriptions) : flag(row, "hasGA");
                boolean halfFare = canonical ? SwissPredictorUtils.hasHalbtaxSubscription(subscriptions)
                        : flag(row, "hasHalbtaxSubscription");
                boolean regional = canonical ? SwissPredictorUtils.hasRegionalSubscription(subscriptions)
                        : flag(row, "hasVerbundAbo") || flag(row, "hasStreckenAbo");
                boolean junior = canonical ? SwissPredictorUtils.hasJuniorSubscription(subscriptions) : flag(row, "hasJuniorAbo");
                boolean gleis7 = canonical ? SwissPredictorUtils.hasGleis7Subscription(subscriptions) : flag(row, "hasGleis7Abo");
                double homeX = regional ? number(row, row.containsKey("home_x") ? "home_x" : "homeX") : trip.originX;
                double homeY = regional ? number(row, row.containsKey("home_y") ? "home_y" : "homeY") : trip.originY;
                trip.pricingRequest = new CSVRequest(trip.identifier, trip.originX, trip.originY, trip.destinationX,
                        trip.destinationY, trip.departureTime, homeX, homeY, ga, halfFare,
                        canonical ? regional : flag(row, "hasVerbundAbo"),
                        !canonical && flag(row, "hasStreckenAbo"), gleis7, junior, age);
            }
            return trip;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("CSV row " + line + ": " + e.getMessage(), e);
        }
    }

    private void requireMode(String mode) {
        if (mode == null || !MODES.contains(mode)) {
            throw new IllegalArgumentException("mode must be car, truck, car_passenger, pt, walk or bike");
        }
        if (!availableModes.contains(mode)) {
            if (mode.equals("truck")) throw new IllegalArgumentException("Truck routing requires network links allowing truck");
            throw new IllegalArgumentException("Mode " + mode + " requires a config with routing parameters"
                    + (mode.equals("pt") ? " and an enabled transit schedule" : ""));
        }
    }

    private static String optional(Map<String, String> row, String name) {
        String value = row.get(name);
        return value == null || value.isBlank() ? null : value;
    }

    private static Integer optionalIndex(Map<String, String> row, String name) {
        String value = optional(row, name);
        if (value == null) return null;
        int index = Integer.parseInt(value);
        if (index < 0) throw new IllegalArgumentException(name + " must be non-negative (zero-based)");
        return index;
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

    static boolean booleanValue(String value, String name) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException(name + " must be true or false");
    }

    private static boolean flag(Map<String, String> row, String name) {
        String value = row.get(name);
        return value != null && !value.isBlank() && booleanValue(value, name);
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
        return networkFactory(network, time, cost, returnLinks, false);
    }

    private static java.util.function.Supplier<ModeRouter> networkFactory(Network network, TravelTime time,
            TravelDisutility cost, boolean returnLinks, boolean useTollSensitivity) {
        return networkFactory(network, time, cost, returnLinks, useTollSensitivity, false);
    }

    private static java.util.function.Supplier<ModeRouter> networkFactory(Network network, TravelTime time,
            TravelDisutility cost, boolean returnLinks, boolean useTollSensitivity, boolean capturePath) {
        MemoryEfficientSpeedyALTFactory factory = new MemoryEfficientSpeedyALTFactory();
        VehicleType vehicleType = useTollSensitivity
                ? VehicleUtils.createVehicleType(Id.create("csv-car", VehicleType.class)) : null;
        return () -> {
            LeastCostPathCalculator calculator = factory.createPathCalculator(network, cost, time);
            return trip -> {
                var from = NetworkUtils.getNearestNode(network, origin(trip));
                var to = NetworkUtils.getNearestNode(network, destination(trip));
                Vehicle vehicle = null;
                if (useTollSensitivity) {
                    String identifier = trip.identifier == null || trip.identifier.isBlank()
                            ? "csv:" + trip.originX + ":" + trip.originY + ":" + trip.destinationX + ":"
                                    + trip.destinationY + ":" + trip.departureTime
                            : trip.identifier;
                    vehicle = VehicleUtils.createVehicle(Id.createVehicleId(identifier), vehicleType);
                }
                LeastCostPathCalculator.Path path;
                List<Link> links;
                double pathTime;
                if (trip.originLink != null) {
                    Link first = network.getLinks().get(Id.createLinkId(trip.originLink));
                    Link last = network.getLinks().get(Id.createLinkId(trip.destinationLink));
                    if (first == null || last == null) throw new IllegalArgumentException("Endpoint links must allow " + trip.mode);
                    links = new ArrayList<>(); links.add(first);
                    if (!first.getId().equals(last.getId())) {
                        path = calculator.calcLeastCostPath(first, last, trip.departureTime
                                + time.getLinkTravelTime(first, trip.departureTime, null, vehicle), null, vehicle);
                        if (path == null) return missingRoute(trip);
                        links.addAll(path.links); links.add(last);
                    }
                    pathTime = 0;
                    for (Link link : links) pathTime += time.getLinkTravelTime(link, trip.departureTime + pathTime, null, vehicle);
                } else {
                    path = calculator.calcLeastCostPath(from, to, trip.departureTime, null, vehicle);
                    if (path == null) return missingRoute(trip);
                    links = path.links; pathTime = path.travelTime;
                }
                RoutedTrip result = result(trip, trip.mode, pathTime,
                        links.stream().mapToDouble(Link::getLength).sum());
                result.accessDistance = CoordUtils.calcEuclideanDistance(origin(trip), from.getCoord());
                result.egressDistance = CoordUtils.calcEuclideanDistance(destination(trip), to.getCoord());
                result.linkCount = links.size();
                if (capturePath) result.assignmentPath = links.stream().mapToInt(link -> link.getId().index()).toArray();
                result.links = returnLinks ? String.join("-", links.stream()
                        .map(link -> link.getId().toString()).toList()) : "";
                if (trip.exportRoute && !links.isEmpty()) {
                    var ids = links.stream().map(Link::getId).toList();
                    var route = RouteUtils.createLinkNetworkRouteImpl(ids.getFirst(), ids.size() < 3 ? List.of()
                            : ids.subList(1, ids.size() - 1), ids.getLast());
                    route.setTravelTime(result.travelTime); route.setDistance(result.travelDistance);
                    var leg = PopulationUtils.getFactory().createLeg(trip.mode);
                    leg.setRoute(route); leg.setDepartureTime(trip.departureTime); leg.setTravelTime(result.travelTime);
                    result.route = ReusableRoute.encode(trip.mode, trip.departureTime, List.of(leg));
                }
                return result;
            };
        };
    }

    private static int arraySize(Network network) {
        return network.getLinks().keySet().stream().mapToInt(id -> id.index()).max().orElseThrow() + 1;
    }

    private static TravelTime bikeTime(Network network) {
        var speed = new BikeGradientBasedLinkSpeedCalculator((link, t, vehicleId) -> 0);
        float[] times = new float[arraySize(network)];
        for (Link link : network.getLinks().values()) {
            times[link.getId().index()] = (float) (link.getLength() / speed.getMaximumBikeVelocity(null, link, 0));
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
        return withMinimumCost(delegate, link -> minimums[link.getId().index()]);
    }

    private static TravelDisutility withMinimumCost(TravelDisutility delegate,
            java.util.function.ToDoubleFunction<Link> minimum) {
        return new TravelDisutility() {
            @Override
            public double getLinkTravelDisutility(Link link, double t, Person person, Vehicle vehicle) {
                return delegate.getLinkTravelDisutility(link, t, person, vehicle);
            }

            @Override
            public double getLinkMinimumTravelDisutility(Link link) {
                return minimum.applyAsDouble(link);
            }
        };
    }

    private static ModeRouter planRouter(java.util.function.Function<org.matsim.core.router.RoutingRequest,
            List<? extends org.matsim.api.core.v01.population.PlanElement>> router, Network network, String mode) {
        return planRouter(router, network, mode, null);
    }

    private static ModeRouter planRouter(java.util.function.Function<org.matsim.core.router.RoutingRequest,
            List<? extends org.matsim.api.core.v01.population.PlanElement>> router, Network network, String mode,
            RouterCosts.PtPricer pricer) {
        ModeRouter routing = trip -> {
            var from = new LinkWrapperFacility(NetworkUtils.getNearestLink(network, origin(trip)));
            var to = new LinkWrapperFacility(NetworkUtils.getNearestLink(network, destination(trip)));
            List<? extends org.matsim.api.core.v01.population.PlanElement> elements;
            try {
                elements = router.apply(DefaultRoutingRequest.withoutAttributes(from, to, trip.departureTime, null));
            } catch (NoPath e) {
                return missingRoute(trip);
            }
            if (elements == null || elements.isEmpty()) return missingRoute(trip);
            // RAPTOR can return a direct-walk fallback when no transit journey is
            // available. Keep zero-distance requests valid, but do not report this
            // fallback as a successful PT journey.
            if (mode.equals("pt") && !origin(trip).equals(destination(trip))
                    && elements.stream().noneMatch(e -> e instanceof Leg leg && leg.getMode().equals("pt"))) {
                return missingRoute(trip);
            }
            double arrival = trip.departureTime;
            double distance = 0;
            for (var element : elements) {
                if (element instanceof Leg leg) {
                    double start = leg.getDepartureTime().isDefined() ? leg.getDepartureTime().seconds() : arrival;
                    if (leg.getTravelTime().isUndefined() || leg.getRoute() == null) return missingRoute(trip);
                    arrival = Math.max(arrival, start) + leg.getTravelTime().seconds();
                    distance += leg.getRoute().getDistance();
                }
            }
            if (!Double.isFinite(arrival) || !Double.isFinite(distance)) return missingRoute(trip);
            RoutedTrip result = result(trip, mode, arrival - trip.departureTime, distance);
            if (trip.exportRoute) result.route = ReusableRoute.encode(trip.mode, trip.departureTime, elements);
            if (mode.equals("pt") && trip.includeCost && !trip.costOnly) {
                result.ptCostData = RouterCosts.save(trip.pricingRequest, elements);
            }
            if (trip.includeCost && mode.equals("pt")) {
                result.cost = pricer.price(trip.pricingRequest, elements);
            }
            return result;
        };
        return new ModeRouter() {
            public RoutedTrip route(Trip trip) {
                return routing.route(trip);
            }

            public double cost(Trip trip) {
                return trip.savedPtRoute == null ? route(trip).cost : pricer.price(trip.savedPtRoute);
            }
        };
    }

    private static final class NoPath extends RuntimeException { }

    /** Preserve link-based routing (and its turn restrictions) when detecting failures. */
    private static LeastCostPathCalculator requirePath(LeastCostPathCalculator delegate) {
        return new LeastCostPathCalculator() {
            private Path check(Path path) {
                if (path == null) throw new NoPath();
                return path;
            }
            @Override public Path calcLeastCostPath(org.matsim.api.core.v01.network.Node from,
                    org.matsim.api.core.v01.network.Node to, double time, Person person, Vehicle vehicle) {
                return check(delegate.calcLeastCostPath(from, to, time, person, vehicle));
            }
            @Override public Path calcLeastCostPath(Link from, Link to, double time, Person person, Vehicle vehicle) {
                return check(delegate.calcLeastCostPath(from, to, time, person, vehicle));
            }
        };
    }

    private static RoutedTrip missingRoute(Trip trip) {
        RoutedTrip result = result(trip, trip.mode, Double.NaN, Double.NaN);
        result.accessDistance = Double.NaN;
        result.egressDistance = Double.NaN;
        result.linkCount = -1;
        result.cost = trip.includeCost ? Double.NaN : null;
        return result;
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
        result.personId = trip.personId;
        result.tripId = trip.tripId; result.tripIndex = trip.tripIndex;
        result.originLink = trip.originLink; result.destinationLink = trip.destinationLink;
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

        default double cost(Trip trip) {
            return route(trip).cost;
        }
    }

    private static final class Trip extends TripsRouter.Trip {
        String personId, tripId, originLink, destinationLink;
        Integer tripIndex;
        boolean exportRoute;
        String mode;
        CSVRequest pricingRequest;
        boolean includeCost;
        boolean costOnly;
        Double savedDistance;
        RouterCosts.SavedPtRoute savedPtRoute;
    }

    public record CostResult(String identifier, String mode, double cost) {}

    public static final class RoutedTrip extends TripsRouter.RoutedTrip {
        @JsonProperty("person_id") public String personId;
        @JsonProperty("trip_id") public String tripId;
        @JsonProperty("trip_index") public Integer tripIndex;
        @JsonProperty("origin_link_id") public String originLink;
        @JsonProperty("destination_link_id") public String destinationLink;
        @JsonProperty("route") public String route;
        @JsonProperty("mode")
        public String mode;
        /** Estimated monetary cost in CHF; blank when cost computation is disabled. */
        @JsonProperty("cost")
        public Double cost;
        /** Saved itinerary and traveler data for later PT fare calculation. */
        @JsonProperty("pt_cost_data")
        public String ptCostData;
        @com.fasterxml.jackson.annotation.JsonIgnore
        int[] assignmentPath;
    }

    /** Keep unreachable-path handling when using MATSim's freespeed-factor router. */
    private static org.matsim.core.router.RoutingModule createFreespeedFactorRouter(String mode,
            org.matsim.api.core.v01.network.Network network, MemoryEfficientSpeedyALTFactory factory,
            TravelDisutility cost, TravelTime time, org.matsim.core.config.groups.RoutingConfigGroup.TeleportedModeParams params) {
        org.matsim.core.router.util.LeastCostPathCalculatorFactory checked =
                (net, disutility, travelTime) -> requirePath(factory.createPathCalculator(net, disutility, travelTime));
        return DefaultRoutingModules.createFreespeedFactorRouter(mode, PopulationUtils.getFactory(), network,
                checked, time, cost, params);
    }
}
