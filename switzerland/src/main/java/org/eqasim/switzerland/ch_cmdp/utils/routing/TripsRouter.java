package org.eqasim.switzerland.ch_cmdp.utils.routing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.travel_disutility.EqasimTravelDisutilityFactory;
import org.eqasim.core.components.travel_time.RecordedTravelTime;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.eqasim.core.simulation.policies.routing.ZeroRoutingPenalty;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.eqasim.core.components.routing.speedy.MemoryEfficientSpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TripsRouter {
    public static final Logger logger = LogManager.getLogger(TripsRouter.class);

    public static Network loadNetwork(String networkPath) {
        Network network = NetworkUtils.readNetwork(networkPath);
        logger.info("Loaded network: {} links, {} nodes.", network.getLinks().size(), network.getNodes().size());
        return network;
    }

    public static RecordedTravelTime loadTravelTime(String eventsPath, RoadNetwork roadNetwork, double startTime,
            double endTime, double interval) {
        File eventsFile = new File(eventsPath);
        return RecordedTravelTime.readFromEvents(eventsFile, roadNetwork, startTime, endTime, interval);
    }

    public static void applyDefaultDepartureTime(List<Trip> trips, double defaultDepartureTime) {
        if (defaultDepartureTime < 0) {
            return;
        }
        for (Trip trip : trips) {
            trip.departureTime = defaultDepartureTime;
        }
    }

    public static List<RoutedTrip> routeTripsFromJsonRequest(ObjectMapper mapper, InputStream body,
            Network network, TravelTime travelTime, int threads, int batchSize, double defaultDepartureTime,
            boolean returnLinks, double routingDistanceUtility)
            throws IOException, InterruptedException {
        List<Trip> trips = readTripsFromJsonBody(mapper, body);
        applyDefaultDepartureTime(trips, defaultDepartureTime);
        return routeTrips(trips, network, travelTime, threads, batchSize, returnLinks, routingDistanceUtility);
    }

    public static List<Trip> readTripsFromJsonBody(ObjectMapper mapper, InputStream body) throws IOException {
        JsonNode json = mapper.readTree(body);
        List<Trip> trips;
        if (json.isArray()) {
            trips = mapper.convertValue(json,
                    mapper.getTypeFactory().constructCollectionType(List.class, Trip.class));
        } else if (json.has("trips")) {
            trips = mapper.convertValue(json.get("trips"),
                    mapper.getTypeFactory().constructCollectionType(List.class, Trip.class));
        } else {
            throw new IllegalArgumentException("Body must be an array of trips or {\"trips\": [...]}.");
        }

        if (trips == null || trips.isEmpty()) {
            throw new IllegalArgumentException("No trips provided.");
        }

        return trips;
    }

    public static List<RoutedTrip> routeTrips(List<Trip> trips, Network network, TravelTime travelTime, int threads,
            int batchSize, boolean returnLinks, double routingDistanceUtility) throws InterruptedException {
        try (var session = new RoutingSession(network, travelTime, threads, batchSize, routingDistanceUtility)) {
            return session.route(trips, returnLinks);
        }
    }

    /** Treat the configured batch as a maximum; keep short/uneven requests parallel too. */
    static int routingBatchSize(int trips, int workers, int maximum) {
        long targetBatches = 8L * workers;
        return (int) Math.max(1, Math.min(maximum, (trips + targetBatches - 1) / targetBatches));
    }

    /** One persistent bounded pool, shared graph/landmarks/times, one private router per job. */
    public static final class RoutingSession implements AutoCloseable {
        private final Network network;
        private final int batchSize;
        private final List<LeastCostPathCalculator> routers = new ArrayList<>();
        private final java.util.concurrent.ExecutorService executor;
        private final java.util.concurrent.locks.ReentrantLock lock = new java.util.concurrent.locks.ReentrantLock(true);
        private boolean closed;

        public RoutingSession(Network network, TravelTime travelTime, int threads, int batchSize, double distanceUtility) {
            if (threads < 1 || batchSize < 1 || !Double.isFinite(distanceUtility) || distanceUtility < 0)
                throw new IllegalArgumentException("threads/batchSize must be positive and routingDistanceUtility finite/nonnegative");
            if (network.getNodes().isEmpty()) throw new IllegalArgumentException("Network has no nodes");
            this.network = network;
            this.batchSize = batchSize;
            NetworkUtils.getNearestNode(network, network.getNodes().values().iterator().next().getCoord());
            var factory = new MemoryEfficientSpeedyALTFactory();
            TravelDisutility costs = new EqasimTravelDisutilityFactory(new ZeroRoutingPenalty(), distanceUtility)
                    .createTravelDisutility(travelTime);
            if (travelTime instanceof RecordedTravelTime recorded) {
                final TravelDisutility delegate = costs;
                int size = network.getLinks().keySet().stream().mapToInt(id -> id.index()).max().orElse(-1) + 1;
                double[] minimums = new double[size];
                for (Link link : network.getLinks().values()) minimums[link.getId().index()] =
                        recorded.getMinimumLinkTravelTime(link) + distanceUtility * link.getLength() / 10.0;
                costs = new TravelDisutility() {
                    public double getLinkTravelDisutility(Link link, double time,
                            org.matsim.api.core.v01.population.Person person, org.matsim.vehicles.Vehicle vehicle) {
                        return delegate.getLinkTravelDisutility(link, time, person, vehicle);
                    }
                    public double getLinkMinimumTravelDisutility(Link link) { return minimums[link.getId().index()]; }
                };
            }
            for (int i = 0; i < threads; i++) routers.add(factory.createPathCalculator(network, costs, travelTime));
            executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
            logger.info("Optimized SpeedyALT prepared: {} workers; shared {} travel times", threads,
                    travelTime.getClass().getSimpleName());
        }

        public List<RoutedTrip> route(List<Trip> trips, boolean returnLinks) throws InterruptedException {
            lock.lockInterruptibly();
            try {
                if (closed) throw new IllegalStateException("Routing session is closed");
                if (trips.isEmpty()) return List.of();
                var next = new AtomicInteger();
                var stop = new java.util.concurrent.atomic.AtomicBoolean();
                int batch = routingBatchSize(trips.size(), routers.size(), batchSize);
                RoutedTrip[] results = new RoutedTrip[trips.size()];
                var tasks = new ArrayList<java.util.concurrent.Future<?>>();
                int count = Math.min(routers.size(), 1 + (trips.size() - 1) / batch);
                for (int worker = 0; worker < count; worker++) {
                    var router = routers.get(worker);
                    tasks.add(executor.submit(() -> {
                        try {
                            for (int start; !stop.get() && (start = next.getAndAdd(batch)) < trips.size();) {
                                int end = (int) Math.min(trips.size(), (long) start + batch);
                                for (int i = start; i < end && !stop.get(); i++)
                                    results[i] = routeTrip(trips.get(i), network, router, returnLinks);
                            }
                        } catch (RuntimeException | Error e) { stop.set(true); throw e; }
                    }));
                }
                Throwable failure = null;
                boolean interrupted = false;
                // Drain every job before releasing its routers to the next request.
                for (var task : tasks) {
                    for (boolean done = false; !done;) {
                        try { task.get(); done = true; }
                        catch (InterruptedException e) { interrupted = true; stop.set(true); }
                        catch (java.util.concurrent.ExecutionException e) { failure = e.getCause(); done = true; }
                    }
                }
                if (interrupted) { Thread.currentThread().interrupt(); throw new InterruptedException("Routing interrupted"); }
                if (failure != null) throw new IllegalStateException("Routing worker failed", failure);
                return Arrays.asList(results);
            } finally { lock.unlock(); }
        }

        @Override public void close() {
            lock.lock();
            try { closed = true; executor.shutdown(); }
            finally { lock.unlock(); }
        }
    }

    public static RoutedTrip routeTrip(Trip trip, Network network, LeastCostPathCalculator router,
            boolean returnLinks) {
        Coord fromCoord = new Coord(trip.originX, trip.originY);
        Coord toCoord = new Coord(trip.destinationX, trip.destinationY);

        Node fromNode = NetworkUtils.getNearestNode(network, fromCoord);
        Node toNode = NetworkUtils.getNearestNode(network, toCoord);

        LeastCostPathCalculator.Path path = router.calcLeastCostPath(fromNode, toNode, trip.departureTime, null, null);

        if (path == null) return new RoutedTrip(trip.identifier, trip.originX, trip.originY,
                trip.destinationX, trip.destinationY, trip.departureTime,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN, -1, "");
        double travelTimeSeconds = path.travelTime;
        double travelDistanceMeters = path.links.stream().mapToDouble(Link::getLength).sum();
        double accessDistance = CoordUtils.calcEuclideanDistance(fromCoord, fromNode.getCoord());
        double egressDistance = CoordUtils.calcEuclideanDistance(toCoord, toNode.getCoord());
        String links = returnLinks
            ? String.join("-", path.links.stream().map(Link::getId).map(Object::toString).toArray(String[]::new))
            : "";

        return new RoutedTrip(trip.identifier, trip.originX, trip.originY,
                              trip.destinationX, trip.destinationY, trip.departureTime,
                      travelTimeSeconds, travelDistanceMeters, accessDistance,
                      egressDistance, path.links.size(), links);
    }

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
        @JsonProperty("links") public String links;

        public RoutedTrip() {}

        public RoutedTrip(String id, double ox, double oy, double dx, double dy, double dep, double tt, double td,
                          double ad, double ed, int numLinks, String links) {
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
            this.linkCount = numLinks;
            this.links = links;
        }
    }
}
