package org.eqasim.examples.zurich_parking.analysis.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;

public class ParkingOccupancyStats {
    public double time;
    public Id<ActivityFacility> parkingFacilityId;
    public String parkingFacilityType;
    public double occupancy;
    public double capacity;

    public ParkingOccupancyStats(double time, Id<ActivityFacility> parkingFacilityId, String parkingFacilityType,
                                 double occupancy, double capacity) {
        this.time = time;
        this.parkingFacilityId = parkingFacilityId;
        this.parkingFacilityType = parkingFacilityType;
        this.occupancy = occupancy;
        this.capacity = capacity;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public void setParkingFacilityId(Id<ActivityFacility> parkingFacilityId) {
        this.parkingFacilityId = parkingFacilityId;
    }

    public void setParkingFacilityType(String parkingFacilityType) {
        this.parkingFacilityType = parkingFacilityType;
    }

    public void setOccupancy(double occupancy) {
        this.occupancy = occupancy;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public double getTime() {
        return time;
    }

    public Id<ActivityFacility> getParkingFacilityId() {
        return parkingFacilityId;
    }

    public String getParkingFacilityType() { return parkingFacilityType; }

    public double getOccupancy() {
        return occupancy;
    }

    public double getCapacity() {
        return capacity;
    }

    @Override
    public String toString() {
        return "ParkingOccupancyStats{" +
                "time=" + time +
                ", parkingFacilityId=" + parkingFacilityId.toString() +
                ", occupancy=" + occupancy +
                ", capacity=" + capacity +
                '}';
    }
}
