package org.eqasim.core.components.fast_calibration;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class AlphaCalibratorConfig extends ReflectiveConfigGroup {
    static public final String GROUP_NAME = "eqasim:alphaCalibration";

    static private final String ACTIVATE = "activate";
    static private final String BETA = "beta";
    static private final String CAR_MODE_SHARE = "carModeShare";
    static private final String PT_MODE_SHARE = "ptModeShare";
    static private final String WALK_MODE_SHARE = "walkModeShare";
    static private final String BIKE_MODE_SHARE = "bikeModeShare";
    static private final String CAR_PASSENGER_MODE_SHARE = "carPassengerModeShare";
    static private final String FILE_PATH = "filePath";
    static private final String LEVEL = "level";
    static private final String CALIBRATED_MODES = "calibratedModes";

    private boolean activate = false;
    private double beta = 0.5;
    private double carModeShare = 0.423;
    private double ptModeShare = 0.146;
    private double walkModeShare = 0.256;
    private double bikeModeShare = 0.083;
    private double carPassengerModeShare = 0.092;
    private String filePath = "";
    private String level = "global";
    private String calibratedModes = "car,pt,walk,bike";

    public AlphaCalibratorConfig() {
        super(GROUP_NAME);
    }

    @Override
    public final Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(ACTIVATE, "Whether to activate the alpha calibrator (default: false)");
        map.put(BETA, "Beta parameter for the alpha calibrator (default: 0.5, used as: alpha_i = beta * alpha_i + (1 - beta) * alpha_i_1)");
        map.put(CAR_MODE_SHARE, "Target mode share for car");
        map.put(PT_MODE_SHARE, "Target mode share for public transport");
        map.put(WALK_MODE_SHARE, "Target mode share for walking");
        map.put(BIKE_MODE_SHARE, "Target mode share for biking");
        map.put(CAR_PASSENGER_MODE_SHARE, "Target mode share for car passengers");
        map.put(FILE_PATH, "In case of using canton level calibration, this is the path to the csv file containing modes shares per canton");
        map.put(LEVEL, "Level of calibration: current implementation contains either 'global' or 'canton'");
        map.put(CALIBRATED_MODES, "Comma-separated list of modes to be calibrated (default: car,pt,walk,bike)," +
                "When car passenger is simulated, it can be added to the list as 'car_passenger'");
        return map;
    }

    @StringGetter(CALIBRATED_MODES)
    public List<String> getCalibratedModes() {
        return Stream.of(calibratedModes.split(",")).map(String::trim).toList();
    }
    @StringSetter(CALIBRATED_MODES)
    public void setCalibratedModes(String calibratedModes) {
        this.calibratedModes = calibratedModes;
    }

    @StringGetter(FILE_PATH)
    public String getFilePath() {
        return filePath;
    }
    @StringSetter(FILE_PATH)
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    @StringGetter(LEVEL)
    public String getLevel() {
        return level;
    }
    @StringSetter(LEVEL)
    public void setLevel(String level) {
        this.level = level;
    }

    @StringGetter(ACTIVATE)
    public boolean isActivate() {
        return activate;
    }
    @StringSetter(ACTIVATE)
    public void setActivate(boolean activate) {
        this.activate = activate;
    }

    @StringGetter(BETA)
    public double getBeta() {
        return beta;
    }
    @StringSetter(BETA)
    public void setBeta(double beta) {
        this.beta = beta;
    }

    @StringGetter(CAR_MODE_SHARE)
    public double getCarModeShare() {
        return carModeShare;
    }
    @StringSetter(CAR_MODE_SHARE)
    public void setCarModeShare(double carModeShare) {
        this.carModeShare = carModeShare;
    }

    @StringGetter(PT_MODE_SHARE)
    public double getPtModeShare() {
        return ptModeShare;
    }
    @StringSetter(PT_MODE_SHARE)
    public void setPtModeShare(double ptModeShare) {
        this.ptModeShare = ptModeShare;
    }

    @StringGetter(WALK_MODE_SHARE)
    public double getWalkModeShare() {
        return walkModeShare;
    }
    @StringSetter(WALK_MODE_SHARE)
    public void setWalkModeShare(double walkModeShare) {
        this.walkModeShare = walkModeShare;
    }

    @StringGetter(BIKE_MODE_SHARE)
    public double getBikeModeShare() {
        return bikeModeShare;
    }
    @StringSetter(BIKE_MODE_SHARE)
    public void setBikeModeShare(double bikeModeShare) {
        this.bikeModeShare = bikeModeShare;
    }

    @StringGetter(CAR_PASSENGER_MODE_SHARE)
    public double getCarPassengerModeShare() {
        return carPassengerModeShare;
    }
    @StringSetter(CAR_PASSENGER_MODE_SHARE)
    public void setCarPassengerModeShare(double carPassengerModeShare) {
        this.carPassengerModeShare = carPassengerModeShare;
    }


    public static AlphaCalibratorConfig getOrCreate(Config config) {
        AlphaCalibratorConfig group = (AlphaCalibratorConfig) config.getModules().get(GROUP_NAME);

        if (group == null) {
            group = new AlphaCalibratorConfig();
            config.addModule(group);
        }

        return group;
    }





}