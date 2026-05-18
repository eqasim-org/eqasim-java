package org.eqasim.core.components.network_calibration.cost_calibration;

import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.network_calibration.LinkCategorizer;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.NetworkCalibrationUtils;
import org.eqasim.core.components.network_calibration.Processors.CountsProcessor;
import org.eqasim.core.components.network_calibration.Processors.FlowProcessor;
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

/**
 * Adapts penalties for link categories based on flow and count data.
 * Penalties are used to adjust routing costs for calibration purposes.
 * Supports advanced features like convergence detection and adaptive learning rates.
 */
public class PenaltiesAdapter implements IterationStartsListener, IterationEndsListener, ShutdownListener {
    private static final Logger logger = LogManager.getLogger(PenaltiesAdapter.class);

    private final CountsProcessor countsProcessor;
    private final FlowProcessor flowProcessor;
    private final Network network;
    private final double sampleSize;
    private final PenaltyManager penaltyManager;
    private final double beta;
    private final int updateInterval;
    private final OutputDirectoryHierarchy outputHierarchy;
    private final double rampFactor;
    private final double trunkFactor;
    private final LinkCategorizer categorizer;
    private final PenaltyKeyManager penaltyKeyManager;
    private final boolean isActivated;
    private final boolean isCalibrating;
    private final int warmupIterations;
    private final boolean hasPenaltiesFile;
    private boolean disable = false;
    /**
     * Constructs a penalties adapter and initializes penalties from network attributes and/or CSV.
     */
    public PenaltiesAdapter(Network network,
                            Provider<CountsProcessor> countsProcessorProvider, Provider<FlowProcessor> flowProcessorProvider,
                            NetworkCalibrationConfigGroup config, OutputDirectoryHierarchy outputHierarchy,
                            EqasimConfigGroup eqasimConfig, LinkCategorizer categorizer,
                            PenaltyKeyManager penaltyKeyManager, PenaltyManager penaltyManager) {
        this.network = network;
        this.sampleSize = eqasimConfig.getSampleSize();
        this.updateInterval = config.getUpdateInterval();
        this.beta = config.getBeta();
        this.outputHierarchy = outputHierarchy;
        this.rampFactor = config.getRampFactor();
        this.trunkFactor = config.getTrunkFactor();
        this.categorizer = categorizer;
        this.penaltyKeyManager = penaltyKeyManager;
        this.penaltyManager = penaltyManager;
        this.isActivated = config.isOneOfObjectives("penalty") && config.isActivated();
        this.isCalibrating = this.isActivated && config.isCalibrationEnabled();
        this.warmupIterations = config.getPenaltiesWarmupIterations();
        this.hasPenaltiesFile = config.hasPenaltiesFile();

        this.countsProcessor = isCalibrating ? countsProcessorProvider.get() : null;
        this.flowProcessor = isCalibrating ? flowProcessorProvider.get() : null;

        if (isActivated) {
            penaltyManager.loadInitialPenalties(loadInitialPenaltiesFromNetwork());

            if (hasPenaltiesFile) {
                penaltyManager.loadFromCsv(config.getPenaltiesFile());
            }

            if (!isCalibrating) {
                logger.info("Penalty objective is active in fixed mode. Penalties are loaded from CSV when provided, otherwise from link attributes.");
            }
        }
    }

    /**
     * Computes the penalty for a given link based on its category.
     * Applies factors for ramps and trunks.
     */
    public double computePenalty(Link link) {
        if (disable) {
            return 0.0;
        }

        PenaltyGroupKey calibrationKey = penaltyKeyManager.toCalibrationKey(link);
        if (calibrationKey == null) {
            return 0.0;
        }

        double categoryPenalty = getGroupPenaltyForLink(link, calibrationKey);
        return computeRoutingPenalty(link, categoryPenalty);
    }

    private double computeRoutingPenalty(Link link, double categoryPenalty) {
        double travelTime = link.getLength() / link.getFreespeed();
        return travelTime * categoryPenalty;
    }

    private double getGroupPenaltyForLink(Link link, PenaltyGroupKey key) {
        double penalty = penaltyManager.getPenalty(key);
        if (NetworkCalibrationUtils.isRamp(link)) {
            penalty *= rampFactor;
        }
        if (NetworkCalibrationUtils.isTrunk(link)) {
            penalty *= trunkFactor;
        }
        return penalty;
    }

