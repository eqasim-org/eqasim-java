package org.eqasim.switzerland.ch_cmdp.utils.routing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.travel_disutility.EqasimTravelDisutilityFactory;
import org.eqasim.core.components.travel_time.RecordedTravelTime;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.eqasim.core.simulation.policies.routing.RoutingPenalty;
import org.eqasim.core.simulation.policies.routing.ZeroRoutingPenalty;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.speedy.SpeedyALTFactory;
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
import java.util.concurrent.atomic.AtomicReference;

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

    // === ROUTING TRIPS IN PARALLEL ===
        public static List<RoutedTrip> routeTrips(List<Trip> trips, Network network, TravelTime travelTime, int threads,
            int batchSize, boolean returnLinks, double routingDistanceUtility) throws InterruptedException {
        logger.info("Routing trips ...");
        logger.info("\t Batch size: {}", batchSize);
        logger.info("\t Threads: {}", threads);

        // configure the disutility used during the routing (since we get travel times from events, we use zero penalty)
        RoutingPenalty zeroPenalty = new ZeroRoutingPenalty();
        EqasimTravelDisutilityFactory disutilityFactory = new EqasimTravelDisutilityFactory(zeroPenalty, routingDistanceUtility);
        // router factory
        SpeedyALTFactory routerFactory = new SpeedyALTFactory();
        // progression trackers
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger nextTripIndex = new AtomicInteger(0);
        AtomicReference<Throwable> failure = new AtomicReference<>(null);

        int total = trips.size();
        int progressInterval = batchSize *  threads;

        RoutedTrip[] results = new RoutedTrip[total];
        List<Thread> workers = new ArrayList<>(threads);

        for (int workerIndex = 0; workerIndex < threads; workerIndex++) {
            Thread worker = new Thread(() -> {
                try {
                    TravelDisutility disutility = disutilityFactory.createTravelDisutility(travelTime);
                    LeastCostPathCalculator router = routerFactory.createPathCalculator(network, disutility, travelTime);

                    while (failure.get() == null) {
                        int start = nextTripIndex.getAndAdd(batchSize);
                        if (start >= total) {
                            return;
                        }

                        int end = Math.min(total, start + batchSize);
                        for (int index = start; index < end; index++) {
                            Trip trip = trips.get(index);
                            results[index] = routeTrip(trip, network, router, returnLinks);
                        }

                        int progress = completed.addAndGet(end - start);
                        if (progress % progressInterval == 0 || progress == total) {
                            logger.info("\t Progress ({}%): {}/{} trips routed.", (progress * 100 / total), progress, total);
                        }
                    }
                } catch (Throwable e) {
                    failure.compareAndSet(null, e);
                }
            }, "trips-router-" + workerIndex);

            workers.add(worker);
            worker.start();
        }

        for (Thread worker : workers) {
            worker.join();
        }

        if (failure.get() != null) {
            throw new RuntimeException("Routing failed in worker thread", failure.get());
        }

        return Arrays.asList(results);
    }

        public static RoutedTrip routeTrip(Trip trip, Network network, LeastCostPathCalculator router,
            boolean returnLinks) {
        Coord fromCoord = new Coord(trip.originX, trip.originY);
        Coord toCoord = new Coord(trip.destinationX, trip.destinationY);

        Node fromNode = NetworkUtils.getNearestNode(network, fromCoord);
        Node toNode = NetworkUtils.getNearestNode(network, toCoord);

        LeastCostPathCalculator.Path path = router.calcLeastCostPath(fromNode, toNode, trip.departureTime, null, null);

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
