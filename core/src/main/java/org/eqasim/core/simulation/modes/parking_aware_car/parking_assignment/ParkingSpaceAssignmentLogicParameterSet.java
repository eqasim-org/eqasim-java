package org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment;

import jakarta.validation.constraints.NotNull;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.attribute_based.PersonAttributeBasedParkingAssignmentConfigGroup;
import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.ConfigGroup;

public class ParkingSpaceAssignmentLogicParameterSet extends ReflectiveConfigGroupWithConfigurableParameterSets {

    public static final String GROUP_NAME = "parkingAssignment";

    public interface ParkingAssignmentLogicParams {

    }

    @NotNull
    private ParkingAssignmentLogicParams parkingAssignmentLogicParams;

    public ParkingSpaceAssignmentLogicParameterSet() {
        super(GROUP_NAME);
        addDefinition(PersonAttributeBasedParkingAssignmentConfigGroup.GROUP_NAME,
                PersonAttributeBasedParkingAssignmentConfigGroup::new,
                () -> (ConfigGroup) parkingAssignmentLogicParams,
                params -> this.parkingAssignmentLogicParams = (ParkingAssignmentLogicParams) params);
    }

    public static ParkingSpaceAssignmentLogicParameterSet buildDefault() {
        ParkingSpaceAssignmentLogicParameterSet params = new ParkingSpaceAssignmentLogicParameterSet();
        params.addParameterSet(new PersonAttributeBasedParkingAssignmentConfigGroup());
        return params;
    }

    public ParkingAssignmentLogicParams getParkingAssignmentLogicParams() {
        return parkingAssignmentLogicParams;
    }
}
