package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.crossborder;

import org.matsim.api.core.v01.population.Activity;

import java.util.Locale;
import java.util.Set;

/** Defines cross-border activities whose locations are structural anchors. */
public final class CrossBorderActivityRules {
    private static final Set<String> ANCHOR_TYPES = Set.of("outside", "border");

    private CrossBorderActivityRules() { }

    public static boolean isFixedLocation(Activity activity) {
        return isAnchor(activity) || isOneSecondConnector(activity);
    }

    public static boolean isAnchor(Activity activity) {
        String type = activity.getType();
        return type != null && ANCHOR_TYPES.contains(type.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean isOneSecondConnector(Activity activity) {
        if (activity.getMaximumDuration().isDefined()
                && isOneSecond(activity.getMaximumDuration().seconds())) {
            return true;
        }
        return activity.getStartTime().isDefined()
                && activity.getEndTime().isDefined()
                && isOneSecond(activity.getEndTime().seconds()
                - activity.getStartTime().seconds());
    }

    private static boolean isOneSecond(double durationSeconds) {
        return Math.abs(durationSeconds - 1.0) <= 1.0e-6;
    }
}
