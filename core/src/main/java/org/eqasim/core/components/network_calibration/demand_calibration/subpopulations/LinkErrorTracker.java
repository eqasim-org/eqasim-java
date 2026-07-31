package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations;

import org.eqasim.core.components.network_calibration.Processors.CountsProcessor;
import org.eqasim.core.components.network_calibration.Processors.FlowProcessor;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tracks, link by link, how much the current simulation over- or under-estimates
 * the observed counts. The tracker is updated incrementally while agents are
 * removed or restored, so every decision is made against the up-to-date error
 * landscape.
 */
final class LinkErrorTracker {
    private static final double MIN_SAMPLE_SIZE = 1.0e-9;

    private final double sampleSize;
    private final Map<Id<Link>, LinkState> links = new HashMap<>();

    LinkErrorTracker(CountsProcessor counts,
                     FlowProcessor flow,
                     Map<Id<Link>, Integer> allTraversals,
                     Map<Id<Link>, Integer> subpopulationTraversals,
                     Map<Id<Link>, Integer> crossBorderTraversals,
                     double sampleSize,
                     CalibrationFormulas.Parameters parameters) {
        this.sampleSize = Math.max(sampleSize, MIN_SAMPLE_SIZE);

        for (Map.Entry<Id<Link>, Integer> entry : allTraversals.entrySet()) {
            Id<Link> linkId = entry.getKey();
            int totalTraversals = entry.getValue();

            if (totalTraversals < parameters.minTraversalsPerLink()) {
                continue;
            }

            float observedCount = counts.getLinkCounts(linkId);
            if (observedCount <= 0.0f) {
                continue;
            }

            double simulatedFlow = flow.getTotalLinkFlow(linkId) / this.sampleSize;
            int subTraversals = subpopulationTraversals.getOrDefault(linkId, 0);
            int cbTraversals = crossBorderTraversals.getOrDefault(linkId, 0);

            links.put(linkId, new LinkState(simulatedFlow, observedCount, totalTraversals, subTraversals, cbTraversals));
        }
    }

    boolean isMonitored(Id<Link> linkId) {
        return links.containsKey(linkId);
    }

    Set<Id<Link>> monitoredLinks() {
        return Collections.unmodifiableSet(links.keySet());
    }

    /**
     * Relative error on the link using the <em>current</em> simulated flow.
     */
    double signedRelativeError(Id<Link> linkId) {
        LinkState state = links.get(linkId);
        if (state == null) {
            return 0.0;
        }
        return CalibrationFormulas.signedRelativeError(state.currentFlow, state.observedCount);
    }

    boolean isOver(Id<Link> linkId, CalibrationFormulas.Parameters p) {
        return signedRelativeError(linkId) > p.flowOverEstimationThreshold()
                && subpopulationShare(linkId) > p.subpopulationShareThreshold();
    }

    boolean isUnderForReduction(Id<Link> linkId, CalibrationFormulas.Parameters p) {
        return -signedRelativeError(linkId) > p.flowUnderEstimationThreshold();
    }

    boolean isUnderForExpansion(Id<Link> linkId, CalibrationFormulas.Parameters p) {
        return -signedRelativeError(linkId) > p.flowUnderEstimationThreshold()
                && crossBorderShare(linkId) > p.crossBorderShareThreshold();
    }

    double subpopulationShare(Id<Link> linkId) {
        LinkState state = links.get(linkId);
        if (state == null || state.totalTraversals == 0) {
            return 0.0;
        }
        return state.subpopulationTraversals / (double) state.totalTraversals;
    }

    double crossBorderShare(Id<Link> linkId) {
        LinkState state = links.get(linkId);
        if (state == null || state.totalTraversals == 0) {
            return 0.0;
        }
        return state.crossBorderTraversals / (double) state.totalTraversals;
    }

    /**
     * Decrease the simulated flow by the traversals of an agent that is being removed.
     */
    void removeAgent(Map<Id<Link>, Integer> traversals) {
        for (Map.Entry<Id<Link>, Integer> entry : traversals.entrySet()) {
            LinkState state = links.get(entry.getKey());
            if (state != null) {
                state.currentFlow -= entry.getValue() / sampleSize;
            }
        }
    }

    /**
     * Increase the simulated flow by the traversals of an agent that is being restored.
     */
    void addAgent(Map<Id<Link>, Integer> traversals) {
        for (Map.Entry<Id<Link>, Integer> entry : traversals.entrySet()) {
            LinkState state = links.get(entry.getKey());
            if (state != null) {
                state.currentFlow += entry.getValue() / sampleSize;
            }
        }
    }

    private static final class LinkState {
        double currentFlow;
        final double observedCount;
        final int totalTraversals;
        final int subpopulationTraversals;
        final int crossBorderTraversals;

        LinkState(double currentFlow,
                  double observedCount,
                  int totalTraversals,
                  int subpopulationTraversals,
                  int crossBorderTraversals) {
            this.currentFlow = currentFlow;
            this.observedCount = observedCount;
            this.totalTraversals = totalTraversals;
            this.subpopulationTraversals = subpopulationTraversals;
            this.crossBorderTraversals = crossBorderTraversals;
        }
    }
}
