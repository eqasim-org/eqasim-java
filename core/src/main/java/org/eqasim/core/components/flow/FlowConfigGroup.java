package org.eqasim.core.components.flow;

import org.eqasim.core.components.traffic_light.DelaysConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

public class FlowConfigGroup extends ReflectiveConfigGroup {
    static public final String GROUP_NAME = "eqasim:flow";

    static private final String ACTIVATE = "activate";
    static private final String START_TIME = "startTime";
    static private final String END_TIME = "endTime";

    static private final String BIN_SIZE = "binSize";
    static private final String BETA = "beta";
    static private final String WRITE_FLOW_INTERVAL = "writeFlowInterval";


    private double startTime = 0.0 * 3600.0;
    private double endTime = 24.0 * 3600.0;
    private double binSize = 3600.0;
    private double beta = 0.5;
    private int writeFlowInterval = 1;
    private boolean activate = true;

    public FlowConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public final Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(ACTIVATE, "Whether to activate the module or not (default: true)");
        map.put(START_TIME, "Starting time of counting the traffic, therefore of computing the flow (default: 0.0 * 3600.0). ");
        map.put(END_TIME, "Ending time of counting traffic (default: 24.0 * 3600.0)");
        map.put(BIN_SIZE, "Size of the time bins in seconds (default: 1800.0, other values are not tested)");
        map.put(BETA, "Beta parameter for the flow updater (default: 0.5)" +
                " (1.0 means no flow update, 0.0 means full flow update)");
        map.put(WRITE_FLOW_INTERVAL, "Write flow interval in iterations (default: 1)");
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

    public static FlowConfigGroup getOrCreate(Config config) {
        FlowConfigGroup group = (FlowConfigGroup) config.getModules().get(GROUP_NAME);

        if (group == null) {
            group = new FlowConfigGroup();
            config.addModule(group);
        }

        return group;
    }

}
