package org.eqasim.core.components.network_calibration;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.io.File;

public class NetworkCalibrationConfigGroup extends ReflectiveConfigGroup {
    static public final String GROUP_NAME = "eqasim:networkCalibration";
    static private final String ACTIVATE = "activate";
    static private final String CALIBRATE = "calibrate";
    static private final String UPDATE_INTERVAL = "updateInterval";
    static private final String CORRECT_CAPACITIES = "correctCapacities";
    static private final String MIN_SPEED = "minSpeed";
    static private final String MAX_CAPACITY = "maxCapacity";
    static private final String MIN_CAPACITY = "minCapacity";
    static private final String BETA = "beta";
    static private final String COUNTS_FILE = "countsFile";
    static private final String RAMP_FACTOR = "rampFactor";
    static private final String TRUNK_FACTOR = "trunkFactor";
    static private final String OBJECTIVE = "objective";
    static private final String MAX_PENALTY = "maxPenalty";
    static private final String MIN_PENALTY = "minPenalty";
    static private final String PENALTIES_FILE = "penaltiesFile";
    static private final String OBSERVED_SPEED_TRIPS_FILE = "observedSpeedTripsFile";
    static private final String FREESPEED_FACTORS_FILE = "freespeedFactorsFile";
    static private final String MIN_FREESPEED_FACTOR = "minFreespeedFactor";
    static private final String MAX_FREESPEED_FACTOR = "maxFreespeedFactor";
    static private final String MIN_TRIPS_PER_GROUP = "minTripsPerGroup";
    static private final String FREESPEED_WARMUP_ITERATIONS = "freespeedWarmupIterations";
    static private final String FREESPEED_SPECIAL_REGION_PATH = "freespeedSpecialRegionPath";
    static private final String PENALTIES_SPECIAL_REGION_PATH = "penaltiesSpecialRegionPath";
    static private final String PENALTIES_WARMUP_ITERATIONS = "penaltiesWarmupIterations";

    private boolean activate = false;
    private boolean calibrate = true;
    private int updateInterval = 5;
    private String countsFile = "";
    private double beta = 0.5;
    private boolean correctCapacities = true;
    private double minSpeed = 1.0; // km/h
    private double maxCapacity = 1900.0; // veh/h/lane (for the highest category, used to scale all capacities)
    private double minCapacity = 500.0; // veh/h/lane (for the lowest category)
    private double rampFactor = 1.0;
    private double trunkFactor = 1.0;
    private String objective = "";
    private double maxPenalty = 0.3;
    private double minPenalty = -0.1;
    private String penaltiesFile = "";
    private String freespeedSpecialRegionPath = "";
    private String penaltiesSpecialRegionPath = "";

    private String observedSpeedTripsFile = "";
    private String freespeedFactorsFile = "";
    private double minFreespeedFactor = 0.5;
    private double maxFreespeedFactor = 1.3;
    private int minTripsPerGroup = 50;
    private int freespeedWarmupIterations = 20;
    private int penaltiesWarmupIterations = 15;

