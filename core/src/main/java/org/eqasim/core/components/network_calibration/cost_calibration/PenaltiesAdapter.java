package org.eqasim.core.components.network_calibration.cost_calibration;

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
    private final Network network;
    private final PenaltyManager penaltyManager;
    private final double beta;
    private final int updateInterval;
    private final int saveNetworkInterval;
    private final OutputDirectoryHierarchy outputHierarchy;
    private final double rampFactor;
    private final double trunkFactor;
    private final LinkCategorizer categorizer;
    private final boolean isActivated;

    /**
     * Constructs a PenaltiesAdapter with the given parameters.
     * Initializes penalties from file if provided, or throws error if calibration is disabled without file.
     */
    public PenaltiesAdapter(CountsProcessor countsProcessor, FlowProcessor flowProcessor, Network network,
                            NetworkCalibrationConfigGroup config, OutputDirectoryHierarchy outputHierarchy,
                            EqasimConfigGroup eqasimConfig, LinkCategorizer categorizer, PenaltyManager penaltyManager) {
        this.countsProcessor = countsProcessor;
        this.flowProcessor = flowProcessor;
        this.network = network;
        this.sampleSize = eqasimConfig.getSampleSize();
        this.categoriesToCalibrate = config.getCategoriesToCalibrationAsList();
        this.updateInterval = config.getUpdateInterval();
        this.saveNetworkInterval = config.getSaveNetworkInterval();
        this.beta = config.getBeta();
        this.outputHierarchy = outputHierarchy;
        this.rampFactor = config.getRampFactor();
        this.trunkFactor = config.getTrunkFactor();
        this.categorizer = categorizer;
        this.penaltyManager = penaltyManager;
        this.isActivated = config.isOneOfObjectives("penalty") && config.isActivated();

        if (isActivated) {
            // Load penalties from file if provided
            penaltyManager.loadFromCsv(config.getPenaltiesFile());

            // Adjust network capacities initially
            NetworkCalibrationUtils.adjustNetworkCapacities(network, config.getMinCapacity(), config.getMaxCapacity(), sampleSize,
                    config.getCorrectCapacities(), config.getMinSpeed(), categorizer);
        }
    }

    /**
     * Computes the penalty for a given link based on its category.
     * Applies factors for ramps and trunks.
     */
    public double computePenalty(Link link) {
        Integer category = countsProcessor.getLinkCategory(link.getId());
        if (category == null || !categoriesToCalibrate.contains(category)) {
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
    private double getEffectiveBeta(double percentageError, int iteration) {
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
                double count = countsProcessor.getAverageCountForCategory(category);
                if (count > 0.0 && Double.isFinite(count)) {
                    double flow = flowProcessor.getFlowByCategory(category, sampleSize);
                    if (flow >= 0.0 && Double.isFinite(flow)) {
                        double percentageDifference = (flow - count) / count;
                        double effectiveBeta = getEffectiveBeta(percentageDifference, iteration);
                        penaltyManager.updatePenalty(category, percentageDifference, effectiveBeta, iteration);
                    }
                }
            }
        }
        penaltyManager.logStatistics(iteration);
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
        if (isActivated) {
            flowProcessor.updateAndSaveCounts(iterationEndsEvent);

            if (penaltyManager.isCalibrating()) {
                int iteration = iterationEndsEvent.getIteration();
                if (updateInterval > 0 && iteration % updateInterval == 0 && iteration > 0) {
                    updatePenalties(iteration);
                    savePenalties(iteration);
                }
            }
        }
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
        if (isActivated) {
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

}