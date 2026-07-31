package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.Map;

/**
 * Single location for all calibration scoring formulas.
 *
 * <p>All scores are deterministic and expressed in units of relative error.
 * A positive score means the action (remove / restore / clone) improves the
 * current fit to counts; a negative score means it makes it worse.</p>
 */
public final class CalibrationFormulas {
    private CalibrationFormulas() {
    }

    /**
     * Relative error between simulated flow and observed counts.
     * Positive = over-estimation, negative = under-estimation.
     */
    public static double signedRelativeError(double simulatedFlow, double observedCount) {
        if (observedCount <= 0.0) {
            return 0.0;
        }
        return (simulatedFlow - observedCount) / observedCount;
    }

    /**
     * Share of current flow that should be removed to bring an over-estimated link
     * back to the over-estimation threshold.
     */
    public static double removalShareNeeded(double signedRelativeError, double overThreshold) {
        if (signedRelativeError <= overThreshold) {
            return 0.0;
        }
        return (signedRelativeError - overThreshold) / (1.0 + signedRelativeError);
    }

    /**
     * Magnitude of under-estimation, zero if within the tolerated threshold.
     */
    public static double underMagnitude(double signedRelativeError, double underThreshold) {
        double magnitude = -signedRelativeError;
        if (magnitude <= underThreshold) {
            return 0.0;
        }
        return magnitude;
    }

    /**
     * Share of current flow that should be added to bring an under-estimated link
     * back to the under-estimation threshold.
     */
    public static double expansionShareNeeded(double underMagnitude, double expansionDamping) {
        if (underMagnitude <= 0.0) {
            return 0.0;
        }
        return clip(underMagnitude / Math.max(1.0 - underMagnitude, 1.0e-9) * expansionDamping, 0.0, 1.0);
    }

    /**
     * Score for removing an active subpopulation agent.
     *
     * <p>High score = the agent contributes to over-estimated links much more than
     * to under-estimated links. The tolerance lets us keep agents whose over- and
     * under-contributions are within {@code p.reductionTolerance()} of each other.</p>
     */
    public static double removalScore(Map<Id<Link>, Integer> traversals, LinkErrorTracker tracker, Parameters p) {
        double overContribution = 0.0;
        double underContribution = 0.0;

        for (Map.Entry<Id<Link>, Integer> entry : traversals.entrySet()) {
            Id<Link> linkId = entry.getKey();
            if (!tracker.isMonitored(linkId)) {
                continue;
            }
            int multiplicity = Math.max(1, entry.getValue());
            double error = tracker.signedRelativeError(linkId);

            if (tracker.isOver(linkId, p)) {
                overContribution += multiplicity * error;
            }
            if (tracker.isUnderForReduction(linkId, p)) {
                underContribution += multiplicity * (-error);
            }
        }

        return overContribution - (1.0 + p.reductionTolerance()) * underContribution;
    }

    /**
     * Score for restoring a previously removed agent.
     *
     * <p>High score = the agent's original plan mainly serves under-estimated links
     * and does not worsen over-estimated links too much.</p>
     */
    public static double restoreScore(Map<Id<Link>, Integer> traversals, LinkErrorTracker tracker, Parameters p) {
        return expansionScore(traversals, tracker, p, p.restoreOverPenaltyWeight(), false);
    }

    /**
     * Score for cloning a cross-border donor agent.
     *
     * <p>High score = the donor mainly crosses under-estimated counting stations
     * (with a relevant cross-border share) and does not worsen over-estimated links
     * too much.</p>
     */
    public static double cloneScore(Map<Id<Link>, Integer> traversals, LinkErrorTracker tracker, Parameters p) {
        return expansionScore(traversals, tracker, p, p.cloneOverPenaltyWeight(), true);
    }

    private static double expansionScore(Map<Id<Link>, Integer> traversals,
                                         LinkErrorTracker tracker,
                                         Parameters p,
                                         double overPenaltyWeight,
                                         boolean requireCrossBorderShare) {
        double underContribution = 0.0;
        double overContribution = 0.0;

        for (Map.Entry<Id<Link>, Integer> entry : traversals.entrySet()) {
            Id<Link> linkId = entry.getKey();
            if (!tracker.isMonitored(linkId)) {
                continue;
            }
            int multiplicity = Math.max(1, entry.getValue());
            double error = tracker.signedRelativeError(linkId);

            boolean isUnder = requireCrossBorderShare
                    ? tracker.isUnderForExpansion(linkId, p)
                    : tracker.isUnderForReduction(linkId, p);

            if (isUnder) {
                underContribution += multiplicity * (-error);
            }
            if (tracker.isOver(linkId, p)) {
                overContribution += multiplicity * error;
            }
        }

        return underContribution - overPenaltyWeight * overContribution;
    }

    /**
     * Estimates how many cross-border donor agents should be cloned to cover the
     * total expansion deficit on cross-border-dominant under-estimated links.
     */
    public static int computeCloneCount(LinkErrorTracker tracker, Parameters p) {
        double totalNeededShare = 0.0;
        for (Id<Link> linkId : tracker.monitoredLinks()) {
            if (tracker.isUnderForExpansion(linkId, p)) {
                double underMagnitude = underMagnitude(tracker.signedRelativeError(linkId), p.flowUnderEstimationThreshold());
                totalNeededShare += expansionShareNeeded(underMagnitude, p.expansionDamping());
            }
        }
        return (int) Math.ceil(totalNeededShare);
    }

    public static double clip(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Immutable calibration parameters. All relative thresholds are expressed as
     * fractions (e.g. 0.15 = 15 %).
     */
    public record Parameters(
            double flowOverEstimationThreshold,
            double flowUnderEstimationThreshold,
            double reductionTolerance,
            double subpopulationShareThreshold,
            double crossBorderShareThreshold,
            int minTraversalsPerLink,
            double restoreOverPenaltyWeight,
            double cloneOverPenaltyWeight,
            double expansionDamping
    ) {
    }
}
