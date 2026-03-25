package org.eqasim.core.components.network_calibration.freespeed_calibration;

import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.network_calibration.LinkCategorizer;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.cost_calibration.PenaltiesAdapter;
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

public class FreespeedAdapter implements IterationEndsListener, IterationStartsListener {
    private static final Logger logger = LogManager.getLogger(FreespeedAdapter.class);

    private final Network network;
    private final OutputDirectoryHierarchy outputHierarchy;
    private final LinkCategorizer categorizer;
    private final FreespeedFactorManager factorManager;
    private final int updateInterval;
    private final int updateStartIteration;
    private final List<Integer> categoriesToCalibrate;
    private final IdMap<Link, Double> baseFreespeeds = new IdMap<>(Link.class);
    private final boolean isActivated;
    private final boolean isCalibrating;
    private final PenaltiesAdapter penaltiesAdapter;
    private final TripsHandler tripsHandler;

    public FreespeedAdapter(Network network,
                            NetworkCalibrationConfigGroup config,
                            OutputDirectoryHierarchy outputHierarchy,
                            LinkCategorizer categorizer,
                            FreespeedFactorManager factorManager,
                            PenaltiesAdapter penaltiesAdapter,
                            TripsHandler tripsHandler
                            ) {
        this.network = network;
        this.penaltiesAdapter = penaltiesAdapter;
        this.outputHierarchy = outputHierarchy;
        this.categorizer = categorizer;
        this.tripsHandler = tripsHandler;
        this.factorManager = factorManager;
        this.updateInterval = Math.max(5, config.getUpdateInterval());
        this.updateStartIteration = Math.max(10, config.getFreespeedWarmupIterations());
        this.categoriesToCalibrate = config.getCategoriesToCalibrationAsList();
        this.isActivated = config.isOneOfObjectives("freespeed") && config.isActivated();
        this.isCalibrating = this.isActivated && config.isCalibrationEnabled();

        if (isActivated) {
            for (Link link : network.getLinks().values()) {
                if (categorizer.getBaseCategory(link) != LinkCategorizer.UNKNOWN_CATEGORY) {
                    baseFreespeeds.put(link.getId(), link.getFreespeed());
                }
            }

            if (isCalibrating) {
                if (!config.hasObservedSpeedTripsFile()) {
                    throw new IllegalArgumentException("observedSpeedTripsFile must be provided for freespeed calibration objective.");
                }

                logger.info("Freespeed updates will start at iteration {} and then repeat every {} iterations",
                        updateStartIteration, updateInterval);
            } else {
                factorManager.loadFactors(FreespeedCsvHandler.readFactors(config.getFreespeedFactorsFile()));
                applyFactors();
                logger.info("Freespeed objective is active in fixed mode. Loaded factors are kept constant during simulation.");
            }
        }
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (isActivated && isCalibrating) {
            int iteration = event.getIteration();
            if (shouldUpdateAtIteration(iteration)) {
                logger.info("Triggering freespeed calibration update at iteration {} (start={}, interval={})",
                        iteration, updateStartIteration, updateInterval);
                penaltiesAdapter.diable(); // do not use penalties during this routing (freespeedDisutility is used, but keep this here in case we change it at some point, this is safer)
                Map<LinkGroupKey, FreespeedFactorManager.GroupStats> groupStats = tripsHandler.routeTrips();
                penaltiesAdapter.enable(); // reset it

                if (groupStats.isEmpty()) {
                    logger.warn("No valid group stats collected at iteration {}. Factors will remain unchanged. " +
                                    "Check observed trips coverage, acceptance filters, categoriesToCalibrate, and minTripsPerGroup.",
                            iteration);
                }

                factorManager.updateFactors(groupStats, iteration);
                applyFactors();
                saveOutputs(iteration, groupStats);
            }

        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        // No action needed at iteration end for this class
    }

    public boolean shouldUpdateAtIteration(int iteration) {
        if (updateInterval <= 0 || iteration < updateStartIteration) {
            return false;
        }
        return iteration == updateStartIteration || (iteration - updateStartIteration) % updateInterval == 0;
    }

    private void applyFactors() {
        for (Link link : network.getLinks().values()) {
            int category = categorizer.getBaseCategory(link);
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
        String diagnosticsFile = outputHierarchy.getIterationFilename(iteration, "freespeed_group_diagnostics.csv");

        FreespeedCsvHandler.writeFactors(factorsFile, factorManager.getFactorsSnapshot());
        FreespeedCsvHandler.writeGroupStats(groupsFile, groupStats);
        FreespeedCsvHandler.writeDiagnostics(diagnosticsFile, factorManager.getDiagnosticsSnapshot());
        FreespeedCsvHandler.logFactorsByMunicipalityType(logger, factorManager.getFactorsSnapshot());
    }

}

