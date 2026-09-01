package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring;

public enum TrafficCategory {
    CROSS_BORDER(1),
    FREIGHT(2);

    private final int mask;

    TrafficCategory(int mask) {
        this.mask = mask;
    }

    int mask() {
        return mask;
    }

    static boolean contains(int categories, TrafficCategory category) {
        return (categories & category.mask) != 0;
    }
}
