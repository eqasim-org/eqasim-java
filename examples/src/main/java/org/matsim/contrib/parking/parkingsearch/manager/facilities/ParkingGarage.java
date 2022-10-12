package org.matsim.contrib.parking.parkingsearch.manager.facilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordUtils;

public class ParkingGarage implements ParkingFacility {

    private final String parkingType = ParkingFacilityType.Garage.toString();

    private final Coord coordReference = new Coord(0, 0);
    private final Coord coordParking;

    private final double hourlyRateBase = 5.28;
    private final double hourlyRateDistanceCoefficient = -0.82;

    private final double maxParkingDuration = Double.MAX_VALUE;
    private final boolean isAllowedToPark = true;

    public ParkingGarage(Coord coordParking) {
        this.coordParking = coordParking;
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
        double parkingDuration = endTime - startTime;
        double distanceToHB = CoordUtils.calcEuclideanDistance(coordParking, coordReference) / 1e3;
        return (hourlyRateBase + hourlyRateDistanceCoefficient * distanceToHB) * parkingDuration;
    }

    @Override
    public boolean isAllowedToPark(double startTime, double endTime, Id<Person> personId) {
        return isAllowedToPark;
    }

    @Override
    public Coord getCoord() {
        return coordParking;
    }
}
