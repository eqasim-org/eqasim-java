package org.eqasim.core.components.network_calibration.freespeed_calibration;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class FreeSpeedCalibrationConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "freespeedCalibration";

    private static final String ACTIVATE = "activate";
    private static final String UPDATE_INTERVAL = "updateInterval";
    private static final String WARMUP_ITERATIONS = "warmupIterations";
    private static final String OBSERVED_TRIPS_FILE = "observedTripsFile";
    private static final String FACTORS_FILE = "factorsFile";
    private static final String SPECIAL_REGION_PATH = "specialRegionPath";
    private static final String MIN_FACTOR = "minFactor";
    private static final String MAX_FACTOR = "maxFactor";
    private static final String MIN_TRIPS_PER_GROUP = "minTripsPerGroup";
    private static final String LEARNING_RATE = "learningRate";
    private static final String HISTORY_SIZE = "historySize";
    private static final String MIN_EFFECTIVE_LEARNING_RATE = "minEffectiveLearningRate";
    private static final String MAX_EFFECTIVE_LEARNING_RATE = "maxEffectiveLearningRate";
    private static final String MAX_FACTOR_STEP = "maxFactorStep";
    private static final String UNBOUNDED_INITIAL_UPDATES = "unboundedInitialUpdates";
    private static final String MIN_IMPROVEMENT_RATIO = "minImprovementRatio";
    private static final String NO_IMPROVEMENT_PATIENCE = "noImprovementPatience";
    private static final String FROZEN_ITERATIONS = "frozenIterations";
    private static final String KEEP_FROZEN_FROM_ITERATION = "keepFrozenFromIteration";
    private static final String TRAVEL_TIME_TOLERANCE = "travelTimeTolerance";
    private static final String IMBALANCE_THRESHOLD = "imbalanceThreshold";
    private static final String TRIM_FRACTION = "trimFraction";
    private static final String MIN_TRIP_DISTANCE = "minTripDistance";
    private static final String MIN_TRIP_TRAVEL_TIME = "minTripTravelTime";
    private static final String MAX_DISTANCE_ERROR = "maxDistanceError";
    private static final String MIN_TRAVEL_TIME_ERROR = "minTravelTimeError";
    private static final String MAX_TRAVEL_TIME_ERROR = "maxTravelTimeError";

    private boolean activate;
    private int updateInterval = 5;
    private int warmupIterations = 30;
    private String observedTripsFile = "";
    private String factorsFile = "";
    private String specialRegionPath = "";
    private double minFactor = 0.5;
    private double maxFactor = 1.3;
    private int minTripsPerGroup = 50;
    private double learningRate = 0.7;
    private int historySize = 5;
    private double minEffectiveLearningRate = 0.3;
    private double maxEffectiveLearningRate = 0.9;
    private double maxFactorStep = 0.08;
    private int unboundedInitialUpdates = 2;
    private double minImprovementRatio = 0.015;
    private int noImprovementPatience = 3;
    private int frozenIterations = 4;
    private int keepFrozenFromIteration = 90;
    private double travelTimeTolerance = 0.02;
    private double imbalanceThreshold = 0.02;
    private double trimFraction = 0.20;
    private double minTripDistance = 1_000.0;
    private double minTripTravelTime = 180.0;
    private double maxDistanceError = 0.10;
    private double minTravelTimeError = -0.70;
    private double maxTravelTimeError = 2.0;

    public FreeSpeedCalibrationConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(ACTIVATE, "Whether freespeed calibration is active (default: false)");
        comments.put(UPDATE_INTERVAL, "Iterations between freespeed-factor updates (default: 5)");
        comments.put(WARMUP_ITERATIONS, "Iterations before the first freespeed update (default: 20)");
        comments.put(OBSERVED_TRIPS_FILE, "Observed trips CSV used for freespeed calibration");
        comments.put(FACTORS_FILE, "Optional initial factors CSV with columns category;municipalityType;specialRegion;factor");
        comments.put(SPECIAL_REGION_PATH, "Semicolon-separated GeoJSON files defining freespeed-calibration regions");
        comments.put(MIN_FACTOR, "Minimum freespeed factor (default: 0.5)");
        comments.put(MAX_FACTOR, "Maximum freespeed factor (default: 1.3)");
        comments.put(MIN_TRIPS_PER_GROUP, "Minimum routed trips required to update a group (default: 50)");
        comments.put(LEARNING_RATE, "Base factor-update learning rate (default: 0.5)");
        comments.put(HISTORY_SIZE, "Number of factor decisions retained for convergence diagnostics (default: 5)");
        comments.put(MIN_EFFECTIVE_LEARNING_RATE, "Minimum adaptive learning rate (default: 0.4)");
        comments.put(MAX_EFFECTIVE_LEARNING_RATE, "Maximum adaptive learning rate (default: 0.8)");
        comments.put(MAX_FACTOR_STEP, "Maximum absolute factor change after initial updates (default: 0.08)");
        comments.put(UNBOUNDED_INITIAL_UPDATES, "Initial updates that may use the full factor range (default: 2)");
        comments.put(MIN_IMPROVEMENT_RATIO, "Minimum relative error improvement (default: 0.015)");
        comments.put(NO_IMPROVEMENT_PATIENCE, "Updates without improvement before freezing a group (default: 3)");
        comments.put(FROZEN_ITERATIONS, "Length of a temporary freeze cycle (default: 4)");
        comments.put(KEEP_FROZEN_FROM_ITERATION, "Iteration after which a frozen group stays frozen (default: 90)");
        comments.put(TRAVEL_TIME_TOLERANCE, "Relative travel-time tolerance used to classify fast and slow observations (default: 0.02)");
        comments.put(IMBALANCE_THRESHOLD, "Minimum fast/slow imbalance required for an update (default: 0.02)");
        comments.put(TRIM_FRACTION, "Fraction trimmed from each tail of factor/error observations (default: 0.20)");
        comments.put(MIN_TRIP_DISTANCE, "Minimum observed and simulated trip distance in meters (default: 1000)");
        comments.put(MIN_TRIP_TRAVEL_TIME, "Minimum observed and simulated travel time in seconds (default: 180)");
        comments.put(MAX_DISTANCE_ERROR, "Maximum absolute relative routed-distance error (default: 0.10)");
        comments.put(MIN_TRAVEL_TIME_ERROR, "Minimum accepted relative travel-time error (default: -0.30)");
        comments.put(MAX_TRAVEL_TIME_ERROR, "Maximum accepted relative travel-time error (default: 2.0)");
        return comments;
    }

    @StringGetter(ACTIVATE) public boolean isActivated() { return activate; }
    @StringSetter(ACTIVATE) public void setActivate(boolean value) { activate = value; }
    @StringGetter(UPDATE_INTERVAL) public int getUpdateInterval() { return updateInterval; }
    @StringSetter(UPDATE_INTERVAL) public void setUpdateInterval(int value) { updateInterval = value; }
    @StringGetter(WARMUP_ITERATIONS) public int getWarmupIterations() { return warmupIterations; }
    @StringSetter(WARMUP_ITERATIONS) public void setWarmupIterations(int value) { warmupIterations = value; }
    @StringGetter(OBSERVED_TRIPS_FILE) public String getObservedTripsFile() { return observedTripsFile; }
    @StringSetter(OBSERVED_TRIPS_FILE) public void setObservedTripsFile(String value) { observedTripsFile = value; }
    public boolean hasObservedTripsFile() { return !observedTripsFile.isBlank() && observedTripsFile.endsWith(".csv"); }
    @StringGetter(FACTORS_FILE) public String getFactorsFile() { return factorsFile; }
    @StringSetter(FACTORS_FILE) public void setFactorsFile(String value) { factorsFile = value; }
    public boolean hasFactorsFile() { return !factorsFile.isBlank() && factorsFile.endsWith(".csv"); }
    @StringGetter(SPECIAL_REGION_PATH) public String getSpecialRegionPath() { return specialRegionPath; }
    @StringSetter(SPECIAL_REGION_PATH) public void setSpecialRegionPath(String value) { specialRegionPath = value; }
    public List<String> getSpecialRegionFiles() { return splitPaths(specialRegionPath); }
    public boolean hasSpecialRegions() { return !getSpecialRegionFiles().isEmpty() && getSpecialRegionFiles().stream().allMatch(path -> path.endsWith("json") && new File(path).exists()); }
    @StringGetter(MIN_FACTOR) public double getMinFactor() { return minFactor; }
    @StringSetter(MIN_FACTOR) public void setMinFactor(double value) { minFactor = value; }
    @StringGetter(MAX_FACTOR) public double getMaxFactor() { return maxFactor; }
    @StringSetter(MAX_FACTOR) public void setMaxFactor(double value) { maxFactor = value; }
    @StringGetter(MIN_TRIPS_PER_GROUP) public int getMinTripsPerGroup() { return minTripsPerGroup; }
    @StringSetter(MIN_TRIPS_PER_GROUP) public void setMinTripsPerGroup(int value) { minTripsPerGroup = value; }
    @StringGetter(LEARNING_RATE) public double getLearningRate() { return learningRate; }
    @StringSetter(LEARNING_RATE) public void setLearningRate(double value) { learningRate = value; }
    @StringGetter(HISTORY_SIZE) public int getHistorySize() { return historySize; }
    @StringSetter(HISTORY_SIZE) public void setHistorySize(int value) { historySize = value; }
    @StringGetter(MIN_EFFECTIVE_LEARNING_RATE) public double getMinEffectiveLearningRate() { return minEffectiveLearningRate; }
    @StringSetter(MIN_EFFECTIVE_LEARNING_RATE) public void setMinEffectiveLearningRate(double value) { minEffectiveLearningRate = value; }
    @StringGetter(MAX_EFFECTIVE_LEARNING_RATE) public double getMaxEffectiveLearningRate() { return maxEffectiveLearningRate; }
    @StringSetter(MAX_EFFECTIVE_LEARNING_RATE) public void setMaxEffectiveLearningRate(double value) { maxEffectiveLearningRate = value; }
    @StringGetter(MAX_FACTOR_STEP) public double getMaxFactorStep() { return maxFactorStep; }
    @StringSetter(MAX_FACTOR_STEP) public void setMaxFactorStep(double value) { maxFactorStep = value; }
    @StringGetter(UNBOUNDED_INITIAL_UPDATES) public int getUnboundedInitialUpdates() { return unboundedInitialUpdates; }
    @StringSetter(UNBOUNDED_INITIAL_UPDATES) public void setUnboundedInitialUpdates(int value) { unboundedInitialUpdates = value; }
    @StringGetter(MIN_IMPROVEMENT_RATIO) public double getMinImprovementRatio() { return minImprovementRatio; }
    @StringSetter(MIN_IMPROVEMENT_RATIO) public void setMinImprovementRatio(double value) { minImprovementRatio = value; }
    @StringGetter(NO_IMPROVEMENT_PATIENCE) public int getNoImprovementPatience() { return noImprovementPatience; }
    @StringSetter(NO_IMPROVEMENT_PATIENCE) public void setNoImprovementPatience(int value) { noImprovementPatience = value; }
    @StringGetter(FROZEN_ITERATIONS) public int getFrozenIterations() { return frozenIterations; }
    @StringSetter(FROZEN_ITERATIONS) public void setFrozenIterations(int value) { frozenIterations = value; }
    @StringGetter(KEEP_FROZEN_FROM_ITERATION) public int getKeepFrozenFromIteration() { return keepFrozenFromIteration; }
    @StringSetter(KEEP_FROZEN_FROM_ITERATION) public void setKeepFrozenFromIteration(int value) { keepFrozenFromIteration = value; }
    @StringGetter(TRAVEL_TIME_TOLERANCE) public double getTravelTimeTolerance() { return travelTimeTolerance; }
    @StringSetter(TRAVEL_TIME_TOLERANCE) public void setTravelTimeTolerance(double value) { travelTimeTolerance = value; }
    @StringGetter(IMBALANCE_THRESHOLD) public double getImbalanceThreshold() { return imbalanceThreshold; }
    @StringSetter(IMBALANCE_THRESHOLD) public void setImbalanceThreshold(double value) { imbalanceThreshold = value; }
    @StringGetter(TRIM_FRACTION) public double getTrimFraction() { return trimFraction; }
    @StringSetter(TRIM_FRACTION) public void setTrimFraction(double value) { trimFraction = value; }
    @StringGetter(MIN_TRIP_DISTANCE) public double getMinTripDistance() { return minTripDistance; }
    @StringSetter(MIN_TRIP_DISTANCE) public void setMinTripDistance(double value) { minTripDistance = value; }
    @StringGetter(MIN_TRIP_TRAVEL_TIME) public double getMinTripTravelTime() { return minTripTravelTime; }
    @StringSetter(MIN_TRIP_TRAVEL_TIME) public void setMinTripTravelTime(double value) { minTripTravelTime = value; }
    @StringGetter(MAX_DISTANCE_ERROR) public double getMaxDistanceError() { return maxDistanceError; }
    @StringSetter(MAX_DISTANCE_ERROR) public void setMaxDistanceError(double value) { maxDistanceError = value; }
    @StringGetter(MIN_TRAVEL_TIME_ERROR) public double getMinTravelTimeError() { return minTravelTimeError; }
    @StringSetter(MIN_TRAVEL_TIME_ERROR) public void setMinTravelTimeError(double value) { minTravelTimeError = value; }
    @StringGetter(MAX_TRAVEL_TIME_ERROR) public double getMaxTravelTimeError() { return maxTravelTimeError; }
    @StringSetter(MAX_TRAVEL_TIME_ERROR) public void setMaxTravelTimeError(double value) { maxTravelTimeError = value; }

    public void applyContext(Config config) {
        observedTripsFile = resolvePath(config, observedTripsFile);
        factorsFile = resolvePath(config, factorsFile);
        specialRegionPath = splitPaths(specialRegionPath).stream()
                .map(path -> resolvePath(config, path))
                .reduce((a, b) -> a + ";" + b)
                .orElse("");
    }

    private static List<String> splitPaths(String paths) {
        return Stream.of(paths.split(";")).map(String::trim).filter(path -> !path.isEmpty()).toList();
    }

    private static String resolvePath(Config config, String path) {
        if (path.isBlank()) return path;
        URL url = ConfigGroup.getInputFileURL(config.getContext(), path);
        return url == null ? path : url.getPath();
    }
}
