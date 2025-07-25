package org.eqasim.core.components.traffic_light;

import org.eqasim.core.components.traffic_light.delays.webster.WebsterConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;


public class TrafficLightConfigGroup extends ReflectiveConfigGroup {
    static public final String GROUP_NAME = "eqasim:tl";

    static public final String ACTIVATE_TL = "activateTl";
    static private final String START_TIME = "startTime";
    static private final String END_TIME = "endTime";
    static private final String BIN_SIZE = "binSize";
    static private final String BETA = "beta";
    static private final String WRITE_FLOW_INTERVAL = "writeFlowInterval";
    static private final String WRITE_DELAY_INTERVAL = "writeDelayInterval";
    static private final String TL_STARTING_ITERATION = "tlStartingIteration";

    private double startTime = 0.0 * 3600.0;
    private double endTime = 24.0 * 3600.0;
    private double binSize = 3600.0;
    private double beta = 0.5;
    private int writeFlowInterval = 1;
    private int writeDelayInterval = 1;
    private boolean activateTl = false;
    private int tlStartingIteration = 3;

    public TrafficLightConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public final Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(ACTIVATE_TL, "Whether to activate traffic light crossing penalties or not (default: false)");
        map.put(START_TIME, "Starting time of the flow calculation in seconds (default: 0.0 * 3600.0)");
        map.put(END_TIME, "Ending time of the flow calculation in seconds (default: 24.0 * 3600.0)");
        map.put(BIN_SIZE, "Size of the time bins in seconds (default: 3600.0, other values are not tested)");
        map.put(BETA, "Beta parameter for the flow updater (default: 0.5)" +
                " (0.0 means no flow update, 1.0 means full flow update)");
        map.put(WRITE_FLOW_INTERVAL, "Write flow interval in iterations (default: 1)");
        map.put(WRITE_DELAY_INTERVAL, "Write traffic light delays interval in iterations (default: 1)");
        map.put(TL_STARTING_ITERATION, "Iteration from which the traffic light module starts to run (default: 3)");
        return map;
    }

    @StringGetter(TL_STARTING_ITERATION)
    public int getTlStartingIteration() {
        return tlStartingIteration;
    }
    @StringSetter(TL_STARTING_ITERATION)
    public void setTlStartingIteration(int inputTlStartingIteration) {
        tlStartingIteration = inputTlStartingIteration;
    }

    @StringGetter(START_TIME)
    public double getStartTime() {
        return startTime;
    }

    @StringSetter(START_TIME)
    public void setStartTime(double inputStartTime) {
        startTime = inputStartTime;
    }

    @StringGetter(END_TIME)
    public double getEndTime() {
        return endTime;
    }

    @StringSetter(END_TIME)
    public void setEndTime(double inputEndTime) {
        endTime = inputEndTime;
    }

    @StringGetter(BIN_SIZE)
    public double getBinSize() {
        return binSize;
    }

    @StringSetter(BIN_SIZE)
    public void setBinSize(double inputBinSize) {
        binSize = inputBinSize;
    }

    @StringGetter(BETA)
    public double getBeta() {
        return beta;
    }

    @StringSetter(BETA)
    public void setBeta(double inputBeta) {
        beta = inputBeta;
    }

    @StringGetter(WRITE_FLOW_INTERVAL)
    public int getWriteFlowInterval() {
        return writeFlowInterval;
    }

    @StringSetter(WRITE_FLOW_INTERVAL)
    public void setWriteFlowInterval(int inputWriteFlowInterval) {
        writeFlowInterval = inputWriteFlowInterval;
    }

    @StringGetter(WRITE_DELAY_INTERVAL)
    public int getWriteDelayInterval() {
        return writeDelayInterval;
    }

    @StringSetter(WRITE_DELAY_INTERVAL)
    public void setWriteDelayInterval(int inputWriteDelayInterval) {
        writeDelayInterval = inputWriteDelayInterval;
    }

    @StringGetter(ACTIVATE_TL)
    public boolean isActivated() {
        return activateTl;
    }

    @StringSetter(ACTIVATE_TL)
    public void setActivateTl(boolean inputActivateTl) {
        activateTl = inputActivateTl;
    }

    // here I include the WebsterConfigGroup as a submodule
    @Override
    public ConfigGroup createParameterSet(String type) {
        if (type.equals(WebsterConfigGroup.GROUP_NAME)) {
            return new WebsterConfigGroup();
        }
        throw new IllegalArgumentException("Unknown parameter set type: " + type);
    }

    public WebsterConfigGroup getWebsterConfigGroup() {
        for (ConfigGroup group : getParameterSets(WebsterConfigGroup.GROUP_NAME)) {
            return (WebsterConfigGroup) group;
        }
        WebsterConfigGroup config = new WebsterConfigGroup();
        addParameterSet(config);
        return config;
    }

    public static TrafficLightConfigGroup getOrCreate(Config config) {
        TrafficLightConfigGroup group = (TrafficLightConfigGroup) config.getModules().get(GROUP_NAME);

        if (group == null) {
            group = new TrafficLightConfigGroup();
            config.addModule(group);
        }

        return group;
    }
}
