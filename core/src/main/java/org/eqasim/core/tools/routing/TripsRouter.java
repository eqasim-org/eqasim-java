package org.eqasim.core.tools.routing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.travel_time.RecordedTravelTime;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.matsim.core.config.CommandLine;
import java.net.URL;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.router.*;
import org.matsim.core.router.util.*;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TripsRouter {
    public static final Logger logger = LogManager.getLogger(TripsRouter.class);

    public static final Collection<String> REQUIRED_ARGS = Set.of("config-path", "events-path", "trips-path");
    public static final Collection<String> OPTIONAL_ARGS = Set.of("threads","start-time","end-time","interval","output-path", "departure-time");

    public static void main(String[] args) throws Exception {
        CommandLine cmd = new CommandLine.Builder(args)
                .requireOptions(REQUIRED_ARGS)
                .allowOptions(OPTIONAL_ARGS)
                .build();

        int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
                .orElse(Runtime.getRuntime().availableProcessors());

        // Read trips
        List<Trip> trips = readTrips(cmd.getOptionStrict("trips-path"));

        // if departure time is provided, give all trips that departure time
        double departureTime = Double.parseDouble(cmd.getOption("departure-time").orElse("-1"));
        if (departureTime >= 0) {
            for (Trip trip : trips) {
                trip.departureTime = departureTime;
            }
            logger.info("All trips set to departure time: {} seconds", departureTime);
        }

        // Prepare network
        Network network = loadNetwork(cmd.getOptionStrict("config-path"), cmd);
        RoadNetwork roadNetwork = new RoadNetwork(network);

        // Load travel time
        RecordedTravelTime travelTime = loadTravelTime(cmd, roadNetwork);

        // ROUTING SECTION
        List<RoutedTrip> routedTrips = routeTrips(trips, roadNetwork, travelTime, numberOfThreads);
        // Write results
        String outputPath = cmd.getOption("output-path").orElse("routed_trips.csv");
        writeRoutedTrips(outputPath, routedTrips);
        logger.info("Routing completed: {} trips written to {}", routedTrips.size(), outputPath);
    }

    // === LOAD NETWORK FROM CONFIG ===
    public static Network loadNetwork(String configPath, CommandLine cmd) throws CommandLine.ConfigurationException {
        Config config = ConfigUtils.loadConfig(configPath);
        cmd.applyConfiguration(config);
        URL networkPath = config.network().getInputFileURL(config.getContext());

        Network network = NetworkUtils.readNetwork(networkPath.getPath(), config);
        logger.info("Loaded network: {} links, {} nodes.", network.getLinks().size(), network.getNodes().size());
        return network;
    }

    // === LOAD RECORDED TRAVEL TIME FROM EVENTS ===
    public static RecordedTravelTime loadTravelTime(CommandLine cmd, RoadNetwork roadNetwork) throws IOException, CommandLine.ConfigurationException {
        double startTime = Double.parseDouble(cmd.getOption("start-time").orElse("0.0"));
        double endTime = Double.parseDouble(cmd.getOption("end-time").orElse(String.valueOf(24 * 3600.0)));
        double interval = Double.parseDouble(cmd.getOption("interval").orElse("900.0"));
        File eventsFile = new File(cmd.getOptionStrict("events-path"));
        return RecordedTravelTime.readFromEvents(eventsFile, roadNetwork, startTime, endTime, interval);
    }

    // === ROUTING TRIPS IN PARALLEL ===
    private static List<RoutedTrip> routeTrips(List<Trip> trips, Network network, TravelTime travelTime, int threads)
            throws InterruptedException, ExecutionException {

        TravelDisutilityFactory disutilityFactory = new OnlyTimeDependentTravelDisutilityFactory();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<RoutedTrip>> futures = new ArrayList<>();
        AtomicInteger completed = new AtomicInteger(0);
        int total = trips.size();
        int progressInterval = Math.max(1, total / 20); // Log progress every 5%

        for (Trip trip : trips) {
            futures.add(executor.submit(() -> {
                RoutedTrip routed = routeTrip(trip, network, travelTime, disutilityFactory);
                int progress = completed.incrementAndGet();
                if (progress % progressInterval == 0 || progress == total) {
                    logger.info("Progress ({}%): {}/{} trips routed.", (progress * 100 / total), progress, total);
                }
                return routed;
            }));
        }

        List<RoutedTrip> results = new ArrayList<>(total);
        for (Future<RoutedTrip> f : futures) {
            results.add(f.get());
        }

        executor.shutdown();
        return results;
    }

    // === ROUTING FOR ONE TRIP ===
    public static RoutedTrip routeTrip(Trip trip, Network network, TravelTime travelTime, TravelDisutilityFactory disutilityFactory) {
        Coord fromCoord = new Coord(trip.originX, trip.originY);
        Coord toCoord = new Coord(trip.destinationX, trip.destinationY);

        Node fromNode = NetworkUtils.getNearestNode(network, fromCoord);
        Node toNode = NetworkUtils.getNearestNode(network, toCoord);

        TravelDisutility disutility = disutilityFactory.createTravelDisutility(travelTime);
        LeastCostPathCalculator router = new DijkstraFactory().createPathCalculator(network, disutility, travelTime); //SpeedyALTFactory, DijkstraFactory
        // it seems that using fromLink and toLink uses the nodes afterall, so we can directly use nodes here
        LeastCostPathCalculator.Path path = router.calcLeastCostPath(fromNode, toNode, trip.departureTime, null, null);

        // Extract results
        double travelTimeSeconds = path.travelTime;
        double travelDistanceMeters = path.links.stream().mapToDouble(Link::getLength).sum();
        double access_distance = CoordUtils.calcEuclideanDistance(fromCoord,fromNode.getCoord());
        double egress_distance = CoordUtils.calcEuclideanDistance(toCoord,toNode.getCoord());
        return new RoutedTrip(trip.identifier, trip.originX, trip.originY,
                              trip.destinationX, trip.destinationY, trip.departureTime,
                              travelTimeSeconds, travelDistanceMeters, access_distance,
                              egress_distance, path.links.size());
    }

    // === DOMAIN CLASSES ===
    public static class Trip {
        @JsonProperty("identifier") public String identifier;
        @JsonProperty("origin_x") public double originX;
        @JsonProperty("origin_y") public double originY;
        @JsonProperty("destination_x") public double destinationX;
        @JsonProperty("destination_y") public double destinationY;
        @JsonProperty("departure_time") public double departureTime;
    }

    public static class RoutedTrip {
        @JsonProperty("identifier") public String identifier;
        @JsonProperty("origin_x") public double originX;
        @JsonProperty("origin_y") public double originY;
        @JsonProperty("destination_x") public double destinationX;
        @JsonProperty("destination_y") public double destinationY;
        @JsonProperty("departure_time") public double departureTime;
        @JsonProperty("travel_time") public double travelTime;
        @JsonProperty("travel_distance") public double travelDistance;
        @JsonProperty("access_distance") public double accessDistance;
        @JsonProperty("egress_distance") public double egressDistance;
        @JsonProperty("link_count") public int linkCount;

        public RoutedTrip() {}

        public RoutedTrip(String id, double ox, double oy, double dx, double dy, double dep, double tt, double td,
                          double ad, double ed, int links) {
            this.identifier = id;
            this.originX = ox;
            this.originY = oy;
            this.destinationX = dx;
            this.destinationY = dy;
            this.departureTime = dep;
            this.travelTime = tt;
            this.travelDistance = td;
            this.accessDistance = ad;
            this.egressDistance = ed;
            this.linkCount = links;
        }
    }

    // === CSV TRIP READER ===
    public static List<Trip> readTrips(String tripsPath) throws IOException {
        CsvMapper csvMapper = new CsvMapper();
        File tripsFile = new File(tripsPath);
        if (!tripsFile.exists()) {
            throw new IllegalArgumentException("Trips file not found: " + tripsFile.getAbsolutePath());
        }

        CsvSchema tripsSchema = csvMapper.typedSchemaFor(Trip.class)
                .withHeader()
                .withColumnSeparator(',')
                .withComments()
                .withColumnReordering(true);

        MappingIterator<Trip> tripsIterator = csvMapper.readerWithTypedSchemaFor(Trip.class)
                .with(tripsSchema)
                .readValues(tripsFile);
        List<Trip> tripsList = tripsIterator.readAll();

        if (tripsList.isEmpty()) {
            throw new IllegalArgumentException("Trips file is empty.");
        }
        logger.info("Read trips: " + tripsList.size());
        return tripsList;
    }

    // === CSV OUTPUT WRITING ===
    public static void writeRoutedTrips(String path, List<RoutedTrip> trips) throws IOException {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(RoutedTrip.class).withHeader().withColumnSeparator(',');
        mapper.writer(schema).writeValue(new File(path), trips);
    }


}
