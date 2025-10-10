package org.eqasim.core.components.network_calibration;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

public class NetworkCalibrationConfigGroup extends ReflectiveConfigGroup {
    static public final String GROUP_NAME = "eqasim:networkCalibration";
    static private final String ACTIVATE = "activate";
    static private final String UPDATE_INTERVAL = "updateInterval";
    static private final String SAVE_NETWORK_INTERVAL = "saveNetworkInterval";
    static private final String CORRECT_CAPACITIES = "correctCapacities";
    static private final String MIN_SPEED = "minSpeed";
    static private final String MAX_CAPACITY = "maxCapacity";

    static private final String CAT1FLOW = "cat1Flow";
    static private final String CAT2FLOW = "cat2Flow";
    static private final String CAT3FLOW = "cat3Flow";
    static private final String CAT4FLOW = "cat4Flow";
    static private final String CAT5FLOW = "cat5Flow";

    private boolean activate = false;
    private double cat1Flow = 380.0;
    private double cat2Flow = 300.0;
    private double cat3Flow = 200.0;
    private double cat4Flow = 100.0;
    private double cat5Flow = 30.0;
    private int updateInterval = 5;
    private int saveNetworkInterval = 5;

    private boolean correctCapacities = true;
    private double minSpeed = 3.0; // km/h
    private double maxCapacity = 1800.0; // veh/h/lane (for the highest category, used to scale all capacities)

    public NetworkCalibrationConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public final Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(ACTIVATE, "Whether to activate the module or not or not (default: false)");
        map.put(UPDATE_INTERVAL, "Interval (in iterations) at which the capacities are updated (default: 5)");
        map.put(SAVE_NETWORK_INTERVAL, "Interval (in iterations) at which the network is saved (default: 5)");
        map.put(CORRECT_CAPACITIES, "Whether to correct capacities for short links (default: true)");
        map.put(MIN_SPEED, "Minimum speed (in km/h) used in capacity correction (default: 3.0 km/h)");
        map.put(MAX_CAPACITY, "Maximum capacity (in veh/h/lane) used to scale all capacities (default: 1800 veh/h/lane)");
        map.put(CAT1FLOW, "Target flow for link category 1 (default: 800 veh/h/lane)");
        map.put(CAT2FLOW, "Target flow for link category 2 (default: 600 veh/h/lane)");
        map.put(CAT3FLOW, "Target flow for link category 3 (default: 400 veh/h/lane)");
        map.put(CAT4FLOW, "Target flow for link category 4 (default: 200 veh/h/lane)");
        map.put(CAT5FLOW, "Target flow for link category 5 (default: 100 veh/h/lane)");
        return map;
    }

    @StringGetter(ACTIVATE)
    public boolean isActivated() {
        return activate;
    }
    @StringSetter(ACTIVATE)
    public void setActivate(boolean inputActivate) {
        activate = inputActivate;
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

    @StringGetter(CAT1FLOW)
    public double getCat1Flow() {
        return cat1Flow;
    }
    @StringSetter(CAT1FLOW)
    public void setCat1Flow(double inputCat1Flow) {
        cat1Flow = inputCat1Flow;
    }
    @StringGetter(CAT2FLOW)
    public double getCat2Flow() {
        return cat2Flow;
    }
    @StringSetter(CAT2FLOW)
    public void setCat2Flow(double inputCat2Flow) {
        cat2Flow = inputCat2Flow;
    }
    @StringGetter(CAT3FLOW)
    public double getCat3Flow() {
        return cat3Flow;
    }
    @StringSetter(CAT3FLOW)
    public void setCat3Flow(double inputCat3Flow) {
        cat3Flow = inputCat3Flow;
    }
    @StringGetter(CAT4FLOW)
    public double getCat4Flow() {
        return cat4Flow;
    }
    @StringSetter(CAT4FLOW)
    public void setCat4Flow(double inputCat4Flow) {
        cat4Flow = inputCat4Flow;
    }
    @StringGetter(CAT5FLOW)
    public double getCat5Flow() {
        return cat5Flow;
    }
    @StringSetter(CAT5FLOW)
    public void setCat5Flow(double inputCat5Flow) {
        cat5Flow = inputCat5Flow;
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
