package org.matsim.contrib.parking.parkingsearch.manager.facilities;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;

public interface ParkingFacility extends ActivityFacility{

    Id<ParkingFacility> getParkingFacilityId();

    String getParkingType();

    double getMaxParkingDuration();

    double getParkingCost(double startTime, double endTime);

    boolean isAllowedToPark(double startTime, double endTime, Id<Person> personId, String purpose);

}
