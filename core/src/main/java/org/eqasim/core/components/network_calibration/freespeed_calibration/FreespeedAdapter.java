package org.eqasim.core.components.network_calibration.freespeed_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.network_calibration.LinkCategorizer;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.NetworkCalibrationUtils;
import org.eqasim.core.components.network_calibration.cost_calibration.PenaltiesAdapter;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

public class FreespeedAdapter implements IterationEndsListener, IterationStartsListener, ShutdownListener {
    private static final Logger logger = LogManager.getLogger(FreespeedAdapter.class);

    private final Network network;
    private final OutputDirectoryHierarchy outputHierarchy;
    private final LinkCategorizer categorizer;
    private final FreespeedFactorManager factorManager;
    private final int updateInterval;
    private final int updateStartIteration;
    private final IdMap<Link, Double> baseFreespeeds = new IdMap<>(Link.class);
    private final boolean isActivated;
    private final boolean isCalibrating;
    private final boolean hasFactorsFile;
    private final boolean useLinkAttributes;
    private final PenaltiesAdapter penaltiesAdapter;
    private final TripsHandler tripsHandler;

    public FreespeedAdapter(Network network,
                            NetworkCalibrationConfigGroup config,
                            FreeSpeedCalibrationConfigGroup freespeedConfig,
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
        this.updateInterval = freespeedConfig.getUpdateInterval();
        this.updateStartIteration = freespeedConfig.getWarmupIterations();
        this.isActivated = config.isFreeSpeedFactorActivated() && config.isActivated();
        this.isCalibrating = this.isActivated && config.isFreeSpeedFactorCalibrationActivated();
        this.hasFactorsFile = freespeedConfig.hasFactorsFile();
        this.useLinkAttributes = !isCalibrating && !hasFactorsFile;

        if (isActivated) {
            for (Link link : network.getLinks().values()) {
                boolean eligible;
                if (useLinkAttributes) {
                    OptionalDouble factor = NetworkCalibrationUtils.readDoubleAttribute(link, NetworkCalibrationUtils.SPEED_FACTOR_ATTRIBUTE);
                    eligible = categorizer.isCarLink(link) && factor.isPresent() && factor.getAsDouble() > 0;
                } else {
                    eligible = categorizer.getFreespeedCalibrationKey(link) != null;
                }
                if (eligible) {
                    baseFreespeeds.put(link.getId(), link.getFreespeed());
                }
            }

            if (!useLinkAttributes) {
                Map<FreespeedCalibrationKey, Double> initialFactors = loadFactorsFromNetworkAttributes();
                if (hasFactorsFile) {
                    initialFactors.putAll(FreespeedCsvHandler.readFactors(freespeedConfig.getFactorsFile()));
                }
                factorManager.loadFactors(initialFactors);
            }
            applyFactors();

            if (isCalibrating) {
                if (!freespeedConfig.hasObservedTripsFile()) {
                    throw new IllegalArgumentException("observedTripsFile must be provided for freespeed calibration.");
                }

                logger.info("Freespeed updates will start at iteration {} and then repeat every {} iterations",
                        updateStartIteration, updateInterval);
            } else {
                logger.info("Freespeed objective is active in fixed mode. {}",
                        useLinkAttributes ? "Using each link's speedFactor directly, without regional grouping."
                                : "Using grouped factors with CSV overrides.");
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
                penaltiesAdapter.disable(); // do not use penalties during this routing (freespeedDisutility is used, but keep this here in case we change it at some point, this is safer)
                Map<FreespeedCalibrationKey, FreespeedFactorManager.GroupStats> groupStats;
                try {
                    groupStats = tripsHandler.routeTrips();
                } finally {
                    penaltiesAdapter.enable(); // also restore penalties if routing fails
                }

                if (groupStats.isEmpty()) {
                    logger.warn("No valid group stats collected at iteration {}. Factors will remain unchanged. " +
                                    "Check observed trips coverage, acceptance filters, and minTripsPerGroup.",
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
        if (isActivated && event.isLastIteration()) {
            // MATSim's core shutdown listeners write output_network before user
            // shutdown listeners run. Prepare the network while still in the
            // final iteration, also when applying fixed factors without calibration.
            restoreBaseFreespeedsAndStoreFactors();
        }
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        if (!isActivated) {
            return;
        }

        // Fallback for interrupted runs. On normal completion the network was
        // already restored before MATSim's output writer ran.
        restoreBaseFreespeedsAndStoreFactors();

        if (isCalibrating) {
            String filename = outputHierarchy.getOutputFilename("final_freespeed_factors_by_group.csv");
            FreespeedCsvHandler.writeFactors(filename, factorManager.getFactorsSnapshot());
        }
    }

    private void restoreBaseFreespeedsAndStoreFactors() {
        int restored = 0;
        int persisted = 0;
        for (Link link : network.getLinks().values()) {
            if (!baseFreespeeds.containsKey(link.getId())) {
                continue;
            }

            link.setFreespeed(baseFreespeeds.get(link.getId()));
            restored++;

            if (useLinkAttributes) {
                continue; // Preserve each stored factor exactly, including regional differences.
            }

            FreespeedCalibrationKey key = categorizer.getFreespeedCalibrationKey(link);
            if (key == null) {
                continue;
            }
            double factor = factorManager.getFactor(key);
            NetworkCalibrationUtils.writeDoubleAttribute(link, NetworkCalibrationUtils.SPEED_FACTOR_ATTRIBUTE, factor);
            persisted++;
        }

        logger.info("Restored base freespeeds for {} links and persisted '{}' for {} links.",
                restored, NetworkCalibrationUtils.SPEED_FACTOR_ATTRIBUTE, persisted);

    }

    public boolean shouldUpdateAtIteration(int iteration) {
        if (updateInterval <= 0 || iteration < updateStartIteration) {
            return false;
        }
        return iteration == updateStartIteration || (iteration - updateStartIteration) % updateInterval == 0;
    }

    private void applyFactors() {
        if (useLinkAttributes) {
            baseFreespeeds.forEach((id, baseSpeed) -> {
                Link link = network.getLinks().get(id);
                double factor = NetworkCalibrationUtils.readDoubleAttribute(link,
                        NetworkCalibrationUtils.SPEED_FACTOR_ATTRIBUTE).orElse(1.0);
                link.setFreespeed(Math.max(0.1, baseSpeed * factor));
            });
            return;
        }
        for (Link link : network.getLinks().values()) {
            FreespeedCalibrationKey key = categorizer.getFreespeedCalibrationKey(link);
            if (key == null) {
                continue;
            }
            double baseFreespeed = baseFreespeeds.get(link.getId());
            double factor = factorManager.getFactor(key);
            link.setFreespeed(Math.max(0.1, baseFreespeed * factor));
        }
    }

    private Map<FreespeedCalibrationKey, Double> loadFactorsFromNetworkAttributes() {
        Map<FreespeedCalibrationKey, Double> sums = new HashMap<>();
        Map<FreespeedCalibrationKey, Integer> counts = new HashMap<>();

        for (Link link : network.getLinks().values()) {
            FreespeedCalibrationKey key = categorizer.getFreespeedCalibrationKey(link);
            if (key == null) {
                continue;
            }

            OptionalDouble factor = NetworkCalibrationUtils.readDoubleAttribute(link, NetworkCalibrationUtils.SPEED_FACTOR_ATTRIBUTE);
            if (factor.isEmpty() || factor.getAsDouble() <= 0.0) {
                continue;
            }
            sums.merge(key, factor.getAsDouble(), Double::sum);
            counts.merge(key, 1, Integer::sum);
        }

        Map<FreespeedCalibrationKey, Double> initialFactors = new HashMap<>();
        for (Map.Entry<FreespeedCalibrationKey, Double> entry : sums.entrySet()) {
            int count = counts.getOrDefault(entry.getKey(), 0);
            if (count > 0) {
                initialFactors.put(entry.getKey(), entry.getValue() / count);
            }
        }

        logger.info("Loaded initial freespeed factors from network attributes for {} groups.", initialFactors.size());
        return initialFactors;
    }

    private void saveOutputs(int iteration, Map<FreespeedCalibrationKey, FreespeedFactorManager.GroupStats> groupStats) {
        String factorsFile = outputHierarchy.getIterationFilename(iteration, "freespeed_factors_by_group.csv");
        String groupsFile = outputHierarchy.getIterationFilename(iteration, "freespeed_group_stats.csv");
        String diagnosticsFile = outputHierarchy.getIterationFilename(iteration, "freespeed_group_diagnostics.csv");

        FreespeedCsvHandler.writeFactors(factorsFile, factorManager.getFactorsSnapshot());
        FreespeedCsvHandler.writeGroupStats(groupsFile, groupStats);
        FreespeedCsvHandler.writeDiagnostics(diagnosticsFile, factorManager.getDiagnosticsSnapshot());
        FreespeedCsvHandler.logFactorsByMunicipalityType(logger, factorManager.getFactorsSnapshot());
    }

}

