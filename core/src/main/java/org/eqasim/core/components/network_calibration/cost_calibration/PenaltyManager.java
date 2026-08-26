package org.eqasim.core.components.network_calibration.cost_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages penalties for link categories, supporting initialization from CSV and updates during calibration.*
 */
public class PenaltyManager {
    private static final Logger logger = LogManager.getLogger(PenaltyManager.class);
    private static final int LAST_PENALTY_WINDOW = 4;

    private final Map<PenaltyGroupKey, Double> penalties = new HashMap<>();
    private final Map<PenaltyGroupKey, Deque<Double>> lastFourPenalties = new HashMap<>();
    private final double minPenalty;
    private final double maxPenalty;
    private final boolean calibrate;

    /**
     * Constructs a PenaltyManager with bounds and calibration flag.
     */
    public PenaltyManager(NetworkCalibrationConfigGroup config, CostCalibrationConfigGroup costConfig) {
        this.minPenalty = costConfig.getMinPenalty();
        this.maxPenalty = costConfig.getMaxPenalty();
        this.calibrate = config.isCostCalibrationActivated() && config.isActivated() && config.isCalibrationEnabled();
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
        lastFourPenalties.clear();

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

    public double getAverageOfLastFourPenalties(PenaltyGroupKey key) {
        Deque<Double> history = lastFourPenalties.get(key);
        if (history == null || history.isEmpty()) {
            return getPenalty(key);
        }

        double sum = 0.0;
        for (double value : history) {
            sum += value;
        }
        return sum / history.size();
    }

    private void recordPenaltyHistory(PenaltyGroupKey key, double penalty) {
        Deque<Double> history = lastFourPenalties.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        history.addLast(penalty);
        while (history.size() > LAST_PENALTY_WINDOW) {
            history.removeFirst();
        }
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
     * Applies a bounded robust-error update. There is deliberately no minimum
     * non-zero step: updates can become arbitrarily small near equilibrium.
     *
     * @return the actual change after both step clipping and penalty bounds
     */
    public double updatePenalty(PenaltyGroupKey key, double robustError, double learningRate,
                                double maximumUpdate) {
        if (!calibrate || !Double.isFinite(robustError)) {
            return 0.0;
        }
        if (!Double.isFinite(learningRate) || learningRate < 0.0
                || !Double.isFinite(maximumUpdate) || maximumUpdate <= 0.0) {
            throw new IllegalArgumentException("Learning rate must be finite and non-negative, and maximum update must be positive.");
        }

        double currentPenalty = getPenalty(key);
        double requestedChange = clip(learningRate * robustError, -maximumUpdate, maximumUpdate);
        setPenalty(key, currentPenalty + requestedChange);
        double actualChange = getPenalty(key) - currentPenalty;
        recordPenaltyHistory(key, getPenalty(key));

        logger.debug("Updated penalty for group {}: {} -> {} (robust error: {}, learning rate: {}, actual change: {})",
                key, currentPenalty, getPenalty(key), robustError, learningRate, actualChange);
        return actualChange;
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
            double penalty = getAverageOfLastFourPenalties(mappedKey);
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
