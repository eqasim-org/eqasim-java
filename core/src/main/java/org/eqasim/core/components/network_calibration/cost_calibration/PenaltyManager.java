package org.eqasim.core.components.network_calibration.cost_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages penalties for link categories, supporting initialization from CSV and updates during calibration.*
 */
public class PenaltyManager {
    private static final Logger logger = LogManager.getLogger(PenaltyManager.class);

    private final Map<Integer, Double> penalties = new HashMap<>();
    private final Map<Integer, Double> previousPenalties = new HashMap<>();
    private final double minPenalty;
    private final double maxPenalty;
    private final boolean calibrate;

    /**
     * Constructs a PenaltyManager with bounds and calibration flag.
     * @param minPenalty Minimum allowed penalty value.
     * @param maxPenalty Maximum allowed penalty value.
     * @param calibrate Whether to enable calibration updates.*
     */
    public PenaltyManager(double minPenalty, double maxPenalty, boolean calibrate) {
        this.minPenalty = minPenalty;
        this.maxPenalty = maxPenalty;
        this.calibrate = calibrate;
    }

    /**
     * Loads penalties from a CSV file if provided.
     * @param penaltiesFile Path to the penalties CSV file.
     */
    public void loadFromCsv(String penaltiesFile) {
        if (penaltiesFile != null && !penaltiesFile.isEmpty() && !penaltiesFile.equals("none")) {
            PenaltyCsvHandler.readPenaltiesFromFile(penaltiesFile, penalties);
            logger.info("Loaded {} penalty categories from file: {}", penalties.size(), penaltiesFile);
        } else if (!calibrate) {
            throw new IllegalArgumentException("Penalties file must be provided if calibration is disabled.");
        } else {
            logger.info("No penalties file provided, starting with zero penalties for calibration.");
        }
    }

    /**
     * Gets the penalty for a category, defaulting to 0.0 if not set.
     */
    public double getPenalty(int category) {
        return penalties.getOrDefault(category, 0.0);
    }

    /**
     * Sets the penalty for a category, clamping to bounds.
     */
    public void setPenalty(int category, double penalty) {
        double clampedPenalty = Math.min(Math.max(penalty, minPenalty), maxPenalty);
        penalties.put(category, clampedPenalty);
    }

    /**
     * Updates the penalty for a category using adaptive learning rate.
     * @param category The category to update.
     * @param percentageDifference The flow vs count difference ratio.
     * @param effectiveBeta The learning rate.
     * @param iteration Current iteration number.
     */
    public void updatePenalty(int category, double percentageDifference, double effectiveBeta, int iteration, double  unbiasedError, boolean doUpdate) {
        if (!calibrate || !doUpdate) return;

        double currentPenalty = getPenalty(category);
        previousPenalties.put(category, currentPenalty);

        // Adaptive update: reduce learning rate for large changes to prevent oscillations
        double adaptiveBeta = effectiveBeta;
        if (Math.abs(percentageDifference) > 0.3) {
            adaptiveBeta *= 0.5; // Reduce learning rate for large discrepancies
        }

        // Use exponential moving average for stability
        double newPenalty;
        if (Double.isFinite(unbiasedError)) {
            newPenalty = currentPenalty + adaptiveBeta * unbiasedError;
        } else {
            newPenalty = currentPenalty + adaptiveBeta * percentageDifference;
        }


        // Add small regularization
        if (Math.abs(newPenalty) < 5e-3) {
            newPenalty = 0.0;
        }

        setPenalty(category, newPenalty);

        logger.debug("Updated penalty for category {}: {} -> {} (diff: {:.3f}, beta: {:.3f})",
                category, currentPenalty, newPenalty, percentageDifference, adaptiveBeta);
    }

    /**
     * Returns a copy of the penalties map.
     */
    public Map<Integer, Double> getAllPenalties() {
        return new HashMap<>(penalties);
    }

    /**
     * Saves penalties to a CSV file.
     */
    public void saveToCsv(String filename) {
        PenaltyCsvHandler.writePenaltiesToFile(filename, penalties);
    }

    /**
     * Checks if calibration is enabled.
     */
    public boolean isCalibrating() {
        return calibrate;
    }

    /**
     * Gets the number of categories with non-zero penalties.
     */
    public int getActiveCategoriesCount() {
        return (int) penalties.values().stream().filter(p -> Math.abs(p) > 1e-3).count();
    }

    /**
     * Validates penalty values and logs statistics.
     */
    public void logStatistics(int iteration) {
        int totalCategories = penalties.size();
        int activeCategories = getActiveCategoriesCount();
        double avgPenalty = penalties.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double maxAbsPenalty = penalties.values().stream().mapToDouble(Math::abs).max().orElse(0.0);

        logger.info("Iteration {}: {} active penalty categories out of {}, avg: {:.4f}, max: {:.4f}",
                iteration, activeCategories, totalCategories, avgPenalty, maxAbsPenalty);
    }
}
