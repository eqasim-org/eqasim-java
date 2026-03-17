package org.eqasim.core.components.network_calibration.freespeed_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.network_calibration.LinkCategorizer;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
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

public class FreespeedAdapter implements IterationEndsListener, IterationStartsListener {
    private static final Logger logger = LogManager.getLogger(FreespeedAdapter.class);

    private final Network network;
    private final OutputDirectoryHierarchy outputHierarchy;
    private final LinkCategorizer categorizer;
    private final FreespeedFactorManager factorManager;
    private final List<ObservedTripsTravelTimesCsvHandler.ObservedSpeedTrip> observedTrips;
    private final int updateInterval;
    private final int saveNetworkInterval;
    private final List<Integer> categoriesToCalibrate;
    private final IdMap<Link, Double> baseFreespeeds = new IdMap<>(Link.class);
    private final TravelTime carTravelTime;
    private final int threads;
    private final boolean isActivated;

    public FreespeedAdapter(Network network,
                            NetworkCalibrationConfigGroup config,
                            OutputDirectoryHierarchy outputHierarchy,
                            LinkCategorizer categorizer,
                            FreespeedFactorManager factorManager,
                            TravelTime carTravelTime,
                            int threads) {
        this.network = network;
        this.outputHierarchy = outputHierarchy;
        this.categorizer = categorizer;
        this.factorManager = factorManager;
        this.updateInterval = config.getUpdateInterval();
        this.saveNetworkInterval = config.getSaveNetworkInterval();
        this.categoriesToCalibrate = config.getCategoriesToCalibrationAsList();
        this.carTravelTime = carTravelTime;
        this.threads = threads;
        this.isActivated = config.isOneOfObjectives("freespeed") & config.isActivated();

        if (isActivated) {
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

            for (Link link : network.getLinks().values()) {
                if (categorizer.getCategory(link) != LinkCategorizer.UNKNOWN_CATEGORY) {
                    baseFreespeeds.put(link.getId(), link.getFreespeed());
                }
            }

            logger.info("Freespeed calibration initialized with {} observed trips", observedTrips.size());
        } else {
            this.observedTrips = null;
        }
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (isActivated) {
            int iteration = event.getIteration();

            if (updateInterval > 0 && iteration > 0 && iteration % updateInterval == 0) {
                Map<LinkGroupKey, FreespeedFactorManager.GroupStats> groupStats = routeTripsAndCollectGroupStats();
                factorManager.updateFactors(groupStats);
                applyFactors();
                saveOutputs(iteration, groupStats);
            }

            if (saveNetworkInterval > 0 && iteration > 0 && iteration % saveNetworkInterval == 0) {
                saveNetwork(iteration);
            }
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        // No action needed at iteration end for this class
    }

    private Map<LinkGroupKey, FreespeedFactorManager.GroupStats> routeTripsAndCollectGroupStats() {
        logger.info("Start routing trips and collecting group stats for freespeed calibration");
        logger.info("Using car TravelTime implementation for calibration routing: {}", carTravelTime.getClass().getName());
        int workerCount = Math.max(1, Math.min(threads, observedTrips.size()));
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
        LeastCostPathCalculator router = new DijkstraFactory().createPathCalculator(
                network,
                new OnlyTimeDependentTravelDisutility(carTravelTime),
                carTravelTime
        );

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

            if (!acceptRoutedTrip(observedDistance, simulatedDistance)) {
                continue;
            }

            double simulatedTravelTime = path.travelTime;
            double observedTravelTime = trip.travelTimeSeconds;
            double now = trip.departureTime;

            for (Link link : path.links) {
                double linkTravelTime = computeLinkTravelTime(link, now);
                now += linkTravelTime;

                int category = categorizer.getCategory(link);
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

                stats.simulatedTime += simulatedTravelTime * weight;
                stats.observedTime += observedTravelTime * weight;
                stats.simulatedDistance += simulatedDistance * weight;
                stats.observedDistance += observedDistance * weight;
                stats.tripCount += 1;
            }

            acceptedTrips++;
        }

        return new RoutingChunkResult(localStats, routedTrips, acceptedTrips);
    }

    private void mergeGroupStats(Map<LinkGroupKey, FreespeedFactorManager.GroupStats> destination,
                                 Map<LinkGroupKey, FreespeedFactorManager.GroupStats> source) {
        for (Map.Entry<LinkGroupKey, FreespeedFactorManager.GroupStats> entry : source.entrySet()) {
            FreespeedFactorManager.GroupStats destinationStats = destination.computeIfAbsent(entry.getKey(),
                    ignored -> new FreespeedFactorManager.GroupStats());
            FreespeedFactorManager.GroupStats sourceStats = entry.getValue();
            destinationStats.simulatedTime += sourceStats.simulatedTime;
            destinationStats.observedTime += sourceStats.observedTime;
            destinationStats.simulatedDistance += sourceStats.simulatedDistance;
            destinationStats.observedDistance += sourceStats.observedDistance;
            destinationStats.tripCount += sourceStats.tripCount;
        }
    }

    private static class RoutingChunkResult {
        final Map<LinkGroupKey, FreespeedFactorManager.GroupStats> groupStats;
        final int routedTrips;
        final int acceptedTrips;

        RoutingChunkResult(Map<LinkGroupKey, FreespeedFactorManager.GroupStats> groupStats, int routedTrips, int acceptedTrips) {
            this.groupStats = groupStats;
            this.routedTrips = routedTrips;
            this.acceptedTrips = acceptedTrips;
        }
    }

    private boolean acceptRoutedTrip(double observedDistance, double simulatedDistance){
        return Math.abs((observedDistance-simulatedDistance)/observedDistance) < 0.1; // only accept less than 10% difference
    }

    private double computeLinkTravelTime(Link link, double time) {
        return carTravelTime.getLinkTravelTime(link, time, null, null);
    }

    private void applyFactors() {
        for (Link link : network.getLinks().values()) {
            int category = categorizer.getCategory(link);
            if (category == LinkCategorizer.UNKNOWN_CATEGORY || !categoriesToCalibrate.contains(category)) {
                continue;
            }

            LinkGroupKey key = new LinkGroupKey(category, categorizer.getMunicipalityType(link));
            double baseFreespeed = baseFreespeeds.get(link.getId());
            double factor = factorManager.getFactor(key);
            link.setFreespeed(Math.max(0.1, baseFreespeed * factor));
        }
    }

    private void saveOutputs(int iteration, Map<LinkGroupKey, FreespeedFactorManager.GroupStats> groupStats) {
        String factorsFile = outputHierarchy.getIterationFilename(iteration, "freespeed_factors_by_group.csv");
        String groupsFile = outputHierarchy.getIterationFilename(iteration, "freespeed_group_stats.csv");

        FreespeedCsvHandler.writeFactors(factorsFile, factorManager.getFactorsSnapshot());
        FreespeedCsvHandler.writeGroupStats(groupsFile, groupStats);
        FreespeedCsvHandler.logFactorsByMunicipalityType(logger, factorManager.getFactorsSnapshot());;
    }

    private void saveNetwork(int iteration) {
        String filename = outputHierarchy.getIterationFilename(iteration, "network_calibrated_freespeed.xml.gz");
        new NetworkWriter(network).write(filename);
    }
}

