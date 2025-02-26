package org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.attribute_based;

import com.google.common.base.Verify;
import jakarta.validation.constraints.NotEmpty;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingType;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogicParameterSet;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PersonAttributeBasedParkingAssignmentConfigGroup extends ReflectiveConfigGroup implements ParkingSpaceAssignmentLogicParameterSet.ParkingAssignmentLogicParams {

    public static final String GROUP_NAME = "personAttributeBasedParkingAssignment";

    @Parameter
    @NotEmpty
    public List<String> orderedParkingTypes;

    @Parameter
    public Set<String> parkingTypesAvailableForEveryone = new HashSet<>();

    public PersonAttributeBasedParkingAssignmentConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    protected void checkConsistency(Config config) {
        Verify.verify(!orderedParkingTypes.isEmpty(), "orderedParkingTypes cannot be empty");
    }

    public List<Id<ParkingType>> getOrderedParkingTypes() {
        return orderedParkingTypes.stream().map(parkingTypeIdString -> Id.create(parkingTypeIdString, ParkingType.class)).toList();
    }

    public IdSet<ParkingType> getParkingTypesAvailableForEveryone() {
        IdSet<ParkingType> result = new IdSet<>(ParkingType.class);
        this.parkingTypesAvailableForEveryone.stream().map(parkingTypeIdString -> Id.create(parkingTypeIdString, ParkingType.class)).forEach(result::add);
        return result;
    }
}
