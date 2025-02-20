package org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment;

import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingType;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.util.Map;

public class DestinationActivityTypeDependentParkingAssignment implements ParkingSpaceAssignmentLogic{

    private final Map<String, ParkingType> parkingTypePerActivityTypeMap;

    public DestinationActivityTypeDependentParkingAssignment(Map<String, ParkingType> parkingTypePerActivityTypeMap) {
        this.parkingTypePerActivityTypeMap = parkingTypePerActivityTypeMap;
    }


    @Override
    public ParkingSpace getUsedParkingSpace(NetworkWideParkingSpaceStore networkWideParkingSpaceStore, Person person, Id<Link> linkId) {
        return null;
    }
}
