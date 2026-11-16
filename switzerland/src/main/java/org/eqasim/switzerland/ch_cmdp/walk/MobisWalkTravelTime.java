package org.eqasim.switzerland.ch_cmdp.walk;

/** Endpoint walking model from calibration/walking_speed/model.py (791,900 filtered MOBIS legs).
 * Horizontal distance uses actual endpoints; elevation change uses their nearest network nodes.
 * All distances are metres, speeds m/s, times seconds, and grades rise/run ratios.
 */
public final class MobisWalkTravelTime implements WalkTravelTime {
    public static final double DEFAULT_V0_M_S = 1.430;
    public static final double DEFAULT_K = 3.159;
    public static final double DEFAULT_UPHILL_PENALTY = 0.870;
    public static final double DEFAULT_FLAT_GRADE = 0.002;
    public static final double DEFAULT_DETOUR_FACTOR = 1.311;

    private final double v0;
    private final double k;
    private final double uphillPenalty;
    private final double flatGrade;
    private final double detourFactor;

    public MobisWalkTravelTime() {
        this(DEFAULT_V0_M_S, DEFAULT_K, DEFAULT_UPHILL_PENALTY, DEFAULT_FLAT_GRADE, DEFAULT_DETOUR_FACTOR);
    }

    public MobisWalkTravelTime(double v0, double k, double uphillPenalty, double flatGrade, double detourFactor) {
        if (!Double.isFinite(v0) || v0 <= 0 || !Double.isFinite(k) || k < 0
                || !Double.isFinite(uphillPenalty) || uphillPenalty < 0
                || !Double.isFinite(flatGrade) || flatGrade < 0
                || !Double.isFinite(detourFactor) || detourFactor < 1)
            throw new IllegalArgumentException("Invalid MOBIS walking model parameters");
        this.v0 = v0;
        this.k = k;
        this.uphillPenalty = uphillPenalty;
        this.flatGrade = flatGrade;
        this.detourFactor = detourFactor;
    }

    @Override public double calculate(WalkTravelTimeContext context) {
        return travelTime(context.euclideanDistance(), context.nodesElevationChange());
    }

    public double travelTime(double euclideanDistance, double nodesElevationChange) {
        double length = detourFactor * euclideanDistance;
        if (length == 0) {
            if (nodesElevationChange != 0)
                throw new IllegalArgumentException("Endpoint-grade model cannot represent purely vertical walking");
            return 0;
        }
        // Match the fitted Python equation: grade uses the detour-adjusted actual endpoint distance.
        double grade = nodesElevationChange / length;
        return length / speed(grade);
    }

    public double speed(double grade) {
        double v =  v0 * Math.exp(-k * Math.max(Math.abs(grade) - flatGrade, 0)
                                  - uphillPenalty * Math.max(grade - flatGrade, 0));
        return Math.max(Math.min(v, 3.0), 0.3);
    }
}
