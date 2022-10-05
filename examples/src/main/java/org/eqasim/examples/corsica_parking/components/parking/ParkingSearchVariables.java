package org.eqasim.examples.corsica_parking.components.parking;

import org.matsim.api.core.v01.Coord;

public class ParkingSearchVariables {
    private Coord destinationCoord;
    private double arrivalTime;
    private double parkingSearchDuration;

    public void setDestinationCoord(Coord destinationCoord) {
        this.destinationCoord = destinationCoord;
    }

    public void setArrivalTime(double arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public void setParkingSearchDuration(double parkingSearchDuration) {
        this.parkingSearchDuration = parkingSearchDuration;
    }

    public Coord getDestinationCoord() {
        return destinationCoord;
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    public double getParkingSearchDuration() {
        return parkingSearchDuration;
    }
}
