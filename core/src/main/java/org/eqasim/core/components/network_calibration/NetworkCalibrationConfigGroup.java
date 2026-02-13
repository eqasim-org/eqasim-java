package org.eqasim.core.components.network_calibration;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class NetworkCalibrationConfigGroup extends ReflectiveConfigGroup {
    static public final String GROUP_NAME = "eqasim:networkCalibration";
    static private final String ACTIVATE = "activate";
    static private final String CALIBRATE = "calibrate";
    static private final String UPDATE_INTERVAL = "updateInterval";
    static private final String SAVE_NETWORK_INTERVAL = "saveNetworkInterval";
    static private final String CORRECT_CAPACITIES = "correctCapacities";
    static private final String MIN_SPEED = "minSpeed";
    static private final String MAX_CAPACITY = "maxCapacity";
    static private final String MIN_CAPACITY = "minCapacity";
    static private final String BETA = "beta";
    static private final String HOUR_START_COUNTS = "hourStartCounts";
    static private final String HOUR_END_COUNTS = "hourEndCounts";
    static private final String COUNTS_FILE = "countsFile";
    static private final String AVERAGE_COUNTS_PER_CATEGORY_FILE = "averageCountsPerCategoryFile";
    static private final String CATEGORIES_TO_CALIBRATE = "categoriesToCalibrate";
    static private final String RAMP_FACTOR = "rampFactor";
    static private final String TRUNK_FACTOR = "trunkFactor";
    static private final String OBJECTIVE = "objective";
    static private final String MAX_PENALTY = "maxPenalty";
    static private final String MIN_PENALTY = "minPenalty";
    static private final String PENALTIES_FILE = "penaltiesFile";
    static private final String SEPARATE_URBAN_ROADS = "separateUrbanRoads";

    private boolean activate = false;
    private boolean calibrate = true;
    private int updateInterval = 5;
    private int saveNetworkInterval = 5;
    private String countsFile = "";
    private String averageCountsPerCategoryFile = "";
    private double beta = 0.5;
    private int hourStartCounts = 0;
    private int hourEndCounts = 24;
    private boolean correctCapacities = true;
    private double minSpeed = 2.0; // km/h
    private double maxCapacity = 1800.0; // veh/h/lane (for the highest category, used to scale all capacities)
    private double minCapacity = 700.0; // veh/h/lane (for the lowest category)
    private String categoriesToCalibrate = "1,2,3,4,5,11,12,13,14,15";
    private double rampFactor = 1.0;
    private double trunkFactor = 1.0;
    private String objective = "capacity";
    private double maxPenalty = 0.3;
    private double minPenalty = -0.1;
    private String penaltiesFile = "";
    private boolean separateUrbanRoads = false;

    public NetworkCalibrationConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public final Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(ACTIVATE, "Whether to activate the module or not or not (default: false)");
        map.put(CALIBRATE, "Whether to run calibration or not (default: true)");
        map.put(UPDATE_INTERVAL, "Interval (in iterations) at which the capacities are updated (default: 5)");
        map.put(SAVE_NETWORK_INTERVAL, "Interval (in iterations) at which the network is saved (default: 5)");
        map.put(CORRECT_CAPACITIES, "Whether to correct capacities for short links (default: true)");
        map.put(MIN_SPEED, "Minimum speed (in km/h) used in capacity correction (default: 2.0 km/h)");
        map.put(MAX_CAPACITY, "Maximum capacity (in veh/h/lane) used to scale all capacities (default: 1800 veh/h/lane)");
        map.put(MIN_CAPACITY, "Minimum capacity (in veh/h/lane) used to scale all capacities (default: 700 veh/h/lane)");
        map.put(COUNTS_FILE, "Path to the csv counts file (default: empty), it should contain columns 'linkId' and 'count', the counts are in veh/h/lane");
        map.put(AVERAGE_COUNTS_PER_CATEGORY_FILE, "Path to the csv average counts per category file (default: empty), it should contain columns 'category' and 'averageCount'");
        map.put(BETA, "Beta of the exponential moving average used to update capacities (default: 0.5)");
        map.put(HOUR_START_COUNTS, "Hour of the day to start considering counts (default: 0), 24-hour format. "
                + "This class uses timeBin manager from intersection delay, thus the start and end hours should be within the time bin range.");
        map.put(HOUR_END_COUNTS, "Hour of the day to end considering counts (default: 24), 24-hour format.");
        map.put(CATEGORIES_TO_CALIBRATE, "Comma-separated list of link categories to calibrate (default: '1,2,3,4,5')");
        map.put(RAMP_FACTOR, "Factor for ramp links (default: 1.0)");
        map.put(TRUNK_FACTOR, "Factor (compared to motorway) for trunk links (default: 1.0)");
        map.put(OBJECTIVE, "What should be calibrated (capacity, penalty)");
        map.put(MAX_PENALTY, "Maximum penalty to be applied to link categories when objective is penalty (default: 0.3)");
        map.put(MIN_PENALTY, "Minimum penalty to be applied to link categories when objective is penalty (default: -0.1)");
        map.put(PENALTIES_FILE, "Path to the csv penalties file (default: empty), used to initialize link category penalties"+
                ", it should contain columns 'category' and 'penalty'. When it is provided, the penalties will not be updated during the simulation.");
        map.put(SEPARATE_URBAN_ROADS, "Whether to treat urban roads (links within urban areas) as a separate category for calibration (default: false)");
        return map;
    }

    @StringGetter(MAX_PENALTY)
    public double getMaxPenalty() {
        return maxPenalty;
    }
    @StringSetter(MAX_PENALTY)
    public void setMaxPenalty(double inputMaxPenalty) {
        maxPenalty = inputMaxPenalty;
    }

    @StringGetter(MIN_PENALTY)
    public double getMinPenalty() {
        return minPenalty;
    }
    @StringSetter(MIN_PENALTY)
    public void setMinPenalty(double inputMinPenalty) {
        minPenalty = inputMinPenalty;
    }

    @StringGetter(SEPARATE_URBAN_ROADS)
    public boolean getSeparateUrbanRoads() {
        return separateUrbanRoads;
    }
    @StringSetter(SEPARATE_URBAN_ROADS)
    public void setSeparateUrbanRoads(boolean inputSeparateUrbanRoads) {
        separateUrbanRoads = inputSeparateUrbanRoads;
    }

    @StringGetter(PENALTIES_FILE)
    public String getPenaltiesFile() {
        return penaltiesFile;
    }
    @StringSetter(PENALTIES_FILE)
    public void setPenaltiesFile(String inputPenaltiesFile) {
        penaltiesFile = inputPenaltiesFile;
    }

    @StringGetter(ACTIVATE)
    public boolean isActivated() {
        return activate;
    }
    @StringSetter(ACTIVATE)
    public void setActivate(boolean inputActivate) {
        activate = inputActivate;
    }

    @StringGetter(CALIBRATE)
    public boolean getCalibrate() {
        return calibrate;
    }
    @StringSetter(CALIBRATE)
    public void setCalibrate(boolean inputCalibrate) {
        calibrate = inputCalibrate;
    }

    @StringGetter(OBJECTIVE)
    public String getObjective() {
        return objective;
    }
    @StringSetter(OBJECTIVE)
    public void setObjective(String inputObjective) {
        this.objective = inputObjective;
    }

    @StringGetter(BETA)
    public double getBeta() {
        return beta;
    }
    @StringSetter(BETA)
    public void setBeta(double inputBeta) {
        beta = inputBeta;
    }

    @StringGetter(COUNTS_FILE)
    public String getCountsFile() {
        return countsFile;
    }
    @StringSetter(COUNTS_FILE)
    public void setCountsFile(String inputCountsFile) {
        countsFile = inputCountsFile;
    }

    @StringGetter(AVERAGE_COUNTS_PER_CATEGORY_FILE)
    public String getAverageCountsPerCategoryFile() {
        return averageCountsPerCategoryFile;
    }
    @StringSetter(AVERAGE_COUNTS_PER_CATEGORY_FILE)
    public void setAverageCountsPerCategoryFile(String inputAverageCountsPerCategoryFile) {
        averageCountsPerCategoryFile = inputAverageCountsPerCategoryFile;
    }

    public boolean hasCountsFile() {
        return !countsFile.isEmpty() && countsFile.endsWith(".csv");
    }

    public boolean hasAverageCountsPerCategoryFile() {
        return !averageCountsPerCategoryFile.isEmpty() && averageCountsPerCategoryFile.endsWith(".csv");
    }

    @StringGetter(HOUR_START_COUNTS)
    public int getHourStartCounts() {
        return hourStartCounts;
    }
    @StringSetter(HOUR_START_COUNTS)
    public void setHourStartCounts(int inputHourStartCounts) {
        hourStartCounts = inputHourStartCounts;
    }
    @StringGetter(HOUR_END_COUNTS)
    public int getHourEndCounts() {
        return hourEndCounts;
    }
    @StringSetter(HOUR_END_COUNTS)
    public void setHourEndCounts(int inputHourEndCounts) {
        hourEndCounts = inputHourEndCounts;
    }

    @StringGetter(CORRECT_CAPACITIES)
    public boolean getCorrectCapacities() {
        return correctCapacities;
    }
    @StringSetter(CORRECT_CAPACITIES)
    public void setCorrectCapacities(boolean inputCorrectCapacities) {
        correctCapacities = inputCorrectCapacities;
    }

    @StringGetter(MIN_SPEED)
    public double getMinSpeed() {
        return minSpeed;
    }
    @StringSetter(MIN_SPEED)
    public void setMinSpeed(double inputMinSpeed) {
        minSpeed = inputMinSpeed;
    }

    @StringGetter(MAX_CAPACITY)
    public double getMaxCapacity() {
        return maxCapacity;
    }
    @StringSetter(MAX_CAPACITY)
    public void setMaxCapacity(double inputMaxCapacity) {
        maxCapacity = inputMaxCapacity;
    }

    @StringGetter(MIN_CAPACITY)
    public double getMinCapacity() {
        return minCapacity;
    }
    @StringSetter(MIN_CAPACITY)
    public void setMinCapacity(double inputMinCapacities) {
        minCapacity = inputMinCapacities;
    }

    @StringGetter(UPDATE_INTERVAL)
    public int getUpdateInterval() {
        return updateInterval;
    }
    @StringSetter(UPDATE_INTERVAL)
    public void setUpdateInterval(int inputUpdateInterval) {
        updateInterval = inputUpdateInterval;
    }
    @StringGetter(SAVE_NETWORK_INTERVAL)
    public int getSaveNetworkInterval() {
        return saveNetworkInterval;
    }
    @StringSetter(SAVE_NETWORK_INTERVAL)
    public void setSaveNetworkInterval(int inputSaveNetworkInterval) {
        saveNetworkInterval = inputSaveNetworkInterval;
    }

    public List<Integer> getCategoriesToCalibrationAsList() {
        return Stream.of(categoriesToCalibrate.split(","))
            .map(String::trim)
            .map(Integer::valueOf)
            .toList();
    }

    @StringGetter(CATEGORIES_TO_CALIBRATE)
    public String getCategoriesToCalibrate() {
        return categoriesToCalibrate;
    }

    @StringSetter(CATEGORIES_TO_CALIBRATE)
    public void setCategoriesToCalibrate(String inputCategoriesToCalibrate) {
        categoriesToCalibrate = inputCategoriesToCalibrate;
    }

    @StringGetter(RAMP_FACTOR)
    public double getRampFactor() {
        return rampFactor;
    }
    @StringSetter(RAMP_FACTOR)
    public void setRampFactor(double inputRampCapacityFactor) {
        rampFactor = inputRampCapacityFactor;
    }

    @StringGetter(TRUNK_FACTOR)
    public double getTrunkFactor() {
        return trunkFactor;
    }
    @StringSetter(TRUNK_FACTOR)
    public void setTrunkFactor(double inputTrunkCapacityFactor) {
        trunkFactor = inputTrunkCapacityFactor;
    }

    public static NetworkCalibrationConfigGroup getOrCreate(Config config) {
        NetworkCalibrationConfigGroup group = (NetworkCalibrationConfigGroup) config.getModules().get(GROUP_NAME);

        if (group == null) {
            group = new NetworkCalibrationConfigGroup();
            config.addModule(group);
        }

        return group;
    }

}
