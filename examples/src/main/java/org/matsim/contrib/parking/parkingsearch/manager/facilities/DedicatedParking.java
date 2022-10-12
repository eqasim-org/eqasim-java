package org.matsim.contrib.parking.parkingsearch.manager.facilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.util.LinkedList;
import java.util.List;

public class DedicatedParking implements ParkingFacility {

    private final String parkingType = ParkingFacilityType.DedicatedParking.toString();
    private final double maxParkingDuration = Double.MAX_VALUE;
    private final Coord coordParking;
    private List<Id<Person>> allowedPersons;

    public DedicatedParking(Coord coordParking, List<Id<Person>> allowedPersons) {
        this.coordParking = coordParking;
        this.allowedPersons = allowedPersons;
    }

    public DedicatedParking(Coord coordParking) {
        this(coordParking, new LinkedList<>());
    }

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

    @Override
    public Coord getCoord() {
        return coordParking;
    }

}
