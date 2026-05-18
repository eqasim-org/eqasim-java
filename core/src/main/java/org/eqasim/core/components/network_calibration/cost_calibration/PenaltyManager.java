package org.eqasim.core.components.network_calibration.cost_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages penalties for link categories, supporting initialization from CSV and updates during calibration.*
 */
public class PenaltyManager {
    private static final Logger logger = LogManager.getLogger(PenaltyManager.class);

    private final Map<PenaltyGroupKey, Double> penalties = new HashMap<>();
    private final Map<PenaltyGroupKey, Double> previousPenalties = new HashMap<>();
    private final double minPenalty;
    private final double maxPenalty;
    private final boolean calibrate;

    /**
     * Constructs a PenaltyManager with bounds and calibration flag.
     */
    public PenaltyManager(NetworkCalibrationConfigGroup config) {
        this.minPenalty = config.getMinPenalty();
        this.maxPenalty = config.getMaxPenalty();
        this.calibrate = config.isOneOfObjectives("penalty") && config.isActivated() && config.isCalibrationEnabled();
    }

    /**
     * Loads penalties from a CSV file if provided.
     * @param penaltiesFile Path to the penalties CSV file.
     */
    public void loadFromCsv(String penaltiesFile) {
        if (penaltiesFile != null && !penaltiesFile.isEmpty() && !penaltiesFile.equals("none")) {
            PenaltyCsvHandler.readPenaltiesFromFile(penaltiesFile, penalties);
            logger.info("Loaded {} penalty groups from file: {}", penalties.size(), penaltiesFile);
        } else {
            logger.info("No penalties file provided, keeping existing penalties (from defaults/network attributes).");
        }
    }

    public void loadInitialPenalties(Map<PenaltyGroupKey, Double> initialPenalties) {
        penalties.clear();
        previousPenalties.clear();

        if (initialPenalties == null || initialPenalties.isEmpty()) {
            logger.info("No initial penalties provided. Falling back to zero penalties by default.");
            return;
        }

        for (Map.Entry<PenaltyGroupKey, Double> entry : initialPenalties.entrySet()) {
            setPenalty(entry.getKey(), entry.getValue());
        }
        logger.info("Initialized {} penalty groups from input values.", penalties.size());
    }

    /**
     * Gets the penalty for a category, defaulting to 0.0 if not set.
     */
    public double getPenalty(PenaltyGroupKey key) {
        return penalties.getOrDefault(key, 0.0);
    }

    /**
     * Sets the penalty for a category, clamping to bounds.
     */
    public void setPenalty(PenaltyGroupKey key, double penalty) {
        double clampedPenalty = clip(penalty, minPenalty, maxPenalty);
        penalties.put(key, clampedPenalty);
    }

    private double clip(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }

    /**
     * Updates the penalty for a category using adaptive learning rate.
     * @param key The penalty group key.
     * @param percentageDifference The flow vs count difference ratio.
     * @param effectiveBeta The learning rate.
     * @param iteration Current iteration number.
     * @param unbiasedError The unbiased error for this category (if available).
     * @param doUpdate Whether to perform the update (can be false for logging only).
     */
    public void updatePenalty(PenaltyGroupKey key, double percentageDifference, double effectiveBeta, int iteration, double  unbiasedError, boolean doUpdate) {
        if (!calibrate || !doUpdate) return;

        double currentPenalty = getPenalty(key);
        previousPenalties.put(key, currentPenalty);

        // Adaptive update: reduce learning rate for large changes to prevent oscillations
        double adaptiveBeta = effectiveBeta;
        if (Math.abs(percentageDifference) > 0.3) {
            adaptiveBeta *= 0.5; // Reduce learning rate for large discrepancies
        }

        // Use exponential moving average for stability
        double consideredError = Double.isFinite(unbiasedError) ? unbiasedError: percentageDifference;
        double deltaPenalty = adaptiveBeta * consideredError;

        boolean clipProgression = iteration>=60;
        if (clipProgression) {
            if (deltaPenalty > 0.005) {
                deltaPenalty = clip(deltaPenalty, 0.01, 0.1);
            } else if (deltaPenalty < -0.005) {
                deltaPenalty = clip(deltaPenalty, -0.1, -0.01);
            } else {
                deltaPenalty = 0.0;
            }
        }
        double newPenalty = currentPenalty + deltaPenalty;

        // Add small regularization
        if (Math.abs(newPenalty) < 5e-3) {
            newPenalty = 0.0;
        }

        setPenalty(key, newPenalty);
        logger.debug("Updated penalty for group {}: {} -> {} (diff: {}, beta: {})",
            key, currentPenalty, newPenalty, percentageDifference, adaptiveBeta);
    }

    /**
     * Returns a copy of the penalties map.
     */
    public Map<PenaltyGroupKey, Double> getAllPenalties() {
        return new HashMap<>(penalties);
    }

    /**
     * Saves penalties to a CSV file.
     */
    public void saveToCsv(String filename) {
        PenaltyCsvHandler.writePenaltiesToFile(filename, penalties);
    }

    public void saveToCsvWithAllKeys(String filename, PenaltyKeyManager penaltyKeyManager) {
        Map<PenaltyGroupKey, Double> pen = new HashMap<>();
        Map<PenaltyGroupKey, PenaltyGroupKey> keyMapping = penaltyKeyManager.getKeyMapping();
        for (Map.Entry<PenaltyGroupKey, PenaltyGroupKey> entry : keyMapping.entrySet()) {
            PenaltyGroupKey realKey = entry.getKey();
            PenaltyGroupKey mappedKey = entry.getValue();
            double penalty = getPenalty(mappedKey);
            pen.put(realKey, penalty);
        }
        PenaltyCsvHandler.writePenaltiesToFile(filename, pen);
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

        logger.info("Iteration {}: {} active penalty categories out of {}, avg: {}, max: {}",
                iteration, activeCategories, totalCategories, avgPenalty, maxAbsPenalty);
    }
}
