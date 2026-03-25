package org.eqasim.core.components.network_calibration.freespeed_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import java.util.*;

public class FreespeedFactorManager {
    private static final Logger logger = LogManager.getLogger(FreespeedFactorManager.class);
    private static final int HISTORY_SIZE = 5;
    private static final double MIN_EFFECTIVE_BETA = 0.2;
    private static final double MAX_EFFECTIVE_BETA = 0.8;
    private static final double MAX_FACTOR_STEP = 0.08;
    private static final double MIN_IMPROVEMENT_RATIO = 0.015;
    private static final int NO_IMPROVEMENT_PATIENCE = 3;
    private static final int NUM_FROZEN_ITERATIONS = 3;

    private int NUM_UPDATES = 0;

    private final Map<LinkGroupKey, Double> factors = new HashMap<>();
    private final Map<LinkGroupKey, GroupDiagnostics> diagnostics = new HashMap<>();
    private final double minFactor;
    private final double maxFactor;
    private final double beta;
    private final boolean calibrate;
    private final int minTripsPerGroup;

    public FreespeedFactorManager(NetworkCalibrationConfigGroup config) {
        this.minFactor = config.getMinFreespeedFactor();
        this.maxFactor = config.getMaxFreespeedFactor();
        this.beta = config.getBeta();
        this.calibrate = config.isOneOfObjectives("freespeed") && config.isActivated() && config.isCalibrationEnabled();
        this.minTripsPerGroup = config.getMinTripsPerGroup();
    }

    public double getFactor(LinkGroupKey key) {
        return factors.getOrDefault(key, 1.0);
    }

    public Map<LinkGroupKey, Double> getFactorsSnapshot() {
        return new HashMap<>(factors);
    }

    public void loadFactors(Map<LinkGroupKey, Double> loadedFactors) {
        factors.clear();
        diagnostics.clear();

        for (Map.Entry<LinkGroupKey, Double> entry : loadedFactors.entrySet()) {
            double factor = clipFactor(entry.getValue());
            factors.put(entry.getKey(), factor);
        }
    }
    
