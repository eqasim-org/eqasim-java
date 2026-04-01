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
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import java.util.List;

/**
 * Adapts penalties for link categories based on flow and count data.
 * Penalties are used to adjust routing costs for calibration purposes.
 * Supports advanced features like convergence detection and adaptive learning rates.
 */
public class PenaltiesAdapter implements IterationStartsListener, IterationEndsListener {
    private static final Logger logger = LogManager.getLogger(PenaltiesAdapter.class);

    private final CountsProcessor countsProcessor;
    private final FlowProcessor flowProcessor;
    private final double sampleSize;
    private final List<Integer> categoriesToCalibrate;
    private final PenaltyManager penaltyManager;
    private final double beta;
    private final int updateInterval;
    private final OutputDirectoryHierarchy outputHierarchy;
    private final double rampFactor;
    private final double trunkFactor;
    private final LinkCategorizer categorizer;
    private final boolean isActivated;
    private final boolean isCalibrating;
    private final int warmupIterations;
    private boolean disable = false;
    /**
     * Constructs a PenaltiesAdapter with the given parameters.
     * Initializes penalties from file if provided, or throws error if calibration is disabled without file.
     */
    public PenaltiesAdapter(Provider<CountsProcessor> countsProcessorProvider, Provider<FlowProcessor> flowProcessorProvider,
                            NetworkCalibrationConfigGroup config, OutputDirectoryHierarchy outputHierarchy,
                            EqasimConfigGroup eqasimConfig, LinkCategorizer categorizer, PenaltyManager penaltyManager) {
        this.sampleSize = eqasimConfig.getSampleSize();
        this.categoriesToCalibrate = config.getCategoriesToCalibrationAsList();
        this.updateInterval = config.getUpdateInterval();
        this.beta = config.getBeta();
        this.outputHierarchy = outputHierarchy;
        this.rampFactor = config.getRampFactor();
        this.trunkFactor = config.getTrunkFactor();
        this.categorizer = categorizer;
        this.penaltyManager = penaltyManager;
        this.isActivated = config.isOneOfObjectives("penalty") && config.isActivated();
        this.isCalibrating = this.isActivated && config.isCalibrationEnabled();
        this.warmupIterations = config.getPenaltiesWarmupIterations();

        this.countsProcessor = isCalibrating ? countsProcessorProvider.get() : null;
        this.flowProcessor = isCalibrating ? flowProcessorProvider.get() : null;

        if (isActivated) {
            penaltyManager.loadFromCsv(config.getPenaltiesFile());

            if (!isCalibrating) {
                logger.info("Penalty objective is active in fixed mode. Loaded penalties are kept constant during simulation.");
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

        int category = categorizer.getCategory(link);
        if (category == LinkCategorizer.UNKNOWN_CATEGORY || !categoriesToCalibrate.contains(category)) {
            return 0.0;
        }
        double penalty = penaltyManager.getPenalty(category);
        double travelTime = link.getLength() / link.getFreespeed();

        // Apply corrections for ramps and trunks
        if (NetworkCalibrationUtils.isRamp(link)) {
            penalty *= rampFactor;
        }
        if (NetworkCalibrationUtils.isTrunk(link)) {
            penalty *= trunkFactor;
        }
        return travelTime * penalty;
    }

    /**
     * Calculates the effective beta based on iteration for smoothing penalty updates.
     */
    private double getEffectiveBeta(int iteration) {
        double factor;
        if (iteration <= 20) {
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
        for (Integer category : categoriesToCalibrate) {
            if (categorizer.getAllCategories().contains(category)) {
                Double count = countsProcessor.getAverageCountForCategory(category);
                if (count > 0.0 && Double.isFinite(count)) {
                    double flow = flowProcessor.getFlowByCategory(category, sampleSize);
                    if (flow >= 0.0 && Double.isFinite(flow)) {
                        double percentageDifference = (flow - count) / count;
                        double effectiveBeta = getEffectiveBeta(iteration);
                        double unbiasedError = flowProcessor.getUnbiasedError(category);
                        boolean doUpdate = flowProcessor.doUpdate(category);

                        penaltyManager.updatePenalty(category, percentageDifference, effectiveBeta, iteration, unbiasedError, doUpdate);
                    }
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