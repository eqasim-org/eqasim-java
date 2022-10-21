package org.eqasim.examples.zurich_parking.parking.manager.facilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.manager.facilities.ParkingFacility;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.*;

public class ZurichParkingGarage extends ActivityFacilityImpl implements ParkingFacility {

    private final Id<ParkingFacility> parkingFacilityId;
    private final String parkingType = ParkingFacilityType.Garage.toString();
    private final double maxParkingDuration;

    private final Coord coordReference = new Coord(2683265.987, 1247929.869);
    private final double hourlyRateBase = 5.28;
    private final double hourlyRateDistanceCoefficient = -0.82;

    public ZurichParkingGarage(Id<ActivityFacility> id, Coord coordParking, Id<Link> linkId, double capacity, double maxParkingDuration) {
        super(id, coordParking, linkId);
        this.parkingFacilityId = Id.create(id, ParkingFacility.class);
        this.maxParkingDuration = maxParkingDuration;

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
        double distanceToReferenceCoordKm = CoordUtils.calcEuclideanDistance(this.getCoord(), coordReference) / 1e3;
        return (hourlyRateBase + hourlyRateDistanceCoefficient * distanceToReferenceCoordKm) * parkingDuration_hour;
    }

    @Override
    public boolean isAllowedToPark(double startTime, double endTime, Id<Person> personId) {
        return true;
    }

}
