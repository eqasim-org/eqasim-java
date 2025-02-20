package org.eqasim.core.simulation.modes.parking_aware_car.config;

import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;

public class ParkingAwareNetworkModeConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {

    public static final String GROUP_NAME = "eqasim:parking";

    public int parkingUsageAggregationInterval = 600;

    public ParkingAwareNetworkModeConfigGroup() {
        super(GROUP_NAME);
    }
}
