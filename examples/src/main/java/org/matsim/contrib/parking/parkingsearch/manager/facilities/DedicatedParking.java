package org.matsim.contrib.parking.parkingsearch.manager.facilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.facilities.*;
import org.matsim.vehicles.Vehicle;

import java.util.Set;

public class DedicatedParking extends ActivityFacilityImpl implements ParkingFacility {

    // ParkingFacility attributes
    private final String parkingType = ParkingFacilityType.DedicatedParking.toString();
    private final double maxParkingDuration = Double.MAX_VALUE;
    private final Id<ParkingFacility> parkingFacilityId;
    private Set<Id<Vehicle>> allowedVehicles;

    public DedicatedParking(Id<ActivityFacility> id, Coord coordParking, Id<Link> linkId,
                            Set<Id<Vehicle>> allowedVehicles, double capacity) {
        super(id, coordParking, linkId);
        this.parkingFacilityId = Id.create(id, ParkingFacility.class);
        this.allowedVehicles = allowedVehicles;

        // add parking activity option with capacity
        ActivityFacilitiesFactory facilitiesFactory = new ActivityFacilitiesFactoryImpl();
        ActivityOption activityOption = facilitiesFactory.createActivityOption(ParkingUtils.PARKACTIVITYTYPE);
        activityOption.setCapacity(capacity);
        this.addActivityOption(activityOption);
    }

    @Override
    public Id<ParkingFacility> getParkingFacilityId() { return parkingFacilityId; }

    @Override
    public String getParkingType() {
        return parkingType;
    }

    @Override
    public double getMaxParkingDuration() {
        return maxParkingDuration;
    }

    @Override
    public double getParkingCost(double startTime, double endTime) {
        return 0;
    }

    // dedicated parking is only permitted for allowed persons
    @Override
    public boolean isAllowedToPark(double startTime, double endTime, Id<Person> personId, String purpose) {
        return allowedVehicles.contains(personId);
    }

}