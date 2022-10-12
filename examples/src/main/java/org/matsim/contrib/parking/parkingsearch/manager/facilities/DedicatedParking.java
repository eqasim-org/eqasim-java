package org.matsim.contrib.parking.parkingsearch.manager.facilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

import java.util.LinkedList;
import java.util.List;

public class DedicatedParking extends ActivityFacilityImpl implements ParkingFacility {

    // ParkingFacility attributes
    private final String parkingType = ParkingFacilityType.DedicatedParking.toString();
    private final double maxParkingDuration = Double.MAX_VALUE;
    private final Id<ParkingFacility> parkingFacilityId;
    private List<Id<Person>> allowedPersons;

    public DedicatedParking(Id<ActivityFacility> id, Coord coordParking, Id<Link> linkId,
                            List<Id<Person>> allowedPersons) {
        super(id, coordParking, linkId);
        this.parkingFacilityId = Id.create(id, ParkingFacility.class);
        this.allowedPersons = allowedPersons;
    }

    public DedicatedParking(Id<ActivityFacility> id, Coord coordParking, Id<Link> linkId) {
        this(id, coordParking, linkId, new LinkedList<>());
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
    public boolean isAllowedToPark(double startTime, double endTime, Id<Person> personId) {
        return allowedPersons.contains(personId);
    }

}