    /**
     * Loads initial penalties from network attributes by averaging values for each category.
     * We could, in this case, get the penalty from the link directly and use it, but this way would make it
     * compatible with the calibration, as we are not calibrating the category for each link separately
     */
    private Map<PenaltyGroupKey, Double> loadInitialPenaltiesFromNetwork() {
        Map<PenaltyGroupKey, Double> sums = new HashMap<>();
        Map<PenaltyGroupKey, Integer> counts = new HashMap<>();

        for (Link link : network.getLinks().values()) {
            PenaltyGroupKey key = categorizer.getPenaltyGroupKey(link);
            if (key == null) {
                continue;
            }

            PenaltyGroupKey calibrationKey = penaltyKeyManager.toCalibrationKey(key);
            if (calibrationKey == null) {
                continue;
            }

            OptionalDouble penalty = NetworkCalibrationUtils.readDoubleAttribute(link, NetworkCalibrationUtils.PENALTY_ATTRIBUTE);
            if (penalty.isEmpty()) {
                continue;
            }

            // Stored link attribute is interpreted as plain penalty factor.
            double baseCategoryPenalty = penalty.getAsDouble();
            if (!Double.isFinite(baseCategoryPenalty)) {
                continue;
            }

            sums.merge(calibrationKey, baseCategoryPenalty, Double::sum);
            counts.merge(calibrationKey, 1, Integer::sum);
        }

        Map<PenaltyGroupKey, Double> initial = new HashMap<>();
        for (Map.Entry<PenaltyGroupKey, Double> entry : sums.entrySet()) {
            PenaltyGroupKey key = entry.getKey();
            int count = counts.getOrDefault(key, 0);
            if (count > 0) {
                initial.put(key, entry.getValue() / count);
            }
        }

        logger.info("Loaded initial penalties from network attributes for {} groups.", initial.size());
        return initial;
    }

    /**
     * Calculates the effective beta based on iteration for smoothing penalty updates.
     */
    private double getEffectiveBeta(int iteration) {
        double factor;
        if (iteration <= 30) {
            factor = 2.0;
        } else if (iteration <= 40) {
            factor = 1.2;
        } else if (iteration <= 60) {
            factor = 0.8;
        } else {
            factor = 0.5;
        }
        return beta * factor;
    }

    /**
     * Updates penalties for all categories to calibrate based on flow vs count discrepancies.
     */
    public void updatePenalties(int iteration) {
        for (PenaltyGroupKey key : countsProcessor.getGroups()) {
            Double count = countsProcessor.getAverageCountForGroup(key);
            if (count > 0.0 && Double.isFinite(count)) {
                double flow = flowProcessor.getFlowByGroup(key, sampleSize);
                if (flow >= 0.0 && Double.isFinite(flow)) {
                    double percentageDifference = (flow - count) / count;
                    double effectiveBeta = getEffectiveBeta(iteration);
                    double unbiasedError = flowProcessor.getUnbiasedError(key);
                    boolean doUpdate = flowProcessor.doUpdate(key);

                    penaltyManager.updatePenalty(key, percentageDifference, effectiveBeta, iteration, unbiasedError, doUpdate);
                }
            }
        }
        penaltyManager.logStatistics(iteration);
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
        if (isActivated && isCalibrating) {
            flowProcessor.updateAndSaveCounts(iterationEndsEvent);

            if (penaltyManager.isCalibrating()) {
                int iteration = iterationEndsEvent.getIteration();
                if (updateInterval > 0 && iteration % updateInterval == 0 && iteration >=warmupIterations) {
                    updatePenalties(iteration);
                    savePenalties(iteration);
                }
            }
        }
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
        if (isActivated && isCalibrating) {
            flowProcessor.resetCounts(iterationStartsEvent.getIteration());
        }
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        if (!isActivated) {
            return;
        }
        // save penalties in the network
        int persisted = 0;
        for (Link link : network.getLinks().values()) {
            PenaltyGroupKey calibrationKey = penaltyKeyManager.toCalibrationKey(link);
            if (calibrationKey == null) {
                continue;
            }

            double penaltyFactor = Math.round(penaltyManager.getPenalty(calibrationKey) * 1000.0)/1000.0;
            NetworkCalibrationUtils.writeDoubleAttribute(link, NetworkCalibrationUtils.PENALTY_ATTRIBUTE, penaltyFactor);
            persisted++;
        }
        logger.info("Persisted penalty attribute '{}' for {} links.", NetworkCalibrationUtils.PENALTY_ATTRIBUTE, persisted);

        // return the penalties too, in real keys format
        String filename = outputHierarchy.getOutputFilename("final_link_category_penalties.csv");
        penaltyManager.saveToCsvWithAllKeys(filename, penaltyKeyManager);
    }

    /**
     * Saves current penalties to a CSV file for the given iteration.
     */
    private void savePenalties(int iteration) {
        String filename = outputHierarchy.getIterationFilename(iteration, "link_category_penalties.csv");
        penaltyManager.saveToCsv(filename);
    }


    public void disable(){
        this.disable = true;
    }

    public void enable(){
        this.disable = false;
    }

}