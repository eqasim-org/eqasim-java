package org.eqasim.core.components.network_calibration.cost_calibration;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CostCalibrationConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "costCalibration";

    private static final String ACTIVATE = "activate";
    private static final String UPDATE_INTERVAL = "updateInterval";
    private static final String WARMUP_ITERATIONS = "warmupIterations";
    private static final String END_ITERATION = "endIteration";
    private static final String MIN_PENALTY = "minPenalty";
    private static final String MAX_PENALTY = "maxPenalty";
    private static final String PENALTIES_FILE = "penaltiesFile";
    private static final String SPECIAL_REGION_PATH = "specialRegionPath";
    private static final String RAMP_FACTOR = "rampFactor";
    private static final String TRUNK_FACTOR = "trunkFactor";
    private static final String INITIAL_LEARNING_RATE = "initialLearningRate";
    private static final String MINIMUM_LEARNING_RATE = "minimumLearningRate";
    private static final String LEARNING_RATE_DECAY_SCALE = "learningRateDecayScale";
    private static final String LEARNING_RATE_DECAY_EXPONENT = "learningRateDecayExponent";
    private static final String MAXIMUM_PENALTY_UPDATE = "maximumPenaltyUpdate";
    private static final String ROBUST_ERROR_THRESHOLD = "robustErrorThreshold";
    private static final String ROBUST_ERROR_EPSILON = "robustErrorEpsilon";
    private static final String SAMPLE_SIZE_SHRINKAGE = "sampleSizeShrinkage";
    private static final String SIGN_REVERSAL_FACTOR = "signReversalFactor";
    private static final String SIGN_REVERSAL_THRESHOLD = "signReversalThreshold";
    private static final String MINIMUM_GAIN_MULTIPLIER = "minimumGainMultiplier";
    private static final String GAIN_RECOVERY_RATE = "gainRecoveryRate";
    private static final String MIN_OBSERVATIONS_URBAN_RURAL = "minObservationsUrbanRural";
    private static final String MIN_OBSERVATIONS_SPECIAL_REGION = "minObservationsSpecialRegion";

    private boolean activate;
    private int updateInterval = 5;
    private int warmupIterations = 10;
    private int endIteration = 100;
    private double minPenalty = -0.1;
    private double maxPenalty = 0.3;
    private String penaltiesFile = "";
    private String specialRegionPath = "";
    private double rampFactor = 1.1;
    private double trunkFactor = 1.25;
    private double initialLearningRate = 0.2;
    private double minimumLearningRate = 0.08;
    private double learningRateDecayScale = 6.0;
    private double learningRateDecayExponent = 0.8;
    private double maximumPenaltyUpdate = 0.075;
    private double robustErrorThreshold = 0.25;
    private double robustErrorEpsilon = 2.0;
    private double sampleSizeShrinkage = 7.0;
    private double signReversalFactor = 0.7;
    private double signReversalThreshold = 0.003;
    private double minimumGainMultiplier = 0.1;
    private double gainRecoveryRate = 0.25;
    private int minObservationsUrbanRural = 7;
    private int minObservationsSpecialRegion = 4;

    public CostCalibrationConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(ACTIVATE, "Whether link-cost calibration is active (default: false)");
        comments.put(UPDATE_INTERVAL, "Iterations between penalty updates (default: 5)");
        comments.put(WARMUP_ITERATIONS, "Iterations before the first penalty update (default: 10)");
        comments.put(END_ITERATION, "Iterations before the first penalty update (default: 100)");
        comments.put(MIN_PENALTY, "Minimum link-category penalty factor (default: -0.1)");
        comments.put(MAX_PENALTY, "Maximum link-category penalty factor (default: 0.3)");
        comments.put(PENALTIES_FILE, "Optional penalty CSV with columns linkCategory;isUrban;specialRegion;penalty");
        comments.put(SPECIAL_REGION_PATH, "Semicolon-separated GeoJSON files defining cost-calibration regions");
        comments.put(RAMP_FACTOR, "Multiplier applied to penalties on ramp links (default: 1.0)");
        comments.put(TRUNK_FACTOR, "Multiplier applied to penalties on trunk links (default: 1.0)");
        comments.put(INITIAL_LEARNING_RATE, "Initial penalty-controller learning rate (default: 0.25)");
        comments.put(MINIMUM_LEARNING_RATE, "Asymptotic minimum learning rate (default: 0.07)");
        comments.put(LEARNING_RATE_DECAY_SCALE, "Scale of the learning-rate decay schedule (default: 6.0)");
        comments.put(LEARNING_RATE_DECAY_EXPONENT, "Exponent of the learning-rate decay schedule (default: 0.8)");
        comments.put(MAXIMUM_PENALTY_UPDATE, "Maximum absolute penalty change per update (default: 0.10)");
        comments.put(ROBUST_ERROR_THRESHOLD, "Transition threshold of the robust flow-error function (default: 0.20)");
        comments.put(ROBUST_ERROR_EPSILON, "Flow denominator stabilizer in veh/h/lane (default: 2.0)");
        comments.put(SAMPLE_SIZE_SHRINKAGE, "Robust-error shrinkage strength (default: 8.0)");
        comments.put(SIGN_REVERSAL_FACTOR, "Gain multiplier applied after an error sign reversal (default: 0.60)");
        comments.put(SIGN_REVERSAL_THRESHOLD, "Minimum error magnitude counted as a sign reversal (default: 0.03)");
        comments.put(MINIMUM_GAIN_MULTIPLIER, "Lower bound for group-specific controller gain (default: 0.15)");
        comments.put(GAIN_RECOVERY_RATE, "Fraction of lost controller gain recovered per update (default: 0.10)");
        comments.put(MIN_OBSERVATIONS_URBAN_RURAL, "Minimum observations before keeping urban and rural groups separate (default: 10)");
        comments.put(MIN_OBSERVATIONS_SPECIAL_REGION, "Minimum observations before keeping a special-region group separate (default: 4)");
        return comments;
    }

    @StringGetter(ACTIVATE) public boolean isActivated() { return activate; }
    @StringSetter(ACTIVATE) public void setActivate(boolean activate) { this.activate = activate; }
    @StringGetter(UPDATE_INTERVAL) public int getUpdateInterval() { return updateInterval; }
    @StringSetter(UPDATE_INTERVAL) public void setUpdateInterval(int value) { updateInterval = value; }
    @StringGetter(WARMUP_ITERATIONS) public int getWarmupIterations() { return warmupIterations; }
    @StringSetter(WARMUP_ITERATIONS) public void setWarmupIterations(int value) { warmupIterations = value; }
    @StringGetter(END_ITERATION) public int getEndIteration() { return endIteration; }
    @StringSetter(END_ITERATION) public void setEndIteration(int value) { endIteration = value; }
    @StringGetter(MIN_PENALTY) public double getMinPenalty() { return minPenalty; }
    @StringSetter(MIN_PENALTY) public void setMinPenalty(double value) { minPenalty = value; }
    @StringGetter(MAX_PENALTY) public double getMaxPenalty() { return maxPenalty; }
    @StringSetter(MAX_PENALTY) public void setMaxPenalty(double value) { maxPenalty = value; }
    @StringGetter(PENALTIES_FILE) public String getPenaltiesFile() { return penaltiesFile; }
    @StringSetter(PENALTIES_FILE) public void setPenaltiesFile(String value) { penaltiesFile = value; }
    public boolean hasPenaltiesFile() { return !penaltiesFile.isBlank() && penaltiesFile.endsWith(".csv"); }
    @StringGetter(SPECIAL_REGION_PATH) public String getSpecialRegionPath() { return specialRegionPath; }
    @StringSetter(SPECIAL_REGION_PATH) public void setSpecialRegionPath(String value) { specialRegionPath = value; }
    public List<String> getSpecialRegionFiles() { return splitPaths(specialRegionPath); }
    public boolean hasSpecialRegions() { return !getSpecialRegionFiles().isEmpty() && getSpecialRegionFiles().stream().allMatch(path -> path.endsWith("json") && new File(path).exists()); }
    @StringGetter(RAMP_FACTOR) public double getRampFactor() { return rampFactor; }
    @StringSetter(RAMP_FACTOR) public void setRampFactor(double value) { rampFactor = value; }
    @StringGetter(TRUNK_FACTOR) public double getTrunkFactor() { return trunkFactor; }
    @StringSetter(TRUNK_FACTOR) public void setTrunkFactor(double value) { trunkFactor = value; }
    @StringGetter(INITIAL_LEARNING_RATE) public double getInitialLearningRate() { return initialLearningRate; }
    @StringSetter(INITIAL_LEARNING_RATE) public void setInitialLearningRate(double value) { initialLearningRate = value; }
    @StringGetter(MINIMUM_LEARNING_RATE) public double getMinimumLearningRate() { return minimumLearningRate; }
    @StringSetter(MINIMUM_LEARNING_RATE) public void setMinimumLearningRate(double value) { minimumLearningRate = value; }
    @StringGetter(LEARNING_RATE_DECAY_SCALE) public double getLearningRateDecayScale() { return learningRateDecayScale; }
    @StringSetter(LEARNING_RATE_DECAY_SCALE) public void setLearningRateDecayScale(double value) { learningRateDecayScale = value; }
    @StringGetter(LEARNING_RATE_DECAY_EXPONENT) public double getLearningRateDecayExponent() { return learningRateDecayExponent; }
    @StringSetter(LEARNING_RATE_DECAY_EXPONENT) public void setLearningRateDecayExponent(double value) { learningRateDecayExponent = value; }
    @StringGetter(MAXIMUM_PENALTY_UPDATE) public double getMaximumPenaltyUpdate() { return maximumPenaltyUpdate; }
    @StringSetter(MAXIMUM_PENALTY_UPDATE) public void setMaximumPenaltyUpdate(double value) { maximumPenaltyUpdate = value; }
    @StringGetter(ROBUST_ERROR_THRESHOLD) public double getRobustErrorThreshold() { return robustErrorThreshold; }
    @StringSetter(ROBUST_ERROR_THRESHOLD) public void setRobustErrorThreshold(double value) { robustErrorThreshold = value; }
    @StringGetter(ROBUST_ERROR_EPSILON) public double getRobustErrorEpsilon() { return robustErrorEpsilon; }
    @StringSetter(ROBUST_ERROR_EPSILON) public void setRobustErrorEpsilon(double value) { robustErrorEpsilon = value; }
    @StringGetter(SAMPLE_SIZE_SHRINKAGE) public double getSampleSizeShrinkage() { return sampleSizeShrinkage; }
    @StringSetter(SAMPLE_SIZE_SHRINKAGE) public void setSampleSizeShrinkage(double value) { sampleSizeShrinkage = value; }
    @StringGetter(SIGN_REVERSAL_FACTOR) public double getSignReversalFactor() { return signReversalFactor; }
    @StringSetter(SIGN_REVERSAL_FACTOR) public void setSignReversalFactor(double value) { signReversalFactor = value; }
    @StringGetter(SIGN_REVERSAL_THRESHOLD) public double getSignReversalThreshold() { return signReversalThreshold; }
    @StringSetter(SIGN_REVERSAL_THRESHOLD) public void setSignReversalThreshold(double value) { signReversalThreshold = value; }
    @StringGetter(MINIMUM_GAIN_MULTIPLIER) public double getMinimumGainMultiplier() { return minimumGainMultiplier; }
    @StringSetter(MINIMUM_GAIN_MULTIPLIER) public void setMinimumGainMultiplier(double value) { minimumGainMultiplier = value; }
    @StringGetter(GAIN_RECOVERY_RATE) public double getGainRecoveryRate() { return gainRecoveryRate; }
    @StringSetter(GAIN_RECOVERY_RATE) public void setGainRecoveryRate(double value) { gainRecoveryRate = value; }
    @StringGetter(MIN_OBSERVATIONS_URBAN_RURAL) public int getMinObservationsUrbanRural() { return minObservationsUrbanRural; }
    @StringSetter(MIN_OBSERVATIONS_URBAN_RURAL) public void setMinObservationsUrbanRural(int value) { minObservationsUrbanRural = value; }
    @StringGetter(MIN_OBSERVATIONS_SPECIAL_REGION) public int getMinObservationsSpecialRegion() { return minObservationsSpecialRegion; }
    @StringSetter(MIN_OBSERVATIONS_SPECIAL_REGION) public void setMinObservationsSpecialRegion(int value) { minObservationsSpecialRegion = value; }

    public void applyContext(Config config) {
        penaltiesFile = resolvePath(config, penaltiesFile);
        specialRegionPath = resolvePaths(config, specialRegionPath);
    }

    private static List<String> splitPaths(String paths) {
        return Stream.of(paths.split(";"))
                .map(String::trim)
                .filter(path -> !path.isEmpty())
                .toList();
    }

    private static String resolvePath(Config config, String path) {
        if (path.isBlank()) return path;
        URL url = ConfigGroup.getInputFileURL(config.getContext(), path);
        return url == null ? path : url.getPath();
    }

    private static String resolvePaths(Config config, String paths) {
        return splitPaths(paths).stream().map(path -> resolvePath(config, path)).reduce((a, b) -> a + ";" + b).orElse("");
    }
}
