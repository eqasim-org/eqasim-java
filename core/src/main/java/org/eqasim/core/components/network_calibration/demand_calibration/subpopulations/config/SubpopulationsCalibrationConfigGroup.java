package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.config;

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SubpopulationsCalibrationConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "subpopulationsCalibration";

    private static final String ACTIVATE = "activate";
    private static final String WARMUP_ITERATIONS = "warmupIterations";
    private static final String UPDATE_INTERVAL = "updateInterval";
    private static final String EARLY_UPDATE_INTERVAL = "earlyUpdateInterval";
    private static final String EARLY_ITERATION_LIMIT = "earlyIterationLimit";
    private static final String CALIBRATE_CROSS_BORDER = "calibrateCrossBorder";
    private static final String CROSS_BORDER_CLONING_ITERATIONS = "crossBorderCloningIterations";
    private static final String FLOW_OVER_ESTIMATION_THRESHOLD = "flowOverEstimationThreshold";
    private static final String FLOW_UNDER_ESTIMATION_THRESHOLD = "flowUnderEstimationThreshold";
    private static final String CROSS_BORDER_SHARE_THRESHOLD = "crossBorderShareThreshold";
    private static final String CROSS_BORDER_UPDATE_FRACTION = "crossBorderUpdateFraction";
    private static final String RELOCATION_RADIUS = "relocationRadius";
    private static final String HOME_RELOCATION_RADIUS = "homeRelocationRadius";
    private static final String MAXIMUM_TIME_SHIFT = "maximumTimeShift";
    private static final String FREIGHT_RELOCATION_RADIUS_FACTOR = "freightRelocationRadiusFactor";
    private static final String FREIGHT_MINIMUM_RADIUS = "freightMinimumRadius";
    private static final String FREIGHT_MAXIMUM_RADIUS = "freightMaximumRadius";
    private static final String FREIGHT_RELOCATION_TRY_FRACTION = "freightRelocationTryFraction";
    private static final String DESTINATION_SELECTION_PROBABILITY = "destinationSelectionProbability";

    private boolean activate=false;
    private int warmupIterations = 16;
    private int updateInterval = 5;
    private int earlyUpdateInterval = 8;
    private int earlyIterationLimit = 50;
    private boolean calibrateCrossBorder = false;
    private String crossBorderCloningIterations = "2,15,32,56,79";
    private double flowOverEstimationThreshold = 0.05;
    private double flowUnderEstimationThreshold = 0.05;
    private double crossBorderShareThreshold = 0.20;
    private double crossBorderUpdateFraction = 0.5;
    private double relocationRadius = 2_000.0;
    private double homeRelocationRadius = 500.0;
    private int maximumTimeShift = 600;
    private double freightRelocationRadiusFactor = 0.20;
    private double freightMinimumRadius = 200.0;
    private double freightMaximumRadius = 3_000.0;
    private double freightRelocationTryFraction = 0.5;
    private double destinationSelectionProbability = 0.75;

    public SubpopulationsCalibrationConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(ACTIVATE, "Whether subpopulation demand calibration is active (default: false)");
        comments.put(WARMUP_ITERATIONS, "Iterations before demand-reduction updates begin (default: 16)");
        comments.put(UPDATE_INTERVAL, "Iterations between updates after the early phase (default: 5)");
        comments.put(EARLY_UPDATE_INTERVAL, "Iterations between updates during the early phase (default: 8)");
        comments.put(EARLY_ITERATION_LIMIT, "Exclusive end of the early-interval phase (default: 50)");
        comments.put(CALIBRATE_CROSS_BORDER, "Whether cross-border location and volume are calibrated; when false, only freight is calibrated (default: false)");
        comments.put(CROSS_BORDER_CLONING_ITERATIONS, "Comma-separated iterations at which cross-border agents are cloned");
        comments.put(FLOW_OVER_ESTIMATION_THRESHOLD, "Relative flow excess that marks a link as overestimated (default: 0.05)");
        comments.put(FLOW_UNDER_ESTIMATION_THRESHOLD, "Relative flow deficit that marks a link as underestimated (default: 0.05)");
        comments.put(CROSS_BORDER_SHARE_THRESHOLD, "Minimum cross-border passage share for both counted links of a detected border station (default: 0.20)");
        comments.put(CROSS_BORDER_UPDATE_FRACTION, "Fraction of the required cross-border correction applied at each update (default: 0.50)");
        comments.put(RELOCATION_RADIUS, "Maximum relocation radius for cloned cross-border activities in meters (default: 2000)");
        comments.put(HOME_RELOCATION_RADIUS, "Maximum relocation radius for cloned home activities in meters (default: 500)");
        comments.put(MAXIMUM_TIME_SHIFT, "Maximum absolute random time shift for cloned plans in seconds (default: 600)");
        comments.put(FREIGHT_RELOCATION_RADIUS_FACTOR, "Background-traffic relocation radius factor; legacy parameter name retained for configuration compatibility (default: 0.20)");
        comments.put(FREIGHT_MINIMUM_RADIUS, "Minimum background-traffic relocation radius in meters; legacy parameter name retained (default: 200)");
        comments.put(FREIGHT_MAXIMUM_RADIUS, "Maximum background-traffic relocation radius in meters; legacy parameter name retained (default: 3000)");
        comments.put(FREIGHT_RELOCATION_TRY_FRACTION, "Fraction of eligible freight and cross-border plans tried for location relocation; legacy parameter name retained (default: 0.50)");
        comments.put(DESTINATION_SELECTION_PROBABILITY, "For one-trip freight only, probability of relocating the destination instead of the origin (default: 0.75)");
        return comments;
    }

    @StringGetter(ACTIVATE) public boolean isActivated() { return activate; }
    @StringSetter(ACTIVATE) public void setActivate(boolean value) { activate = value; }
    @StringGetter(WARMUP_ITERATIONS) public int getWarmupIterations() { return warmupIterations; }
    @StringSetter(WARMUP_ITERATIONS) public void setWarmupIterations(int value) { warmupIterations = value; }
    @StringGetter(UPDATE_INTERVAL) public int getUpdateInterval() { return updateInterval; }
    @StringSetter(UPDATE_INTERVAL) public void setUpdateInterval(int value) { updateInterval = value; }
    @StringGetter(EARLY_UPDATE_INTERVAL) public int getEarlyUpdateInterval() { return earlyUpdateInterval; }
    @StringSetter(EARLY_UPDATE_INTERVAL) public void setEarlyUpdateInterval(int value) { earlyUpdateInterval = value; }
    @StringGetter(EARLY_ITERATION_LIMIT) public int getEarlyIterationLimit() { return earlyIterationLimit; }
    @StringSetter(EARLY_ITERATION_LIMIT) public void setEarlyIterationLimit(int value) { earlyIterationLimit = value; }
    @StringGetter(CALIBRATE_CROSS_BORDER) public boolean isCrossBorderCalibrationEnabled() { return calibrateCrossBorder; }
    @StringSetter(CALIBRATE_CROSS_BORDER) public void setCalibrateCrossBorder(boolean value) { calibrateCrossBorder = value; }
    @StringGetter(CROSS_BORDER_CLONING_ITERATIONS) public String getCrossBorderCloningIterationsAsString() { return crossBorderCloningIterations; }
    @StringSetter(CROSS_BORDER_CLONING_ITERATIONS) public void setCrossBorderCloningIterations(String value) { crossBorderCloningIterations = value; }
    public List<Integer> getCrossBorderCloningIterations() { return Arrays.stream(crossBorderCloningIterations.split(",")).map(String::trim).filter(value -> !value.isEmpty()).map(Integer::parseInt).toList(); }
    @StringGetter(FLOW_OVER_ESTIMATION_THRESHOLD) public double getFlowOverEstimationThreshold() { return flowOverEstimationThreshold; }
    @StringSetter(FLOW_OVER_ESTIMATION_THRESHOLD) public void setFlowOverEstimationThreshold(double value) { flowOverEstimationThreshold = value; }
    @StringGetter(FLOW_UNDER_ESTIMATION_THRESHOLD) public double getFlowUnderEstimationThreshold() { return flowUnderEstimationThreshold; }
    @StringSetter(FLOW_UNDER_ESTIMATION_THRESHOLD) public void setFlowUnderEstimationThreshold(double value) { flowUnderEstimationThreshold = value; }
    @StringGetter(CROSS_BORDER_SHARE_THRESHOLD) public double getCrossBorderShareThreshold() { return crossBorderShareThreshold; }
    @StringSetter(CROSS_BORDER_SHARE_THRESHOLD) public void setCrossBorderShareThreshold(double value) { crossBorderShareThreshold = value; }
    @StringGetter(CROSS_BORDER_UPDATE_FRACTION) public double getCrossBorderUpdateFraction() { return crossBorderUpdateFraction; }
    @StringSetter(CROSS_BORDER_UPDATE_FRACTION) public void setCrossBorderUpdateFraction(double value) { crossBorderUpdateFraction = value; }
    @StringGetter(RELOCATION_RADIUS) public double getRelocationRadius() { return relocationRadius; }
    @StringSetter(RELOCATION_RADIUS) public void setRelocationRadius(double value) { relocationRadius = value; }
    @StringGetter(HOME_RELOCATION_RADIUS) public double getHomeRelocationRadius() { return homeRelocationRadius; }
    @StringSetter(HOME_RELOCATION_RADIUS) public void setHomeRelocationRadius(double value) { homeRelocationRadius = value; }
    @StringGetter(MAXIMUM_TIME_SHIFT) public int getMaximumTimeShift() { return maximumTimeShift; }
    @StringSetter(MAXIMUM_TIME_SHIFT) public void setMaximumTimeShift(int value) { maximumTimeShift = value; }
    @StringGetter(FREIGHT_RELOCATION_RADIUS_FACTOR) public double getFreightRelocationRadiusFactor() { return freightRelocationRadiusFactor; }
    @StringSetter(FREIGHT_RELOCATION_RADIUS_FACTOR) public void setFreightRelocationRadiusFactor(double value) { freightRelocationRadiusFactor = value; }
    @StringGetter(FREIGHT_MINIMUM_RADIUS) public double getFreightMinimumRadius() { return freightMinimumRadius; }
    @StringSetter(FREIGHT_MINIMUM_RADIUS) public void setFreightMinimumRadius(double value) { freightMinimumRadius = value; }
    @StringGetter(FREIGHT_MAXIMUM_RADIUS) public double getFreightMaximumRadius() { return freightMaximumRadius; }
    @StringSetter(FREIGHT_MAXIMUM_RADIUS) public void setFreightMaximumRadius(double value) { freightMaximumRadius = value; }
    @StringGetter(FREIGHT_RELOCATION_TRY_FRACTION) public double getFreightRelocationTryFraction() { return freightRelocationTryFraction; }
    @StringSetter(FREIGHT_RELOCATION_TRY_FRACTION) public void setFreightRelocationTryFraction(double value) { freightRelocationTryFraction = value; }
    @StringGetter(DESTINATION_SELECTION_PROBABILITY) public double getDestinationSelectionProbability() { return destinationSelectionProbability; }
    @StringSetter(DESTINATION_SELECTION_PROBABILITY) public void setDestinationSelectionProbability(double value) { destinationSelectionProbability = value; }

    // Neutral names used by the implementation. The serialized freight* keys
    // remain supported so existing scenario configurations do not break.
    public double getBackgroundRelocationRadiusFactor() { return freightRelocationRadiusFactor; }
    public void setBackgroundRelocationRadiusFactor(double value) { freightRelocationRadiusFactor = value; }
    public double getBackgroundMinimumRadius() { return freightMinimumRadius; }
    public void setBackgroundMinimumRadius(double value) { freightMinimumRadius = value; }
    public double getBackgroundMaximumRadius() { return freightMaximumRadius; }
    public void setBackgroundMaximumRadius(double value) { freightMaximumRadius = value; }
    public double getBackgroundRelocationTryFraction() { return freightRelocationTryFraction; }
    public void setBackgroundRelocationTryFraction(double value) { freightRelocationTryFraction = value; }
}