    public void updateFactors(Map<LinkGroupKey, GroupStats> groupStats, int iteration) {
        if (!calibrate) {
            return;
        }
        int changed = 0;
        int updated = 0;
        int skipped = 0;
        int frozen = 0;
        NUM_UPDATES++;

        for (Map.Entry<LinkGroupKey, GroupStats> entry : groupStats.entrySet()) {
            LinkGroupKey key = entry.getKey();
            GroupStats stats = entry.getValue();
            double currentFactor = factors.getOrDefault(key, 1.0);
            GroupDiagnostics diag = diagnostics.computeIfAbsent(key, k -> new GroupDiagnostics(Decision.FIRST, currentFactor, Double.NaN));
            double currentError = Math.abs(stats.getAverageErrors());

            // If there isn't many trips, skip
            if (stats.tripCount < minTripsPerGroup || stats.observedTime <= 0.0 || stats.simulatedTime <= 0.0 || !Double.isFinite(currentError)) {
                diag.add(Decision.SKIPPED_INSUFFICIENT_TRIPS, currentFactor, Double.NaN);
                skipped++;
                continue;
            }

            // If it is frozen, do not update
            if (diag.isFrozen()) {
                diag.frozen++;
                diag.noImprovementStreak = 0;
                diag.lastlyFrozen = true;
                diag.add(Decision.SKIPPED_FROZEN, currentFactor, currentError);
                skipped++;
                frozen++;
                continue;
            }

            // If we have almost 50% overestimation and 50% underestimation, do not update.
            if (!stats.shouldUpdate()) {
                diag.noImprovementStreak = 0;
                skipped++;
                diag.add(Decision.SKIPPED_GOOD_FIT, currentFactor, currentError);
                continue;
            }

            // If no improvement, skipp it, if more than NO_IMPROVEMENT_PATIENCE, roll back to the last factor with an improvement
            // and keep it frozen for a few iterations, if lastly frozen, keep going (to try new values after some frozen iterations, might be interesting)
            Double previousError = diag.lastErrors.getFromLast(diag.noImprovementStreak); // compare against the latest improving point
            if (previousError == null) previousError = diag.lastErrors.getFromLast(0);

            if (previousError != null && Double.isFinite(previousError) && !isImproved(previousError, currentError) && !diag.lastlyFrozen) {
                diag.noImprovementStreak++;
                if (diag.noImprovementStreak >= NO_IMPROVEMENT_PATIENCE) {
                    diag.frozen++;
                    frozen++;
                    Double rollbackFactor = diag.lastFactors.getFromLast(diag.noImprovementStreak);
                    if (rollbackFactor == null || !Double.isFinite(rollbackFactor)) {
                        rollbackFactor = currentFactor;
                    }

                    double lastFactorWithImprovement = clipFactor(rollbackFactor);
                    factors.put(key, lastFactorWithImprovement);
                    diag.add(Decision.SKIPPED_NO_MORE_IMPROVEMENT, lastFactorWithImprovement, currentError);
                } else{
                    diag.add(Decision.SKIPPED_NO_MORE_IMPROVEMENT, currentFactor, currentError);
                }

                skipped++;
                continue;
            }

            // This is the last case, in this case, we do update the factors and reset the no improvement streak
            diag.noImprovementStreak = 0;
            diag.lastlyFrozen = false;

            double candidateFactor = clipFactor(stats.getAverageFactor() * currentFactor); // we multiply it here because the factor over the already existing one (multiplicative factor)
            if (!Double.isFinite(candidateFactor)) {
                skipped++;
                diag.add(Decision.SKIPPED_INSUFFICIENT_TRIPS, currentFactor, currentError);
                continue;
            }

            double effectiveBeta = clipValue(beta * 2.0 / NUM_UPDATES,  MIN_EFFECTIVE_BETA, MAX_EFFECTIVE_BETA);
            double rawStep = (candidateFactor - currentFactor) * effectiveBeta;
            double boundedStep = clipValue(rawStep, -MAX_FACTOR_STEP, MAX_FACTOR_STEP);
            double newFactor = clipFactor(currentFactor + boundedStep);

            factors.put(key, newFactor);
            diag.add(Decision.UPDATED, newFactor, currentError);
            updated++;
            if (!almostEqual(newFactor, currentFactor)) {
                changed++;
            }
        }

        logger.info("Freespeed update: groupsWithStats={}, totalGroupsWithFactors={}, updated={}, skipped={}, changed={}, frozen={}, iteration={}",
                groupStats.size(), factors.size(), updated, skipped, changed, frozen, iteration);
    }

    private static boolean isImproved(double previousError, double currentError) {
        if (!Double.isFinite(previousError)) {
            return true;
        }

        double threshold = previousError * (1.0 - MIN_IMPROVEMENT_RATIO);
        return currentError < threshold;
    }

    public Map<LinkGroupKey, GroupDiagnostics> getDiagnosticsSnapshot() {
        return new HashMap<>(diagnostics);
    }

    private double clipFactor(double factor) {
        return clipValue(factor, minFactor, maxFactor);
    }

    private double clipValue(double x, double min, double max) {
        return Math.max(min, Math.min(max, x));
    }

    private static boolean almostEqual(double x, double y) {
        return Math.abs(x - y) < 1.0e-6;
    }

    public enum Decision {
        FIRST,
        SKIPPED_INSUFFICIENT_TRIPS,
        SKIPPED_GOOD_FIT,
        SKIPPED_NO_MORE_IMPROVEMENT,
        SKIPPED_FROZEN,
        UPDATED,
    }

    public static class GroupDiagnostics {
        public final Q<Decision> decisions = new Q<>(HISTORY_SIZE);
        public final Q<Double> lastFactors = new Q<>(HISTORY_SIZE);
        public final Q<Double> lastErrors = new Q<>(HISTORY_SIZE);
        public int noImprovementStreak;
        public int frozen;
        public boolean lastlyFrozen = false;

        public GroupDiagnostics(Decision decision, double factor, double error) {
            this.decisions.add(decision);
            this.lastFactors.add(factor);
            this.lastErrors.add(error);
            this.noImprovementStreak = 0;
            this.frozen = 0;
        }

