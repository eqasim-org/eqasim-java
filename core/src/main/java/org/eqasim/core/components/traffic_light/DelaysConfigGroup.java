package org.eqasim.core.components.traffic_light;

import org.eqasim.core.components.traffic_light.delays.shahpar.ShahparConfigGroup;
import org.eqasim.core.components.traffic_light.delays.webster.WebsterConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;


public class DelaysConfigGroup extends ReflectiveConfigGroup {
    static public final String GROUP_NAME = "eqasim:intersectionDelays";

    // Activation of the module
    static private final String ACTIVATE = "activate";
    static private final String ACTIVATE_TL_DELAYS = "activateTl";
    static private final String ACTIVATE_UNSIGNALIZED_DELAYS = "activateUnsignalized";

    // starting times of the crossing delays
    static private final String START_TIME = "startTime";
    static private final String END_TIME = "endTime";
    static private final String TL_START_TIME = "tlStartTime";
    static private final String TL_END_TIME = "tlEndTime";

    // parameters for the flow updater
    static private final String BIN_SIZE = "binSize";
    static private final String BETA = "beta";
    static private final String WRITE_FLOW_INTERVAL = "writeFlowInterval";

    // parameters for the traffic light delays
    static private final String WRITE_DELAY_INTERVAL = "writeDelayInterval";
    static private final String STARTING_ITERATION = "startingIteration";
    static private final String MINIMUM_DISTANCE_BETWEEN_DELAYS = "minimumDistanceBetweenDelays";

    private double startTime = 0.0 * 3600.0;
    private double endTime = 24.0 * 3600.0;
    private double tlStartTime = 5.0 * 3600.0;
    private double tlEndTime = 23.0 * 3600.0;

    private double binSize = 3600.0;
    private double beta = 0.5;
    private int writeFlowInterval = 1;
    private int writeDelayInterval = 1;
    private boolean activateTl = false;
    private boolean activate = false;
    private boolean activateUnsignalized = false;
    private int startingIteration = 3;
    private double minimumDistanceBetweenDelays = 30.0; // meters

    public DelaysConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public final Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(ACTIVATE, "Whether to activate the module or not or not (default: false)");
        map.put(ACTIVATE_TL_DELAYS, "Whether to activate traffic light delays or not (default: false)");
        map.put(ACTIVATE_UNSIGNALIZED_DELAYS, "Whether to activate unsignalized intersection delays or not (default: false)");
        map.put(START_TIME, "Starting time of applying crossing delays, therefore of computing the flow (default: 0.0 * 3600.0). " +
                            "Out of these bounds, the default crossing penalty is applied. It can be set to 0 if needed in eqasim:crossingPenalty");
        map.put(END_TIME, "Ending time of applying crossing delays (default: 24.0 * 3600.0)");
        map.put(TL_START_TIME, "Starting time of traffic light crossing delays (default: 5.0 * 3600.0)");
        map.put(TL_END_TIME, "Ending time of traffic light crossing delays (default: 23.0 * 3600.0)");

        map.put(BIN_SIZE, "Size of the time bins in seconds (default: 3600.0, other values are not tested)");
        map.put(BETA, "Beta parameter for the flow updater (default: 0.5)" +
                " (0.0 means no flow update, 1.0 means full flow update)");
        map.put(WRITE_FLOW_INTERVAL, "Write flow interval in iterations (default: 1)");
        map.put(WRITE_DELAY_INTERVAL, "Write traffic light delays interval in iterations (default: 1)");
        map.put(STARTING_ITERATION, "Iteration from which the module starts to apply crossing penalties (default: 3)");
        return map;
    }

    @StringGetter(STARTING_ITERATION)
    public int getStartingIteration() {
        return startingIteration;
    }
    @StringSetter(STARTING_ITERATION)
    public void setStartingIteration(int inputStartingIteration) {
        startingIteration = inputStartingIteration;
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

    @StringGetter(TL_START_TIME)
    public double getTlStartTime() {
        return tlStartTime;
    }

    @StringSetter(TL_START_TIME)
    public void setTlStartTime(double inputStartTime) {
        tlStartTime = inputStartTime;
    }

    @StringGetter(TL_END_TIME)
    public double getTlEndTime() {
        return tlEndTime;
    }

    @StringSetter(TL_END_TIME)
    public void setTlEndTime(double inputEndTime) {
        tlEndTime = inputEndTime;
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

    @StringGetter(ACTIVATE_TL_DELAYS)
    public boolean isTlActivated() {
        return activateTl;
    }

    @StringSetter(ACTIVATE_TL_DELAYS)
    public void setActivateTl(boolean inputActivateTl) {
        activateTl = inputActivateTl;
    }

    @StringGetter(ACTIVATE_UNSIGNALIZED_DELAYS)
    public boolean isUnsignalizedActivated() {
        return activateUnsignalized;
    }
    @StringSetter(ACTIVATE_UNSIGNALIZED_DELAYS)
    public void setActivateUnsignalized(boolean inputActivateUnsignalized) {
        activateUnsignalized = inputActivateUnsignalized;
    }

    @StringGetter(ACTIVATE)
    public boolean isActivated() {
        return activate;
    }

    @StringSetter(ACTIVATE)
    public void setActivate(boolean inputActivate) {
        activate = inputActivate;
    }

    @StringGetter(MINIMUM_DISTANCE_BETWEEN_DELAYS)
    public double getMinimumDistanceBetweenDelays() {
        return minimumDistanceBetweenDelays;
    }
    @StringSetter(MINIMUM_DISTANCE_BETWEEN_DELAYS)
    public void setMinimumDistanceBetweenDelays(double inputMinimumDistanceBetweenDelays) {
        minimumDistanceBetweenDelays = inputMinimumDistanceBetweenDelays;
    }



    // here I include the WebsterConfigGroup as a submodule
    @Override
    public ConfigGroup createParameterSet(String type) {
        if (type.equals(WebsterConfigGroup.GROUP_NAME)) {
            return new WebsterConfigGroup();
        }
        if (type.equals(ShahparConfigGroup.GROUP_NAME)) {
            return new ShahparConfigGroup();
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

    public ShahparConfigGroup getShahparConfigGroup() {
        for (ConfigGroup group : getParameterSets(ShahparConfigGroup.GROUP_NAME)) {
            return (ShahparConfigGroup) group;
        }
        ShahparConfigGroup config = new ShahparConfigGroup();
        addParameterSet(config);
        return config;
    }

    public static DelaysConfigGroup getOrCreate(Config config) {
        DelaysConfigGroup group = (DelaysConfigGroup) config.getModules().get(GROUP_NAME);

        if (group == null) {
            group = new DelaysConfigGroup();
            config.addModule(group);
        }

        return group;
    }
}