    public NetworkCalibrationConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public final Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(ACTIVATE, "Whether to activate the module or not or not (default: false)");
        map.put(CALIBRATE, "Whether to update parameters during the run (default: true). If false, values are loaded from CSV files when provided, otherwise from network link attributes.");
        map.put(UPDATE_INTERVAL, "Interval (in iterations) at which calibration parameters are updated (default: 5)");
        map.put(CORRECT_CAPACITIES, "Whether to correct capacities for short links (default: true)");
        map.put(MIN_SPEED, "Minimum speed (in km/h) used in capacity correction (default: 2.0 km/h)");
        map.put(MAX_CAPACITY, "Maximum capacity (in veh/h/lane) used to scale all capacities (default: 1800 veh/h/lane)");
        map.put(MIN_CAPACITY, "Minimum capacity (in veh/h/lane) used to scale all capacities (default: 700 veh/h/lane)");
        map.put(COUNTS_FILE, "Path to the csv counts file (default: empty), it should contain columns 'linkId' and 'count', the counts are in veh/h/lane");
        map.put(BETA, "Beta of the exponential moving average used to update calibration parameters (default: 0.5)");
        map.put(RAMP_FACTOR, "Factor for ramp links (default: 1.0)");
        map.put(TRUNK_FACTOR, "Factor (compared to motorway) for trunk links (default: 1.0)");
        map.put(OBJECTIVE, "What should be calibrated (penalty, freespeed), or comma separated choice");
        map.put(MAX_PENALTY, "Maximum penalty to be applied to link categories when objective is penalty (default: 0.3)");
        map.put(MIN_PENALTY, "Minimum penalty to be applied to link categories when objective is penalty (default: -0.1)");
        map.put(PENALTIES_FILE, "Path to the csv penalties file (default: empty). Expected columns: linkCategory;isUrban;specialRegion;penalty. CSV values override penalties from link attributes.");
        map.put(OBSERVED_SPEED_TRIPS_FILE, "Path to observed trips CSV used when objective is freespeed. Expected columns: departure_x,departure_y,arrival_x,arrival_y,departure_hour,travel_time,traveled_distance");
        map.put(FREESPEED_FACTORS_FILE, "Path to freespeed factors CSV (default: empty). Expected columns: category;municipalityType;specialRegion;factor. CSV values override speedFactor link attributes.");
        map.put(FREESPEED_SPECIAL_REGION_PATH, "Semicolon-separated list of GeoJSON files defining freespeed special regions. Each file is assigned an index (1..N).");
        map.put(MIN_FREESPEED_FACTOR, "Lower bound applied to freespeed factors during freespeed calibration (default: 0.5)");
        map.put(MAX_FREESPEED_FACTOR, "Upper bound applied to freespeed factors during freespeed calibration (default: 1.3)");
        map.put(MIN_TRIPS_PER_GROUP, "Minimum number of routed observed trips required to update a freespeed group (default: 50)");
        map.put(FREESPEED_WARMUP_ITERATIONS, "Initial iterations where freespeed factors are not updated to let route assignment/network stabilize (default: 20)");
        map.put(PENALTIES_WARMUP_ITERATIONS, "Initial iterations where penalties are not updated to let route assignment/network stabilize (default: 15)");
        map.put(PENALTIES_SPECIAL_REGION_PATH, "This is the path to a geojson file that contains polygones of regions that would be treated differently");
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

    @StringGetter(PENALTIES_FILE)
    public String getPenaltiesFile() {
        return penaltiesFile;
    }
    @StringSetter(PENALTIES_FILE)
    public void setPenaltiesFile(String inputPenaltiesFile) {
        penaltiesFile = inputPenaltiesFile;
    }

