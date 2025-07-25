package org.eqasim.core.components.traffic_light.delays.webster;

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.HashMap;
import java.util.Map;

public class WebsterConfigGroup extends ReflectiveConfigGroup {
    static public final String GROUP_NAME = "websterParameters";

    public static final String TOTAL_LOST_TIME = "totalLostTime"; // NOT USED
    public static final String MINIMUM_GREEN_TIME = "minimumGreenTime";
    public static final String LOST_TIME_PER_PHASE = "lostTimePerPhase";
    public static final String ALL_RED_TIME = "allRedTime";
    public static final String MAXIMUM_SATURATED_RATIO = "maximumSaturatedFlow";
    public static final String MINIMUM_FLOW_RATE = "minimumFlowRate";

    private double totalLostTime = 0.0;
    private double minimumGreenTime = 10.0;
    private double lostTimePerPhase = 3.0;
    private double allRedTime = 0.0;
    private double maximumSaturatedFlow = 0.98;
    private double minimumFlowRate = 1.0/3600;

    public WebsterConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = new HashMap<>();

        comments.put(TOTAL_LOST_TIME,
                "Is the total lost time in seconds (L in Copt formula), when it is set to 0, it will be " +
                "calculated as n*lostTimePerPhase + allRedTime, where n in the number of phases (number of groups at an intersection)");
        comments.put(MINIMUM_GREEN_TIME,
                "Is the minimum green time in seconds per phase, default value is 10.0 seconds");
        comments.put(LOST_TIME_PER_PHASE,
                "Is the lost time per phase in seconds, only used if the totalLostTime is 0. Default value is 3.0 seconds");
        comments.put(ALL_RED_TIME,
                "Is the all red time in seconds, only used if the totalLostTime is 0. Default value is 0.0 seconds");
        comments.put(MAXIMUM_SATURATED_RATIO,
                "Is the maximum saturated flow ratio, this value is used to avoid division by zero in the delay function. Default value is 0.98");
        comments.put(MINIMUM_FLOW_RATE,
                "Is the minimum flow rate in vehicles per second, this value is used to avoid division by zero in the delay function. "+
                "Default value is 1.0/3600 (1 vehicle per hour)");
        return comments;
    }

    @StringGetter(TOTAL_LOST_TIME)
    public double getTotalLostTime() {
        return totalLostTime;
    }
    @StringSetter(TOTAL_LOST_TIME)
    public void setTotalLostTime(double totalLostTime) {
        this.totalLostTime = totalLostTime;
    }

    @StringGetter(MINIMUM_GREEN_TIME)
    public double getMinimumGreenTime() {
        return minimumGreenTime;
    }
    @StringSetter(MINIMUM_GREEN_TIME)
    public void setMinimumGreenTime(double minimumGreenTime) {
        this.minimumGreenTime = minimumGreenTime;
    }

    @StringGetter(LOST_TIME_PER_PHASE)
    public double getLostTimePerPhase() {
        return lostTimePerPhase;
    }
    @StringSetter(LOST_TIME_PER_PHASE)
    public void setLostTimePerPhase(double lostTimePerPhase) {
        this.lostTimePerPhase = lostTimePerPhase;
    }

    @StringGetter(ALL_RED_TIME)
    public double getAllRedTime() {
        return allRedTime;
    }
    @StringSetter(ALL_RED_TIME)
    public void setAllRedTime(double allRedTime) {
        this.allRedTime = allRedTime;
    }

    @StringGetter(MAXIMUM_SATURATED_RATIO)
    public double getMaximumSaturatedFlow() {
        return maximumSaturatedFlow;
    }
    @StringSetter(MAXIMUM_SATURATED_RATIO)
    public void setMaximumSaturatedFlow(double maximumSaturatedFlow) {
        this.maximumSaturatedFlow = maximumSaturatedFlow;
    }

    @StringGetter(MINIMUM_FLOW_RATE)
    public double getMinimumFlowRate() {
        return minimumFlowRate;
    }
    @StringSetter(MINIMUM_FLOW_RATE)
    public void setMinimumFlowRate(double minimumFlowRate) {
        this.minimumFlowRate = minimumFlowRate;
    }
}

