package org.eqasim.core.components.network_calibration.demand_calibration.agent_ascs;

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AgentAscsCalibrationConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "agentAscsCalibration";

    private static final String ACTIVATE = "activate";
    private static final String UPDATE_INTERVAL = "updateInterval";
    private static final String WARMUP_ITERATIONS = "warmupIterations";
    private static final String LEARNING_RATE = "learningRate";
    private static final String MINIMUM_LEARNING_RATE = "minimumLearningRate";
    private static final String LEARNING_RATE_DECAY = "learningRateDecay";
    private static final String MIN_ASC_STEP = "minAscStep";
    private static final String MAX_ASC_STEP = "maxAscStep";
    private static final String ASC_DEADBAND = "ascDeadband";
    private static final String MAX_ABSOLUTE_ASC = "maxAbsoluteAsc";
    private static final String RELATIVE_FLOW_ERROR_THRESHOLD = "relativeFlowErrorThreshold";
    private static final String LOG_ERROR_EPSILON = "logErrorEpsilon";
    private static final String MAX_ABSOLUTE_LOG_ERROR = "maxAbsoluteLogError";
    private static final String OBSERVATION_SHRINKAGE = "observationShrinkage";
    private static final String MIN_TRIP_WEIGHT = "minTripWeight";
    private static final String LOW_COUNT_WEIGHT = "lowCountWeight";
    private static final String MEDIUM_COUNT_WEIGHT = "mediumCountWeight";
    private static final String HIGH_COUNT_WEIGHT = "highCountWeight";
    private static final String VERY_HIGH_COUNT_WEIGHT = "veryHighCountWeight";
    private static final String INITIAL_CELL_SIZE = "initialCellSize";
    private static final String MIN_CELL_SIZE = "minCellSize";
    private static final String MAX_POPULATION_PER_CELL = "maxPopulationPerCell";
    private static final String GRID_REBUILD_UPDATES = "gridRebuildUpdates";
    private static final String GRID_REBUILD_INITIAL_CELL_SIZES = "gridRebuildInitialCellSizes";
    private static final String GRID_REBUILD_MIN_CELL_SIZES = "gridRebuildMinCellSizes";
    private static final String GRID_REBUILD_MAX_POPULATIONS = "gridRebuildMaxPopulations";

    private boolean activate;
    private int updateInterval = 0;
    private int warmupIterations = 20;
    private double learningRate = 2.0;
    private double minimumLearningRate = 0.5;
    private double learningRateDecay = 0.983;
    private double minAscStep = 0.03;
    private double maxAscStep = 0.7;
    private double ascDeadband = 0.005;
    private double maxAbsoluteAsc = 3.0;
    private double relativeFlowErrorThreshold = 0.02;
    private double logErrorEpsilon = 1.0;
    private double maxAbsoluteLogError = 1.5;
    private double observationShrinkage = 100.0;
    private double minTripWeight = 0.05;
    private double lowCountWeight = 0.5;
    private double mediumCountWeight = 1.0;
    private double highCountWeight = 1.5;
    private double veryHighCountWeight = 2.0;
    private double initialCellSize = 16_000.0;
    private double minCellSize = 2_000.0;
    private int maxPopulationPerCell = 2_000;
    private String gridRebuildUpdates = "2,4,6";
    private String gridRebuildInitialCellSizes = "10000,8000,8000";
    private String gridRebuildMinCellSizes = "800,500,200";
    private String gridRebuildMaxPopulations = "1000,800,500";

    public AgentAscsCalibrationConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(ACTIVATE, "Whether per-agent car-ASC calibration is active (default: false)");
        comments.put(UPDATE_INTERVAL, "Iterations between ASC updates; values <= 0 use  1/(DMC strategy weight) (default: 0)");
        comments.put(WARMUP_ITERATIONS, "Iterations before the first ASC update (default: 20)");
        comments.put(LEARNING_RATE, "Initial ASC learning rate (default: 2.0)");
        comments.put(MINIMUM_LEARNING_RATE, "Lower bound for the decayed learning rate (default: 0.5)");
        comments.put(LEARNING_RATE_DECAY, "Per-iteration learning-rate decay factor (default: 0.983)");
        comments.put(MIN_ASC_STEP, "Minimum non-zero absolute ASC update (default: 0.05)");
        comments.put(MAX_ASC_STEP, "Maximum absolute ASC update (default: 0.5)");
        comments.put(ASC_DEADBAND, "ASC updates below this magnitude are ignored (default: 0.005)");
        comments.put(MAX_ABSOLUTE_ASC, "Absolute bound applied to each person's car ASC (default: 3.0)");
        comments.put(RELATIVE_FLOW_ERROR_THRESHOLD, "Relative flow error below which no OD correction is made (default: 0.02)");
        comments.put(LOG_ERROR_EPSILON, "Positive stabilizer in the OD log-error calculation (default: 1.0)");
        comments.put(MAX_ABSOLUTE_LOG_ERROR, "Absolute clipping bound for OD log errors (default: 1.5)");
        comments.put(OBSERVATION_SHRINKAGE, "OD confidence shrinkage before multiplying by simulation sample size (default: 100)");
        comments.put(MIN_TRIP_WEIGHT, "Minimum contribution per counted link on a trip (default: 0.05)");
        comments.put(LOW_COUNT_WEIGHT, "OD-error weight below the first count quartile (default: 0.5)");
        comments.put(MEDIUM_COUNT_WEIGHT, "OD-error weight between the first quartile and median (default: 1.0)");
        comments.put(HIGH_COUNT_WEIGHT, "OD-error weight between the median and third quartile (default: 1.5)");
        comments.put(VERY_HIGH_COUNT_WEIGHT, "OD-error weight above the third count quartile (default: 2.0)");
        comments.put(INITIAL_CELL_SIZE, "Initial OD-grid cell size in meters (default: 12000)");
        comments.put(MIN_CELL_SIZE, "Minimum OD-grid cell size in meters (default: 2000)");
        comments.put(MAX_POPULATION_PER_CELL, "Population threshold for splitting an OD-grid cell (default: 2000)");
        comments.put(GRID_REBUILD_UPDATES, "Comma-separated ASC update numbers at which the OD grid is rebuilt");
        comments.put(GRID_REBUILD_INITIAL_CELL_SIZES, "Comma-separated initial cell sizes for scheduled grid rebuilds");
        comments.put(GRID_REBUILD_MIN_CELL_SIZES, "Comma-separated minimum cell sizes for scheduled grid rebuilds");
        comments.put(GRID_REBUILD_MAX_POPULATIONS, "Comma-separated population thresholds for scheduled grid rebuilds");
        return comments;
    }

    @StringGetter(ACTIVATE) public boolean isActivated() { return activate; }
    @StringSetter(ACTIVATE) public void setActivate(boolean value) { activate = value; }
    @StringGetter(UPDATE_INTERVAL) public int getUpdateInterval() { return updateInterval; }
    @StringSetter(UPDATE_INTERVAL) public void setUpdateInterval(int value) { updateInterval = value; }
    @StringGetter(WARMUP_ITERATIONS) public int getWarmupIterations() { return warmupIterations; }
    @StringSetter(WARMUP_ITERATIONS) public void setWarmupIterations(int value) { warmupIterations = value; }
    @StringGetter(LEARNING_RATE) public double getLearningRate() { return learningRate; }
    @StringSetter(LEARNING_RATE) public void setLearningRate(double value) { learningRate = value; }
    @StringGetter(MINIMUM_LEARNING_RATE) public double getMinimumLearningRate() { return minimumLearningRate; }
    @StringSetter(MINIMUM_LEARNING_RATE) public void setMinimumLearningRate(double value) { minimumLearningRate = value; }
    @StringGetter(LEARNING_RATE_DECAY) public double getLearningRateDecay() { return learningRateDecay; }
    @StringSetter(LEARNING_RATE_DECAY) public void setLearningRateDecay(double value) { learningRateDecay = value; }
    @StringGetter(MIN_ASC_STEP) public double getMinAscStep() { return minAscStep; }
    @StringSetter(MIN_ASC_STEP) public void setMinAscStep(double value) { minAscStep = value; }
    @StringGetter(MAX_ASC_STEP) public double getMaxAscStep() { return maxAscStep; }
    @StringSetter(MAX_ASC_STEP) public void setMaxAscStep(double value) { maxAscStep = value; }
    @StringGetter(ASC_DEADBAND) public double getAscDeadband() { return ascDeadband; }
    @StringSetter(ASC_DEADBAND) public void setAscDeadband(double value) { ascDeadband = value; }
    @StringGetter(MAX_ABSOLUTE_ASC) public double getMaxAbsoluteAsc() { return maxAbsoluteAsc; }
    @StringSetter(MAX_ABSOLUTE_ASC) public void setMaxAbsoluteAsc(double value) { maxAbsoluteAsc = value; }
    @StringGetter(RELATIVE_FLOW_ERROR_THRESHOLD) public double getRelativeFlowErrorThreshold() { return relativeFlowErrorThreshold; }
    @StringSetter(RELATIVE_FLOW_ERROR_THRESHOLD) public void setRelativeFlowErrorThreshold(double value) { relativeFlowErrorThreshold = value; }
    @StringGetter(LOG_ERROR_EPSILON) public double getLogErrorEpsilon() { return logErrorEpsilon; }
    @StringSetter(LOG_ERROR_EPSILON) public void setLogErrorEpsilon(double value) { logErrorEpsilon = value; }
    @StringGetter(MAX_ABSOLUTE_LOG_ERROR) public double getMaxAbsoluteLogError() { return maxAbsoluteLogError; }
    @StringSetter(MAX_ABSOLUTE_LOG_ERROR) public void setMaxAbsoluteLogError(double value) { maxAbsoluteLogError = value; }
    @StringGetter(OBSERVATION_SHRINKAGE) public double getObservationShrinkage() { return observationShrinkage; }
    @StringSetter(OBSERVATION_SHRINKAGE) public void setObservationShrinkage(double value) { observationShrinkage = value; }
    @StringGetter(MIN_TRIP_WEIGHT) public double getMinTripWeight() { return minTripWeight; }
    @StringSetter(MIN_TRIP_WEIGHT) public void setMinTripWeight(double value) { minTripWeight = value; }
    @StringGetter(LOW_COUNT_WEIGHT) public double getLowCountWeight() { return lowCountWeight; }
    @StringSetter(LOW_COUNT_WEIGHT) public void setLowCountWeight(double value) { lowCountWeight = value; }
    @StringGetter(MEDIUM_COUNT_WEIGHT) public double getMediumCountWeight() { return mediumCountWeight; }
    @StringSetter(MEDIUM_COUNT_WEIGHT) public void setMediumCountWeight(double value) { mediumCountWeight = value; }
    @StringGetter(HIGH_COUNT_WEIGHT) public double getHighCountWeight() { return highCountWeight; }
    @StringSetter(HIGH_COUNT_WEIGHT) public void setHighCountWeight(double value) { highCountWeight = value; }
    @StringGetter(VERY_HIGH_COUNT_WEIGHT) public double getVeryHighCountWeight() { return veryHighCountWeight; }
    @StringSetter(VERY_HIGH_COUNT_WEIGHT) public void setVeryHighCountWeight(double value) { veryHighCountWeight = value; }
    @StringGetter(INITIAL_CELL_SIZE) public double getInitialCellSize() { return initialCellSize; }
    @StringSetter(INITIAL_CELL_SIZE) public void setInitialCellSize(double value) { initialCellSize = value; }
    @StringGetter(MIN_CELL_SIZE) public double getMinCellSize() { return minCellSize; }
    @StringSetter(MIN_CELL_SIZE) public void setMinCellSize(double value) { minCellSize = value; }
    @StringGetter(MAX_POPULATION_PER_CELL) public int getMaxPopulationPerCell() { return maxPopulationPerCell; }
    @StringSetter(MAX_POPULATION_PER_CELL) public void setMaxPopulationPerCell(int value) { maxPopulationPerCell = value; }
    @StringGetter(GRID_REBUILD_UPDATES) public String getGridRebuildUpdatesAsString() { return gridRebuildUpdates; }
    @StringSetter(GRID_REBUILD_UPDATES) public void setGridRebuildUpdates(String value) { gridRebuildUpdates = value; }
    public List<Integer> getGridRebuildUpdates() { return parseIntegers(gridRebuildUpdates); }
    @StringGetter(GRID_REBUILD_INITIAL_CELL_SIZES) public String getGridRebuildInitialCellSizesAsString() { return gridRebuildInitialCellSizes; }
    @StringSetter(GRID_REBUILD_INITIAL_CELL_SIZES) public void setGridRebuildInitialCellSizes(String value) { gridRebuildInitialCellSizes = value; }
    public List<Double> getGridRebuildInitialCellSizes() { return parseDoubles(gridRebuildInitialCellSizes); }
    @StringGetter(GRID_REBUILD_MIN_CELL_SIZES) public String getGridRebuildMinCellSizesAsString() { return gridRebuildMinCellSizes; }
    @StringSetter(GRID_REBUILD_MIN_CELL_SIZES) public void setGridRebuildMinCellSizes(String value) { gridRebuildMinCellSizes = value; }
    public List<Double> getGridRebuildMinCellSizes() { return parseDoubles(gridRebuildMinCellSizes); }
    @StringGetter(GRID_REBUILD_MAX_POPULATIONS) public String getGridRebuildMaxPopulationsAsString() { return gridRebuildMaxPopulations; }
    @StringSetter(GRID_REBUILD_MAX_POPULATIONS) public void setGridRebuildMaxPopulations(String value) { gridRebuildMaxPopulations = value; }
    public List<Integer> getGridRebuildMaxPopulations() { return parseIntegers(gridRebuildMaxPopulations); }

    private static List<Integer> parseIntegers(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isEmpty()).map(Integer::parseInt).toList();
    }

    private static List<Double> parseDoubles(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isEmpty()).map(Double::parseDouble).toList();
    }
}