    public boolean hasPenaltiesFile() {
        return !penaltiesFile.isEmpty() && penaltiesFile.endsWith(".csv");
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

    public boolean isCalibrationEnabled() {
        return calibrate;
    }

    @StringGetter(OBJECTIVE)
    public String getObjective() {
        return objective;
    }
    @StringSetter(OBJECTIVE)
    public void setObjective(String inputObjective) {
        this.objective = inputObjective;
    }

    public List<String> getAllObjectives() {
        return Stream.of(objective.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    public boolean isOneOfObjectives(String obj){
        return getAllObjectives().contains(obj);
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

    public boolean hasCountsFile() {
        return !countsFile.isEmpty() && countsFile.endsWith(".csv");
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

    @StringGetter(OBSERVED_SPEED_TRIPS_FILE)
    public String getObservedSpeedTripsFile() {
        return observedSpeedTripsFile;
    }
    @StringSetter(OBSERVED_SPEED_TRIPS_FILE)
    public void setObservedSpeedTripsFile(String inputObservedSpeedTripsFile) {
        observedSpeedTripsFile = inputObservedSpeedTripsFile;
    }

    public boolean hasObservedSpeedTripsFile() {
        return !observedSpeedTripsFile.isEmpty() && observedSpeedTripsFile.endsWith(".csv");
    }

    @StringGetter(FREESPEED_FACTORS_FILE)
    public String getFreespeedFactorsFile() {
        return freespeedFactorsFile;
    }

    @StringSetter(FREESPEED_FACTORS_FILE)
    public void setFreespeedFactorsFile(String inputFreespeedFactorsFile) {
        freespeedFactorsFile = inputFreespeedFactorsFile;
    }

    public boolean hasFreespeedFactorsFile() {
        return !freespeedFactorsFile.isEmpty() && freespeedFactorsFile.endsWith(".csv");
    }

    @StringGetter(FREESPEED_SPECIAL_REGION_PATH)
    public String getFreespeedSpecialRegionPath() {
        return freespeedSpecialRegionPath;
    }

    @StringSetter(FREESPEED_SPECIAL_REGION_PATH)
    public void setFreespeedSpecialRegionPath(String inputFreespeedSpecialRegion) {
        freespeedSpecialRegionPath = inputFreespeedSpecialRegion;
    }

    public List<String> getFreespeedSpecialRegionFiles() {
        return Stream.of(freespeedSpecialRegionPath.split(";"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    public boolean hasFreespeedSpecialRegion() {
        List<String> files = getFreespeedSpecialRegionFiles();
        return !files.isEmpty() && files.stream().allMatch(path -> path.endsWith("json") && new File(path).exists());
    }

    @StringGetter(MIN_FREESPEED_FACTOR)
    public double getMinFreespeedFactor() {
        return minFreespeedFactor;
    }
    @StringSetter(MIN_FREESPEED_FACTOR)
    public void setMinFreespeedFactor(double inputMinFreespeedFactor) {
        minFreespeedFactor = inputMinFreespeedFactor;
    }

    @StringGetter(MAX_FREESPEED_FACTOR)
    public double getMaxFreespeedFactor() {
        return maxFreespeedFactor;
    }
    @StringSetter(MAX_FREESPEED_FACTOR)
    public void setMaxFreespeedFactor(double inputMaxFreespeedFactor) {
        maxFreespeedFactor = inputMaxFreespeedFactor;
    }

    @StringGetter(MIN_TRIPS_PER_GROUP)
    public int getMinTripsPerGroup() {
        return minTripsPerGroup;
    }
    @StringSetter(MIN_TRIPS_PER_GROUP)
    public void setMinTripsPerGroup(int inputMinTripsPerGroup) {
        minTripsPerGroup = inputMinTripsPerGroup;
    }


    @StringGetter(FREESPEED_WARMUP_ITERATIONS)
    public int getFreespeedWarmupIterations() {
        return freespeedWarmupIterations;
    }

    @StringSetter(FREESPEED_WARMUP_ITERATIONS)
    public void setFreespeedWarmupIterations(int inputFreespeedWarmupIterations) {
        freespeedWarmupIterations = inputFreespeedWarmupIterations;
    }

    @StringGetter(PENALTIES_SPECIAL_REGION_PATH)
    public String getPenaltiesSpecialRegionPath() {
        return penaltiesSpecialRegionPath;
    }
    @StringSetter(PENALTIES_SPECIAL_REGION_PATH)
    public void setPenaltiesSpecialRegionPath(String inputPenaltiesSpecialRegion) {
        penaltiesSpecialRegionPath = inputPenaltiesSpecialRegion;
    }

    public List<String> getPenaltiesSpecialRegionFiles() {
        return Stream.of(penaltiesSpecialRegionPath.split(";"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    public boolean hasPenaltiesSpecialRegion() {
        List<String> files = getPenaltiesSpecialRegionFiles();
        return !files.isEmpty() && files.stream().allMatch(path -> path.endsWith("json") && new File(path).exists());
    }

    @StringGetter(PENALTIES_WARMUP_ITERATIONS)
    public int getPenaltiesWarmupIterations() {
        return penaltiesWarmupIterations;
    }
    @StringSetter(PENALTIES_WARMUP_ITERATIONS)
    public void setPenaltiesWarmupIterations(int inputPenaltiesWarmupIterations) {
        penaltiesWarmupIterations = inputPenaltiesWarmupIterations;
    }


    public void applyContext(Config config) {
        List<String> ps = getPenaltiesSpecialRegionFiles();
        if (!ps.isEmpty()) {
            StringBuilder psNew = new StringBuilder();
            for (String p : ps) {
                URL url = ConfigGroup.getInputFileURL(config.getContext(), p);
                if (!psNew.isEmpty()) psNew.append(";");
                psNew.append(url.getPath());
            }
            penaltiesSpecialRegionPath = psNew.toString();
        }

        List<String> fs = getFreespeedSpecialRegionFiles();
        if (!fs.isEmpty()) {
            StringBuilder fsNew = new StringBuilder();
            for (String f : fs) {
                URL url = ConfigGroup.getInputFileURL(config.getContext(), f);
                if (!fsNew.isEmpty()) fsNew.append(";");
                fsNew.append(url.getPath());
            }
            freespeedSpecialRegionPath = fsNew.toString();
        }
    }

    public static NetworkCalibrationConfigGroup getOrCreate(Config config) {
        NetworkCalibrationConfigGroup group = (NetworkCalibrationConfigGroup) config.getModules().get(GROUP_NAME);

        if (group == null) {
            group = new NetworkCalibrationConfigGroup();
            config.addModule(group);
        }
        group.applyContext(config);
        return group;
    }

}
