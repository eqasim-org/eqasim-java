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
    private static final double MIN_EFFECTIVE_BETA = 0.4;
    private static final double MAX_EFFECTIVE_BETA = 0.8;
    private static final double MAX_FACTOR_STEP = 0.08;
    private static final double MIN_IMPROVEMENT_RATIO = 0.015;
    private static final int NO_IMPROVEMENT_PATIENCE = 3;
    private static final int NUM_FROZEN_ITERATIONS = 4;
    private static final int KEEP_FROZEN_FROM_ITERATION = 90;

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
    
    /**
     * Updates each group with the same decision order as before:
     * <ol>
     *     <li>skip if data is insufficient,</li>
     *     <li>skip if the group is currently frozen,</li>
     *     <li>skip if the fit is already good enough,</li>
     *     <li>freeze and optionally roll back when the error stops improving,</li>
     *     <li>otherwise apply the bounded factor update.</li>
     * </ol>
     */
    public void updateFactors(Map<LinkGroupKey, GroupStats> groupStats, int iteration) {
        if (!calibrate) {
            return;
        }

        NUM_UPDATES++;
        UpdateSummary summary = new UpdateSummary(iteration);

        for (Map.Entry<LinkGroupKey, GroupStats> entry : groupStats.entrySet()) {
            processGroupUpdate(entry.getKey(), entry.getValue(), summary);
        }

        logUpdateSummary(groupStats.size(), summary);
    }

    private void processGroupUpdate(LinkGroupKey key, GroupStats stats, UpdateSummary summary) {
        GroupUpdateContext context = createGroupUpdateContext(key, stats);

        if (hasInsufficientData(context)) {
            skipInsufficientData(context, summary);
            return;
        }

        if (context.diagnostics.isFrozen(summary.iteration)) {
            skipFrozenGroup(context, summary);
            return;
        }

        if (!context.stats.shouldUpdate()) {
            skipGoodFit(context, summary);
            return;
        }

        if (shouldSkipForNoImprovement(context)) {
            skipNoImprovement(context, summary);
            return;
        }

        applyFactorUpdate(context, summary);
    }

    private GroupUpdateContext createGroupUpdateContext(LinkGroupKey key, GroupStats stats) {
        double currentFactor = this.factors.getOrDefault(key, 1.0);
        GroupDiagnostics diagnostics = this.diagnostics.computeIfAbsent(key,
                ignored -> new GroupDiagnostics(Decision.FIRST, currentFactor, Double.NaN));
        double currentError = Math.abs(stats.getAverageErrors());

        return new GroupUpdateContext(key, stats, diagnostics, currentFactor, currentError);
    }

    private boolean hasInsufficientData(GroupUpdateContext context) {
        return context.stats.tripCount < minTripsPerGroup
                || context.stats.observedTime <= 0.0
                || context.stats.simulatedTime <= 0.0
                || !Double.isFinite(context.currentError);
    }

    private void skipInsufficientData(GroupUpdateContext context, UpdateSummary summary) {
        context.diagnostics.add(Decision.SKIPPED_INSUFFICIENT_TRIPS, context.currentFactor, Double.NaN);
        summary.skipped++;
    }

    private void skipFrozenGroup(GroupUpdateContext context, UpdateSummary summary) {
        context.diagnostics.frozen++;
        context.diagnostics.noImprovementStreak = 0;
        context.diagnostics.lastlyFrozen = true;
        context.diagnostics.add(Decision.SKIPPED_FROZEN, context.currentFactor, context.currentError);
        summary.skipped++;
        summary.frozen++;
    }

    private void skipGoodFit(GroupUpdateContext context, UpdateSummary summary) {
        context.diagnostics.noImprovementStreak = 0;
        context.diagnostics.add(Decision.SKIPPED_GOOD_FIT, context.currentFactor, context.currentError);
        summary.skipped++;
    }

    private boolean shouldSkipForNoImprovement(GroupUpdateContext context) {
        Double referenceError = getReferenceError(context.diagnostics);
        return referenceError != null
                && Double.isFinite(referenceError)
                && !isImproved(referenceError, context.currentError)
                && !context.diagnostics.lastlyFrozen;
        // if it was frozen in the last iteration and now unfrozen, there is no improvement in the error, but we must allow updates. Why ?
        // because of the interaction of other calibrations (i.e. penalties calibration), the evolution of penalties can change travel times, thus we need a new factor's update
    }

    private Double getReferenceError(GroupDiagnostics diagnostics) {
        Double previousError = diagnostics.lastErrors.getFromLast(diagnostics.noImprovementStreak);
        return previousError != null ? previousError : diagnostics.lastErrors.getFromLast(0);
    }

    private void skipNoImprovement(GroupUpdateContext context, UpdateSummary summary) {
        context.diagnostics.noImprovementStreak++;

        if (context.diagnostics.noImprovementStreak >= NO_IMPROVEMENT_PATIENCE) {
            context.diagnostics.frozen++;
            summary.frozen++;

            double rollbackFactor = getRollbackFactor(context.diagnostics, context.currentFactor);
            factors.put(context.key, rollbackFactor);
            context.diagnostics.add(Decision.SKIPPED_NO_MORE_IMPROVEMENT, rollbackFactor, context.currentError);
        } else {
            context.diagnostics.add(Decision.SKIPPED_NO_MORE_IMPROVEMENT, context.currentFactor, context.currentError);
        }

        summary.skipped++;
    }

    private double getRollbackFactor(GroupDiagnostics diagnostics, double currentFactor) {
        Double rollbackFactor = diagnostics.lastFactors.getFromLast(diagnostics.noImprovementStreak);
        if (rollbackFactor == null || !Double.isFinite(rollbackFactor)) {
            return currentFactor;
        }

        return clipFactor(rollbackFactor);
    }

    private void applyFactorUpdate(GroupUpdateContext context, UpdateSummary summary) {
        context.diagnostics.noImprovementStreak = 0;
        context.diagnostics.lastlyFrozen = false;

        double candidateFactor = computeCandidateFactor(context);
        if (!Double.isFinite(candidateFactor)) {
            context.diagnostics.add(Decision.SKIPPED_INSUFFICIENT_TRIPS, context.currentFactor, context.currentError);
            summary.skipped++;
            return;
        }

        double newFactor = computeUpdatedFactor(context.currentFactor, candidateFactor);
        factors.put(context.key, newFactor);
        context.diagnostics.add(Decision.UPDATED, newFactor, context.currentError);
        summary.updated++;

        if (!almostEqual(newFactor, context.currentFactor)) {
            summary.changed++;
        }
    }

    private double computeCandidateFactor(GroupUpdateContext context) {
        return clipFactor(context.stats.getAverageFactor() * context.currentFactor);
    }

    private double computeUpdatedFactor(double currentFactor, double candidateFactor) {
        double effectiveBeta = NUM_UPDATES>2 ? clipValue(beta * 2.0 / (NUM_UPDATES-2.0), MIN_EFFECTIVE_BETA, MAX_EFFECTIVE_BETA):1.0;
        double maximumStep = NUM_UPDATES>2 ? MAX_FACTOR_STEP: 1.0;
        double rawStep = (candidateFactor - currentFactor) * effectiveBeta;
        double boundedStep = clipValue(rawStep, -maximumStep, maximumStep);
        return clipFactor(currentFactor + boundedStep);
    }

    private void logUpdateSummary(int groupsWithStats, UpdateSummary summary) {
        logger.info("Freespeed update: groupsWithStats={}, totalGroupsWithFactors={}, updated={}, skipped={}, changed={}, frozen={}, iteration={}",
                groupsWithStats, factors.size(), summary.updated, summary.skipped, summary.changed, summary.frozen, summary.iteration);
    }

    private static boolean isImproved(double previousError, double currentError) {
        if (!Double.isFinite(previousError)) {
            return true;
        }

        double threshold = Math.abs(previousError) * (1.0 - MIN_IMPROVEMENT_RATIO);
        return Math.abs(currentError) < threshold;
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

    private static final class GroupUpdateContext {
        private final LinkGroupKey key;
        private final GroupStats stats;
        private final GroupDiagnostics diagnostics;
        private final double currentFactor;
        private final double currentError;

        private GroupUpdateContext(LinkGroupKey key, GroupStats stats, GroupDiagnostics diagnostics,
                                   double currentFactor, double currentError) {
            this.key = key;
            this.stats = stats;
            this.diagnostics = diagnostics;
            this.currentFactor = currentFactor;
            this.currentError = currentError;
        }
    }

    private static final class UpdateSummary {
        private int changed;
        private int updated;
        private int skipped;
        private int frozen;
        private final int iteration;
        public UpdateSummary(int iteration) {this.iteration = iteration;}
    }

    public static class GroupDiagnostics {
        public final Q<Decision> decisions = new Q<>(HISTORY_SIZE);
        public final Q<Double> lastFactors = new Q<>(HISTORY_SIZE);
        public final Q<Double> lastErrors = new Q<>(HISTORY_SIZE);
        public int noImprovementStreak;
        public int frozen;
        public boolean lastlyFrozen = false;
        public boolean keepFrozen = false;

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

        public boolean isFrozen(int iteration) {
            // after KEEP_FROZEN_FROM_ITERATION, if it is frozen once, it will stay frozen up to the end of the simulation
            boolean shouldBeFrozen = (frozen > 0) && (frozen%NUM_FROZEN_ITERATIONS)!=0;
            if (iteration>=KEEP_FROZEN_FROM_ITERATION && shouldBeFrozen) {
                keepFrozen = true;
            }
            return keepFrozen || shouldBeFrozen;
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

        private static final double EPSILON = 0.02;
        private static final double C = 0.07;

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


