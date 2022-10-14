package org.matsim.contrib.parking.parkingsearch.manager.facilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.*;

public class ParkingGarage extends ActivityFacilityImpl implements ParkingFacility {

    private final Id<ParkingFacility> parkingFacilityId;
    private final String parkingType = ParkingFacilityType.Garage.toString();

    private final Coord coordReference = new Coord(1229608.0, 6179070.0);

//    private final double hourlyRateBase = 5.28;
//    private final double hourlyRateDistanceCoefficient = -0.82;

    private final double hourlyRateBase = 3.0;
    private final double hourlyRateDistanceCoefficient = 0.0;

    private final double maxParkingDuration = Double.MAX_VALUE;
    private final boolean isAllowedToPark = true;

    public ParkingGarage(Id<ActivityFacility> id, Coord coordParking, Id<Link> linkId, double capacity) {
        super(id, coordParking, linkId);
        this.parkingFacilityId = Id.create(id, ParkingFacility.class);

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
        double parkingDuration_hour = (endTime - startTime) / 3600.0;
//        double distanceToReferenceCoordKm = CoordUtils.calcEuclideanDistance(this.getCoord(), coordReference) / 1e3;
//        return (hourlyRateBase + hourlyRateDistanceCoefficient * distanceToReferenceCoordKm) * parkingDuration_hour;
        return Math.max(hourlyRateBase * parkingDuration_hour, 0.0);
    }

    @Override
    public boolean isAllowedToPark(double startTime, double endTime, Id<Person> personId) {
        return isAllowedToPark;
    }

}
