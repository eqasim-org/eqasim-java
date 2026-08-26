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
    private final int updateInterval;
    private final OutputDirectoryHierarchy outputHierarchy;
    private final double rampFactor;
    private final double trunkFactor;
    private final LinkCategorizer categorizer;
    private final PenaltyKeyManager penaltyKeyManager;
    private final boolean isActivated;
    private final boolean isCalibrating;
    private final int warmupIterations;
    private final int endIteration;
    private final boolean hasPenaltiesFile;
    private final double initialLearningRate;
    private final double minimumLearningRate;
    private final double learningRateDecayScale;
    private final double learningRateDecayExponent;
    private final double maximumPenaltyUpdate;
    private final double robustErrorThreshold;
    private final double robustErrorEpsilon;
    private final double sampleSizeShrinkage;
    private final double signReversalFactor;
    private final double signReversalThreshold;
    private final double minimumGainMultiplier;
    private final double gainRecoveryRate;
    private final Map<PenaltyGroupKey, Double> previousRobustErrors = new HashMap<>();
    private final Map<PenaltyGroupKey, Double> gainMultipliers = new HashMap<>();
    private int penaltyUpdateCount = 0;
    private boolean disable = false;
    /**
     * Constructs a penalties adapter and initializes penalties from network attributes and/or CSV.
     */
    public PenaltiesAdapter(Network network,
                            Provider<CountsProcessor> countsProcessorProvider, Provider<FlowProcessor> flowProcessorProvider,
                            NetworkCalibrationConfigGroup config, CostCalibrationConfigGroup costConfig,
                            OutputDirectoryHierarchy outputHierarchy,
                            EqasimConfigGroup eqasimConfig, LinkCategorizer categorizer,
                            PenaltyKeyManager penaltyKeyManager, PenaltyManager penaltyManager) {
        this.network = network;
        this.sampleSize = eqasimConfig.getSampleSize();
        this.updateInterval = costConfig.getUpdateInterval();
        this.outputHierarchy = outputHierarchy;
        this.rampFactor = costConfig.getRampFactor();
        this.trunkFactor = costConfig.getTrunkFactor();
        this.categorizer = categorizer;
        this.penaltyKeyManager = penaltyKeyManager;
        this.penaltyManager = penaltyManager;
        this.isActivated = config.isCostCalibrationActivated() && config.isActivated();
        this.isCalibrating = this.isActivated && config.isCalibrationEnabled();
        this.warmupIterations = costConfig.getWarmupIterations();
        this.endIteration = costConfig.getEndIteration();
        this.hasPenaltiesFile = costConfig.hasPenaltiesFile();
        this.initialLearningRate = costConfig.getInitialLearningRate();
        this.minimumLearningRate = costConfig.getMinimumLearningRate();
        this.learningRateDecayScale = costConfig.getLearningRateDecayScale();
        this.learningRateDecayExponent = costConfig.getLearningRateDecayExponent();
        this.maximumPenaltyUpdate = costConfig.getMaximumPenaltyUpdate();
        this.robustErrorThreshold = costConfig.getRobustErrorThreshold();
        this.robustErrorEpsilon = costConfig.getRobustErrorEpsilon();
        this.sampleSizeShrinkage = costConfig.getSampleSizeShrinkage();
        this.signReversalFactor = costConfig.getSignReversalFactor();
        this.signReversalThreshold = costConfig.getSignReversalThreshold();
        this.minimumGainMultiplier = costConfig.getMinimumGainMultiplier();
        this.gainRecoveryRate = costConfig.getGainRecoveryRate();

        this.countsProcessor = isCalibrating ? countsProcessorProvider.get() : null;
        this.flowProcessor = isCalibrating ? flowProcessorProvider.get() : null;

        if (isActivated) {
            if (isCalibrating) {
                penaltyManager.loadInitialPenalties(Map.of());
                logger.info("Penalty calibration starts from zero; network attributes and penalties CSV are ignored.");
            } else {
                penaltyManager.loadInitialPenalties(loadInitialPenaltiesFromNetwork());
                if (hasPenaltiesFile) {
                    penaltyManager.loadFromCsv(costConfig.getPenaltiesFile());
                }
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
            return penalty * rampFactor;
        }
        if (NetworkCalibrationUtils.isTrunk(link)) {
            return penalty * trunkFactor;
        }
        return penalty;
    }

    /**
     * Loads initial penalties from network attributes by averaging values for each category.
     * We could, in this case, get the penalty from the link directly and use it, but this way would make it
     * consistent with the calibration, as we are not calibrating the category for each link separately
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
     * All listeners here
     */

    @Override
    public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
        if (isActivated && isCalibrating) {
            flowProcessor.updateAndSaveCounts(iterationEndsEvent);

            if (penaltyManager.isCalibrating()) {
                int iteration = iterationEndsEvent.getIteration();
                if (updateInterval > 0 && iteration >=warmupIterations && iteration <= endIteration) {
                    if ((iteration-warmupIterations) % updateInterval == 0 || iteration == endIteration) {
                        updatePenalties(iteration);
                        savePenalties(iteration);
                    }
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
        if (!isCalibrating) {
            return;
        }
        // save penalties in the network
        int persisted = 0;
        for (Link link : network.getLinks().values()) {
            PenaltyGroupKey calibrationKey = penaltyKeyManager.toCalibrationKey(link);
            if (calibrationKey == null) {
                continue;
            }

            double penaltyFactor = Math.round(penaltyManager.getAverageOfLastFourPenalties(calibrationKey) * 1000.0)/1000.0;
            NetworkCalibrationUtils.writeDoubleAttribute(link, NetworkCalibrationUtils.PENALTY_ATTRIBUTE, penaltyFactor);
            persisted++;
        }
        logger.info("Persisted penalty attribute '{}' for {} links.", NetworkCalibrationUtils.PENALTY_ATTRIBUTE, persisted);

        // return the penalties too, in real keys format
        String filename = outputHierarchy.getOutputFilename("final_link_category_penalties.csv");
        penaltyManager.saveToCsvWithAllKeys(filename, penaltyKeyManager);
    }


    /**
     * Updates penalties for all categories to calibrate based on flow vs count discrepancies.
     */
    public void updatePenalties(int iteration) {
        double baseLearningRate = getBaseLearningRate();

        for (PenaltyGroupKey key : countsProcessor.getGroups()) {
            Double count = countsProcessor.getAverageCountForGroup(key);
            if (count > 0.0 && Double.isFinite(count)) {
                double flow = flowProcessor.getFlowByGroup(key, sampleSize);
                if (flow >= 0.0 && Double.isFinite(flow)) {
                    double percentageDifference = (flow - count) / count;
                    FlowProcessor.RobustGroupError robustError = flowProcessor.getRobustGroupError(
                            key, robustErrorThreshold, robustErrorEpsilon, sampleSizeShrinkage);
                    double gainMultiplier = updateGainMultiplier(key, robustError.score());
                    double effectiveLearningRate = baseLearningRate * gainMultiplier;
                    double actualChange = penaltyManager.updatePenalty(
                            key, robustError.score(), effectiveLearningRate, maximumPenaltyUpdate);

                    logger.info("Penalty calibration group {}: observations={}, effective observations={}, "
                                    + "raw robust error={}, shrunk error={}, mean difference={}, gain={}, change={}",
                            key, robustError.observations(), robustError.effectiveSampleSize(), robustError.rawScore(),
                            robustError.score(), percentageDifference, effectiveLearningRate, actualChange);
                }
            }
        }

        penaltyUpdateCount++;
        logger.info("Penalty update {} at iteration {} used base learning rate {}.",
                penaltyUpdateCount, iteration, baseLearningRate);
        penaltyManager.logStatistics(iteration);
    }

    /**
     * Learning rates and gains are adjusted based on the number of penalty updates and the observed errors.
     */
    private double getBaseLearningRate() {
        double decay = Math.pow(1.0 + penaltyUpdateCount / learningRateDecayScale, learningRateDecayExponent);
        return minimumLearningRate + (initialLearningRate - minimumLearningRate) / decay;
    }

    private double updateGainMultiplier(PenaltyGroupKey key, double robustError) {
        double multiplier = gainMultipliers.getOrDefault(key, 1.0);
        Double previousError = previousRobustErrors.put(key, robustError);

        boolean meaningfulReversal = previousError != null
                && previousError * robustError < 0.0
                && Math.abs(previousError) >= signReversalThreshold
                && Math.abs(robustError) >= signReversalThreshold;

        if (meaningfulReversal) {
            multiplier = Math.max(minimumGainMultiplier, multiplier * signReversalFactor);
        } else {
            multiplier += gainRecoveryRate * (1.0 - multiplier);
        }

        gainMultipliers.put(key, multiplier);
        return multiplier;
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
