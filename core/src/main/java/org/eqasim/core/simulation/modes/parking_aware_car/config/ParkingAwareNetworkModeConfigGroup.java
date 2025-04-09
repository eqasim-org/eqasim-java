package org.eqasim.core.simulation.modes.parking_aware_car.config;

import jakarta.validation.constraints.NotNull;
import org.eqasim.core.simulation.modes.parking_aware_car.mode_choice.ParkingAwareNetworkModeChoiceConfigGroup;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogicParameterSet;
import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;


public class ParkingAwareNetworkModeConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {

    public static final String GROUP_NAME = "eqasim:parking";

    @Parameter
    public int parkingUsageAggregationInterval = 600;

    @Parameter
    @NotNull
    public String mode = "car";

    @Parameter
    @Comment("If set to the path to a geopackage file, the parking search within a radius will be allowed only when the trip's destination is in the given area. In outside activities, the parking will be forced into the destination's link")
    public String parkingSearchRestrictionArea = null;

    @NotNull
    private ParkingSpaceAssignmentLogicParameterSet parkingSpaceAssignmentLogicParameterSet;

    @NotNull
    private ParkingAwareNetworkModeChoiceConfigGroup parkingAwareNetworkModeChoiceConfigGroup;

    public ParkingAwareNetworkModeConfigGroup() {
        super(GROUP_NAME);
        addDefinition(ParkingSpaceAssignmentLogicParameterSet.GROUP_NAME,
                ParkingSpaceAssignmentLogicParameterSet::new,
                () -> parkingSpaceAssignmentLogicParameterSet,
                configGroup -> this.parkingSpaceAssignmentLogicParameterSet = (ParkingSpaceAssignmentLogicParameterSet) configGroup);
        addDefinition(ParkingAwareNetworkModeChoiceConfigGroup.GROUP_NAME,
                ParkingAwareNetworkModeChoiceConfigGroup::new,
                () -> parkingAwareNetworkModeChoiceConfigGroup,
                configGroup -> this.parkingAwareNetworkModeChoiceConfigGroup = (ParkingAwareNetworkModeChoiceConfigGroup) configGroup);
    }

    public static ParkingAwareNetworkModeConfigGroup buildDefault() {
        ParkingAwareNetworkModeConfigGroup configGroup = new ParkingAwareNetworkModeConfigGroup();
        configGroup.addParameterSet(ParkingSpaceAssignmentLogicParameterSet.buildDefault());
        return configGroup;
    }

    public ParkingSpaceAssignmentLogicParameterSet.ParkingAssignmentLogicParams getParkingSpaceAssignmentLogicParams() {
        return this.parkingSpaceAssignmentLogicParameterSet.getParkingAssignmentLogicParams();
    }

    public ParkingAwareNetworkModeChoiceConfigGroup getParkingAwareNetworkModeChoiceConfigGroup() {
        return this.parkingAwareNetworkModeChoiceConfigGroup;
    }
}
