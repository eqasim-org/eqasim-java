package org.matsim.contrib.parking.parkingsearch.manager.facilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.facilities.*;

public class BlueZoneParking extends ActivityFacilityImpl implements ParkingFacility {

    private final Id<ParkingFacility> parkingFacilityId;
    private final String parkingType = ParkingFacilityType.BlueZone.toString();
    private final double maxParkingDuration = 3600;

    public BlueZoneParking(Id<ActivityFacility> id, Coord coordParking, Id<Link> linkId, double capacity) {
        super(id, coordParking, linkId);
        this.parkingFacilityId = Id.create(id, ParkingFacility.class);

        // add parking activity option with capacity
        ActivityFacilitiesFactory facilitiesFactory = new ActivityFacilitiesFactoryImpl();
        ActivityOption activityOption = facilitiesFactory.createActivityOption(ParkingUtils.PARKACTIVITYTYPE);
        activityOption.setCapacity(capacity);
        this.addActivityOption(activityOption);
    }

    @Override
    public Id<ParkingFacility> getParkingFacilityId() {
        return parkingFacilityId;
    }

    @Override
    public String getParkingType() {
        return parkingType;
    }

    @Override
    public double getMaxParkingDuration() {
        return maxParkingDuration;
    }

    // free to park in blue zones
    @Override
    public double getParkingCost(double startTime, double endTime) {
        return 0;
    }

    // blue zones have specific time restrictions (except for home trips, where we assume people have a permit)
    // TODO: assign people parking permits for blue zones
    @Override
    public boolean isAllowedToPark(double startTime, double endTime, Id<Person> personId, String purpose) {

        if (purpose.equals("home")) {
            return true;
        } else {
            // round up start time to nearest half-hour
            double roundTimeWindow = 30 * 60.0;
            startTime = Math.ceil(startTime / roundTimeWindow) * roundTimeWindow;

            // max time limit by default is one hour
            double mustLeaveByTime = startTime + maxParkingDuration;

            // however, there are exceptions

            // early morning case, i.e., before 8:00
            if (startTime < 8 * 3600.0) {
                mustLeaveByTime = 9 * 3600.0; // must leave by 9:00
            }
            // afternoon case, i.e., between 11:30 and 13:30
            else if (startTime >= 11.5 * 3600.0 & startTime < 13.5 * 3600.0) {
                mustLeaveByTime = 14.5 * 3600.0; // must leave by 14:30
            }
            // evening case, i.e., after 18:00
            else if (startTime >= 18 * 3600.0) {
                mustLeaveByTime = (9 + 24) * 3600.0; // must leave by 9:00 the next day
            }

            return !(endTime > mustLeaveByTime);
        }
    }

}
