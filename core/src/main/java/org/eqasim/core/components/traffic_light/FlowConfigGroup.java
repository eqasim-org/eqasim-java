package org.eqasim.core.components.flow;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

public class FlowConfigGroup extends ReflectiveConfigGroup {
    static public final String GROUP_NAME = "eqasim:flow";

    static private final String START_TIME = "startTime";
    static private final String END_TIME = "endTime";
    static private final String BIN_SIZE = "binSize";
    static private final String BETA = "beta";
    static private final String WRITE_FLOW_INTERVAL = "writeFlowInterval";
    static public final String COMPUTE_FOW = "computerFlow";

    private double startTime = 0.0 * 3600.0;
    private double endTime = 24.0 * 3600.0;
    private double binSize = 3600.0;
    private double beta = 0.9;
    private int writeFlowInterval = 1;
    private boolean computerFlow=false;

    public FlowConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public final Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(START_TIME, "Starting time of the flow calculation in seconds (default: 0.0 * 3600.0)");
        map.put(END_TIME, "Ending time of the flow calculation in seconds (default: 24.0 * 3600.0)");
        map.put(BIN_SIZE, "Size of the time bins in seconds (default: 3600.0)");
        map.put(BETA, "Beta parameter for the flow updater (default: 0.9)"+
                " (0.0 means no flow update, 1.0 means full flow update)");
        map.put(WRITE_FLOW_INTERVAL, "Write flow interval in iterations (default: 1)");
        map.put(COMPUTE_FOW, "Whether to compute the flow or not (default: false)");
        return map;
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

    @StringGetter(COMPUTE_FOW)
    public boolean isComputerFlow() {
        return computerFlow;
    }
    @StringSetter(COMPUTE_FOW)
    public void setComputerFlow(boolean inputComputerFlow) {
        computerFlow = inputComputerFlow;
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
