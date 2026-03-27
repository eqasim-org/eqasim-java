package org.eqasim.core.components.network_calibration.freespeed_calibration;

import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.network_calibration.LinkCategorizer;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TripsHandler {
    private static final Logger logger = LogManager.getLogger(TripsHandler.class);

    private final Network network;
    private final LinkCategorizer categorizer;
    private final List<ObservedTripsTravelTimesCsvHandler.ObservedSpeedTrip> observedTrips;
    private final List<Integer> categoriesToCalibrate;
    private final TravelTime carTravelTime;
    private final int threads;
    private final boolean isActivated;
    private final boolean isCalibrating;
    private final Provider<LeastCostPathCalculatorFactory> routerFactoryProvider;

    public TripsHandler(Network network,
                        NetworkCalibrationConfigGroup config,
                        LinkCategorizer categorizer,
                        TravelTime carTravelTime,
                        Provider<LeastCostPathCalculatorFactory> routerFactoryProvider,
                        int threads) {
        this.network = network;
        this.routerFactoryProvider = routerFactoryProvider;
        this.categorizer = categorizer;
        this.categoriesToCalibrate = config.getCategoriesToCalibrationAsList();
        this.carTravelTime = carTravelTime;
        this.threads = threads;
        this.isActivated = config.isOneOfObjectives("freespeed") && config.isActivated();
        this.isCalibrating = this.isActivated && config.isCalibrationEnabled();

        if (isActivated && isCalibrating) {
                if (carTravelTime == null) {
                    throw new IllegalStateException("car TravelTime is required for freespeed calibration and must include simulated conditions.");
                }

                if (carTravelTime instanceof FreeSpeedTravelTime) {
                    throw new IllegalStateException("Freespeed calibration requires congested simulated car TravelTime, but FreeSpeedTravelTime was provided.");
                }

                if (!config.hasObservedSpeedTripsFile()) {
                    throw new IllegalArgumentException("observedSpeedTripsFile must be provided for freespeed calibration objective.");
                }

                this.observedTrips = ObservedTripsTravelTimesCsvHandler.readTrips(config.getObservedSpeedTripsFile());
                logger.info("Freespeed calibration initialized with {} observed trips", observedTrips.size());
        } else {
                this.observedTrips = List.of();
        }

    }

    public Map<LinkGroupKey, FreespeedFactorManager.GroupStats> routeTrips(){
        return routeTripsAndCollectGroupStats();
    }

    private Map<LinkGroupKey, FreespeedFactorManager.GroupStats> routeTripsAndCollectGroupStats() {
        logger.info("Start routing trips and collecting group stats for freespeed calibration");
        logger.info("Using car TravelTime implementation for calibration routing: {}", carTravelTime.getClass().getName());
        int workerCount = Math.max(1, Math.min(threads*2, observedTrips.size()));
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        List<Future<RoutingChunkResult>> futures = new ArrayList<>();
        int chunkSize = Math.max(1, (observedTrips.size() + workerCount - 1) / workerCount);

        for (int start = 0; start < observedTrips.size(); start += chunkSize) {
            int end = Math.min(observedTrips.size(), start + chunkSize);
            final int chunkStart = start;
            final int chunkEnd = end;
            futures.add(executor.submit(() -> routeTripsChunk(chunkStart, chunkEnd)));
        }

        Map<LinkGroupKey, FreespeedFactorManager.GroupStats> groupStats = new HashMap<>();
        int routedTrips = 0;
        int acceptedTrips = 0;

        try {
            for (Future<RoutingChunkResult> future : futures) {
                RoutingChunkResult chunkResult = future.get();
                mergeGroupStats(groupStats, chunkResult.groupStats);
                routedTrips += chunkResult.routedTrips;
                acceptedTrips += chunkResult.acceptedTrips;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while routing observed trips for freespeed calibration", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to route observed trips for freespeed calibration", e);
        } finally {
            executor.shutdown();
        }

        logger.info("\t Routed {} observed trips for freespeed calibration", routedTrips);
        logger.info("\t Accepted {} observed trips for freespeed calibration after applying distance-based filter", acceptedTrips);
        return groupStats;
    }

    private RoutingChunkResult routeTripsChunk(int startIndex, int endIndex) {
        LeastCostPathCalculatorFactory factory = routerFactoryProvider.get();
        LeastCostPathCalculator router = factory.createPathCalculator(network,
                new OnlyTimeDependentTravelDisutility(carTravelTime),
                carTravelTime);


        Map<LinkGroupKey, FreespeedFactorManager.GroupStats> localStats = new HashMap<>();
        int routedTrips = 0;
        int acceptedTrips = 0;

        for (int i = startIndex; i < endIndex; i++) {
            ObservedTripsTravelTimesCsvHandler.ObservedSpeedTrip trip = observedTrips.get(i);
            if (trip.travelTimeSeconds <= 0.0) {
                continue;
            }

            Coord fromCoord = new Coord(trip.departureX, trip.departureY);
            Coord toCoord = new Coord(trip.arrivalX, trip.arrivalY);

            Node fromNode = NetworkUtils.getNearestNode(network, fromCoord);
            Node toNode = NetworkUtils.getNearestNode(network, toCoord);

            if (fromNode == null || toNode == null) {
                continue;
            }

            LeastCostPathCalculator.Path path = router.calcLeastCostPath(fromNode, toNode, trip.departureTime, null, null);
            if (path == null || path.links == null || path.links.isEmpty() || path.travelTime <= 0.0) {
                continue;
            }

            routedTrips++;
            double simulatedDistance = path.links.stream().mapToDouble(Link::getLength).sum();
            double observedDistance = Math.max(0.0, trip.traveledDistanceMeters);
            double simulatedTravelTime = path.travelTime;
            double observedTravelTime = trip.travelTimeSeconds;
            if (!acceptRoutedTrip(observedDistance, observedTravelTime, simulatedDistance, simulatedTravelTime)) {
                continue;
            }

            double now = trip.departureTime;
            for (Link link : path.links) {
                double linkTravelTime = computeLinkTravelTime(link, now);
                now += linkTravelTime;

                int category = categorizer.getBaseCategory(link);
                if (category == LinkCategorizer.UNKNOWN_CATEGORY || !categoriesToCalibrate.contains(category)) {
                    continue;
                }

                double weight = simulatedTravelTime > 0.0 ? linkTravelTime / simulatedTravelTime : 0.0;
                if (!Double.isFinite(weight) || weight <= 0.0) {
                    weight = 1.0 / path.links.size();
                }

                LinkGroupKey key = new LinkGroupKey(category, categorizer.getMunicipalityType(link));
                FreespeedFactorManager.GroupStats stats = localStats.computeIfAbsent(key,
                        ignored -> new FreespeedFactorManager.GroupStats());

                stats.addStat(observedTravelTime, simulatedTravelTime, weight, link.getLength(), link.getFreespeed());
            }

            acceptedTrips++;
        }

        return new RoutingChunkResult(localStats, routedTrips, acceptedTrips);
    }

    private void mergeGroupStats(Map<LinkGroupKey, FreespeedFactorManager.GroupStats> destination,
                                 Map<LinkGroupKey, FreespeedFactorManager.GroupStats> source) {
        for (Map.Entry<LinkGroupKey, FreespeedFactorManager.GroupStats> entry : source.entrySet()) {
            FreespeedFactorManager.GroupStats destinationStats = destination.computeIfAbsent(entry.getKey(), ignored -> new FreespeedFactorManager.GroupStats());
            FreespeedFactorManager.GroupStats sourceStats = entry.getValue();
            destinationStats.merge(sourceStats);
        }
    }


    private boolean acceptRoutedTrip(double observedDistance, double observedTravelTime, double simulatedDistance, double simulatedTravelTime){
        if (observedDistance <= 1000.0 || observedTravelTime <= 180.0 || simulatedDistance <= 1000.0 || simulatedTravelTime <= 180.0) {
            return false;
        }
        if  (Math.abs((simulatedDistance-observedDistance)/observedDistance) > 0.1) {
            return false;
        }
        double diffTravelTime_pct = (simulatedTravelTime-observedTravelTime)/observedTravelTime;
        return !(diffTravelTime_pct > 2) && !(diffTravelTime_pct < -0.3);
    }

    private double computeLinkTravelTime(Link link, double time) {
        return carTravelTime.getLinkTravelTime(link, time, null, null);
    }

    private record RoutingChunkResult(Map<LinkGroupKey, FreespeedFactorManager.GroupStats> groupStats, int routedTrips, int acceptedTrips) {}
}
