package org.eqasim.core.components.network_calibration.freespeed_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class FreespeedFactorManager {
    private static final Logger logger = LogManager.getLogger(FreespeedFactorManager.class);

    private final Map<LinkGroupKey, Double> factors = new HashMap<>();
    private final double minFactor;
    private final double maxFactor;
    private final double beta;
    private final boolean calibrate;
    private final int minTripsPerGroup;

    public FreespeedFactorManager(double minFactor, double maxFactor, double beta,
                                  boolean calibrate, int minTripsPerGroup) {
        this.minFactor = minFactor;
        this.maxFactor = maxFactor;
        this.beta = beta;
        this.calibrate = calibrate;
        this.minTripsPerGroup = minTripsPerGroup;
    }

    public double getFactor(LinkGroupKey key) {
        return factors.getOrDefault(key, 1.0);
    }

    public Map<LinkGroupKey, Double> getFactorsSnapshot() {
        return new HashMap<>(factors);
    }

    public void loadFactors(Map<LinkGroupKey, Double> loadedFactors) {
        factors.clear();
        for (Map.Entry<LinkGroupKey, Double> entry : loadedFactors.entrySet()) {
            factors.put(entry.getKey(), clip(entry.getValue()));
        }
    }

    public boolean isCalibrating() {
        return calibrate;
    }

    public void updateFactors(Map<LinkGroupKey, GroupStats> groupStats) {
        if (!calibrate) {
            return;
        }

        for (Map.Entry<LinkGroupKey, GroupStats> entry : groupStats.entrySet()) {
            GroupStats stats = entry.getValue();
            if (stats.tripCount < minTripsPerGroup || stats.observedTime <= 1.0 || stats.simulatedTime <= 1.0) {
                continue;
            }

            double adjustment = stats.simulatedTime / stats.observedTime;
            if (!Double.isFinite(adjustment)) {
                continue;
            }

            double smoothedAdjustment = beta + (1.0 - beta) * adjustment;
            double currentFactor = factors.getOrDefault(entry.getKey(), 1.0);
            double updatedFactor = clip(currentFactor * smoothedAdjustment);
            factors.put(entry.getKey(), updatedFactor);
        }

        logger.info("Updated freespeed factors for {} groups", factors.size());
    }

    private double clip(double factor) {
        return Math.max(minFactor, Math.min(maxFactor, factor));
    }

    public static class GroupStats {
        public double simulatedTime;
        public double observedTime;
        public double simulatedDistance;
        public double observedDistance;
        public int tripCount;
    }
}


