package org.eqasim.examples.zurich_parking.analysis.parking;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.ActivityFacility;

public class ParkingOccupancyStats {
    public double time;
    public Id<Link> linkId;
    public Coord fromCoord;
    public Coord toCoord;
    public Id<ActivityFacility> parkingFacilityId;
    public String parkingFacilityType;
    public Coord parkingFacilityCoord;
    public double occupancy;
    public double capacity;

    public ParkingOccupancyStats(double time, Id<Link> linkId, Coord fromCoord, Coord toCoord,
                                 Id<ActivityFacility> parkingFacilityId,
                                 String parkingFacilityType, Coord parkingFacilityCoord,
                                 double occupancy, double capacity) {
        this.time = time;
        this.linkId = linkId;
        this.parkingFacilityId = parkingFacilityId;
        this.parkingFacilityType = parkingFacilityType;
        this.fromCoord = fromCoord;
        this.toCoord = toCoord;
        this.parkingFacilityCoord = parkingFacilityCoord;
        this.occupancy = occupancy;
        this.capacity = capacity;
    }
}
