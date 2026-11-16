package org.eqasim.switzerland.ch_cmdp.walk;

import java.util.Map;
import org.matsim.core.config.ReflectiveConfigGroup;

/** Persist the selected walking expression so an output config can reproduce the choice. */
public final class WalkConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "swissWalking";
    private String model = "mobis";
    private double v0MetersPerSecond = MobisWalkTravelTime.DEFAULT_V0_M_S;
    private double k = MobisWalkTravelTime.DEFAULT_K;
    private double uphillPenalty = MobisWalkTravelTime.DEFAULT_UPHILL_PENALTY;
    private double flatGrade = MobisWalkTravelTime.DEFAULT_FLAT_GRADE;
    private double detourFactor = MobisWalkTravelTime.DEFAULT_DETOUR_FACTOR;

    public WalkConfigGroup() { super(GROUP_NAME); }

    @StringGetter("model") public String getModel() { return model; }
    @StringSetter("model") public void setModel(String model) {
        if (!"teleportation".equals(model) && !"mobis".equals(model))
            throw new IllegalArgumentException("Walking model must be teleportation or mobis");
        this.model = model;
    }

    @StringGetter("v0_m_s") public double getV0MetersPerSecond() { return v0MetersPerSecond; }
    @StringSetter("v0_m_s") public void setV0MetersPerSecond(double value) {
        if (!Double.isFinite(value) || value <= 0) throw new IllegalArgumentException("v0_m_s must be finite and positive");
        v0MetersPerSecond = value;
    }
    @StringGetter("k") public double getK() { return k; }
    @StringSetter("k") public void setK(double value) { k = nonnegative("k", value); }
    @StringGetter("uphill_penalty") public double getUphillPenalty() { return uphillPenalty; }
    @StringSetter("uphill_penalty") public void setUphillPenalty(double value) { uphillPenalty = nonnegative("uphill_penalty", value); }
    @StringGetter("flat_grade") public double getFlatGrade() { return flatGrade; }
    @StringSetter("flat_grade") public void setFlatGrade(double value) { flatGrade = nonnegative("flat_grade", value); }
    @StringGetter("detour_factor") public double getDetourFactor() { return detourFactor; }
    @StringSetter("detour_factor") public void setDetourFactor(double value) {
        if (!Double.isFinite(value) || value < 1) throw new IllegalArgumentException("detour_factor must be finite and at least 1");
        detourFactor = value;
    }

    private static double nonnegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException(name + " must be finite and nonnegative");
        return value;
    }

    @Override public Map<String, String> getComments() {
        var comments = super.getComments();
        comments.put("model", "Walking-time expression for walk trips and network-mode access/egress. PT keeps its internal walking calculations. "
                + "teleportation: existing MATSim calculation; mobis (default): fitted endpoint walking model.");
        comments.put("v0_m_s", "MOBIS flat-zone walking speed in metres per second.");
        comments.put("k", "MOBIS speed decay beyond the flat grade, in both directions.");
        comments.put("uphill_penalty", "Additional MOBIS speed decay for uphill grades.");
        comments.put("flat_grade", "MOBIS flat-zone grade threshold as rise/run (0.002 = 0.2 percent).");
        comments.put("detour_factor", "MOBIS multiplier of actual endpoint XY distance; also used in the grade denominator.");
        return comments;
    }
}