        public void add(Decision decision, double factor, double error){
            this.decisions.add(decision);
            this.lastFactors.add(factor);
            this.lastErrors.add(error);
        }

        public boolean isFrozen() {
            return (frozen > 0) && (frozen%NUM_FROZEN_ITERATIONS)!=0;
        }

    }

    public static class Q<T> {
        private final Deque<T> deque;
        private final int maxSize;

        public Q(int maxSize) {
            this.deque = new ArrayDeque<>();
            this.maxSize = maxSize;
        }

        public void add(T value) {
            if (deque.size() == maxSize) {
                deque.removeFirst(); // remove oldest (FIFO)
            }
            deque.addLast(value); // add newest
        }

        public List<T> toList() {
            return new ArrayList<>(deque);
        }

        public T getFromLast(int offset) {
            if (offset < 0) {
                throw new IllegalArgumentException("offset must be >= 0");
            }
            if (offset >= deque.size()) {
                return null;
            }
            Iterator<T> it = deque.descendingIterator();
            for (int i = 0; i < offset; i++) {
                it.next();
            }
            return it.next();
        }
    }

    public static class GroupStats {

        public double simulatedTime;
        public double observedTime;
        public int tripCount;
        public double nTot;
        public double nFast;
        public double nSlow;

        private final FloatArrayList factors = new FloatArrayList(16_384);
        private final FloatArrayList weights = new FloatArrayList(16_384);
        private final FloatArrayList errors = new FloatArrayList(16_384);

        private static final double EPSILON = 0.03;
        private static final double C = 0.05;

        public void addStat(double observedTravelTime, double simulatedTravelTime,
                            double weight, double length, double freespeed) {

            this.tripCount++;

            double simTt = simulatedTravelTime * weight;
            double realTt = observedTravelTime * weight;

            this.simulatedTime += simTt;
            this.observedTime += realTt;

            double diffSpeed = length / simTt - length / realTt;
            errors.add((float) diffSpeed);

            double factor = 1.0 - diffSpeed / freespeed;
            factors.add((float) factor);

            weights.add((float) weight);

            adaptCounts(realTt, simTt, weight);
        }

        private void adaptCounts(double realTt, double simTt, double weight) {
            this.nTot += weight;

            if (simTt < (1 - EPSILON) * realTt) {
                nFast += weight;
            } else if (simTt > (1 + EPSILON) * realTt) {
                nSlow += weight;
            }
        }

        public void merge(GroupStats source) {
            this.simulatedTime += source.simulatedTime;
            this.observedTime += source.observedTime;
            this.tripCount += source.tripCount;
            this.nTot += source.nTot;
            this.nFast += source.nFast;
            this.nSlow += source.nSlow;

            this.factors.addAll(source.factors);
            this.weights.addAll(source.weights);
            this.errors.addAll(source.errors);
        }

        public boolean shouldUpdate() {
            return Math.abs(nFast - nSlow) > C * nTot;
        }

        public double getAverageFactor() {
            return getTrimmedWeightedAverage(factors, weights);
        }

        public double getAverageErrors() {
            return getTrimmedWeightedAverage(errors, weights);
        }

        private double getTrimmedWeightedAverage(FloatArrayList x, FloatArrayList w) {
            int size = x.size();
            if (w.size() != size) {
                throw new IllegalStateException("Values and weights must have same size.");
            }

            // Create index array (primitive, no boxing)
            int[] order = new int[size];
            for (int i = 0; i < size; i++) {
                order[i] = i;
            }

            // Sort indices based on x values
            IntArrays.quickSort(order, (i, j) -> Float.compare(x.getFloat(i), x.getFloat(j)));

            int lowerBound = (int) (size * 0.2);
            int upperBound = (int) (size * 0.8);

            double weightedSum = 0.0;
            double totalWeight = 0.0;

            for (int k = lowerBound; k < upperBound; k++) {
                int idx = order[k];

                float wi = w.getFloat(idx);
                if (!Float.isFinite(wi) || wi <= 0.0f) continue;

                weightedSum += x.getFloat(idx) * wi;
                totalWeight += wi;
            }

            return totalWeight > 0.0 ? (weightedSum / totalWeight) : Double.NaN;
        }
    }
}


