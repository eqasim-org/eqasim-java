/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.parking.parkingsearch.manager;

import com.google.inject.Inject;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.manager.facilities.*;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author  ctchervenkov
 *
 */
public class MultipleParkingTypeParkingManager implements ParkingSearchManager {

    protected Map<Id<Link>, Integer> capacity = new HashMap<>();
    protected Map<Id<ActivityFacility>, MutableLong> occupation = new HashMap<>();
    protected Map<Id<ActivityFacility>, ParkingFacility> parkingFacilities;
    protected Map<Id<Vehicle>, Id<ActivityFacility>> parkingLocations = new HashMap<>();
    protected Map<Id<Vehicle>, Id<ActivityFacility>> parkingReservation = new HashMap<>();
    protected Map<Id<Vehicle>, Id<Link>> parkingLocationsOutsideFacilities = new HashMap<>();
    protected Map<Id<Link>, Set<Id<ActivityFacility>>> facilitiesPerLink = new HashMap<>();

//    private List<ParkingReservationLog> parkingReservationLog = new LinkedList<>();

    protected Network network;

    @Inject
    public MultipleParkingTypeParkingManager(Scenario scenario) {
        this.network = scenario.getNetwork();
        this.parkingFacilities = new HashMap<>();
        for (ActivityFacility facility : scenario.getActivityFacilities().getFacilitiesForActivityType(ParkingUtils.PARKACTIVITYTYPE).values()) {
            Id<ActivityFacility> parkingId = facility.getId();
            Coord parkingCoord = facility.getCoord();
            Id<Link> parkingLinkId = facility.getLinkId();
            ParkingFacilityType parkingFacilityType = ParkingFacilityType.valueOf(facility.getAttributes().getAttribute("parkingFacilityType").toString());
            double parkingCapacity = facility.getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();

            ParkingFacility parkingFacility;

            switch (parkingFacilityType) {
                case DedicatedParking:
                    Set<Id<Vehicle>> allowedVehicles = (Set<Id<Vehicle>>) facility.getAttributes().getAttribute("allowedVehicles");
                    parkingFacility = new DedicatedParking(parkingId, parkingCoord, parkingLinkId,
                            allowedVehicles, parkingCapacity);
                    break;
                case BlueZone:
                    parkingFacility = new BlueZoneParking(parkingId, parkingCoord, parkingLinkId, parkingCapacity);
                    break;
                case LowTariffWhiteZone:
                    parkingFacility = new WhiteZoneParking(parkingId, parkingCoord, parkingLinkId,
                            3600, ParkingFacilityType.LowTariffWhiteZone.toString(), parkingCapacity);
                    break;
                case HighTariffWhiteZone:
                    parkingFacility = new WhiteZoneParking(parkingId, parkingCoord, parkingLinkId,
                            3600, ParkingFacilityType.HighTariffWhiteZone.toString(), parkingCapacity);
                    break;
                case Garage:
                    parkingFacility = new ParkingGarage(parkingId, parkingCoord, parkingLinkId, parkingCapacity);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + parkingFacilityType);
            }

            this.parkingFacilities.putIfAbsent(facility.getId(), parkingFacility);
        }
        Logger.getLogger(getClass()).info(parkingFacilities.toString());

        for (ParkingFacility fac : this.parkingFacilities.values()) {
            Id<Link> linkId = fac.getLinkId();
            Set<Id<ActivityFacility>> parkingOnLink = new HashSet<>();
            if (this.facilitiesPerLink.containsKey(linkId)) {
                parkingOnLink = this.facilitiesPerLink.get(linkId);
            }
            parkingOnLink.add(fac.getId());
            this.facilitiesPerLink.put(linkId, parkingOnLink);
            this.occupation.put(fac.getId(), new MutableLong(0));

        }
    }

    @Override
    public boolean reserveSpaceAtLinkIdIfVehicleCanParkHere(Id<Link> linkId, double fromTime, double toTime, Id<Vehicle> vehicleId, String purpose) {
        // check if there is any parking on this link
        if (!this.facilitiesPerLink.containsKey(linkId)) {
            return false;
        }

        // if there are, loop through them
        Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.facilitiesPerLink.get(linkId);
        for (Id<ActivityFacility> facilityId : parkingFacilitiesAtLink) {

            ParkingFacility parkingFacility = this.parkingFacilities.get(facilityId);

            // check if the vehicle is allowed to park in this facility
            if (!parkingFacility.isAllowedToPark(fromTime, toTime, Id.createPersonId(vehicleId), purpose)) {
                continue;
            }

            // check if there is remaining capacity and reserve the first available spot encountered
            double capacity = parkingFacility.getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();
            if (this.occupation.get(facilityId).doubleValue() < capacity) {
                reserveSpaceAtFacilityId(vehicleId, facilityId);
//                this.parkingReservationLog.add(new ParkingReservationLog(fromTime, vehicleId, facilityId, "reservation"));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean reserveSpaceAtParkingFacilityIdIfVehicleCanParkHere(Id<ActivityFacility> parkingFacilityId, double fromTime, double toTime, Id<Vehicle> vehicleId, String purpose) {
        boolean canPark = parkingFacilityIdHasAvailableParkingForVehicle(parkingFacilityId, vehicleId, fromTime, toTime, purpose);
        if (canPark) {
            reserveSpaceAtFacilityId(vehicleId, parkingFacilityId);
//            this.parkingReservationLog.add(new ParkingReservationLog(fromTime, vehicleId, parkingFacilityId, "reservation"));
        }
        return canPark;
    }

    private void reserveSpaceAtFacilityId(Id<Vehicle> vehicleId, Id<ActivityFacility> facilityId){
        this.occupation.get(facilityId).increment();
        this.parkingReservation.put(vehicleId, facilityId);
    }

    private boolean parkingFacilityIdHasAvailableParkingForVehicle(Id<ActivityFacility> facilityId, Id<Vehicle> vehicleId, double startTime, double endTime, String purpose){
        // first check if the facility is actually in the list of parking facilities
        if (!this.parkingFacilities.containsKey(facilityId)){
            return false;
        }
        ParkingFacility parkingFacility = this.parkingFacilities.get(facilityId);

        // check if the vehicle is allowed to park in this facility
        if (!parkingFacility.isAllowedToPark(startTime, endTime, Id.createPersonId(vehicleId), purpose)) {
            return false;
        }

        // check if there is remaining capacity
        double capacity = parkingFacility.getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();
        return this.occupation.get(facilityId).doubleValue() < capacity;
    }

    @Override
    public Id<Link> getVehicleParkingLocationLinkId(Id<Vehicle> vehicleId) {
        if (this.parkingLocations.containsKey(vehicleId)) {
            return this.parkingFacilities.get(this.parkingLocations.get(vehicleId)).getLinkId();
        } else return this.parkingLocationsOutsideFacilities.getOrDefault(vehicleId, null);
    }

    @Override
    public Id<ActivityFacility> getVehicleParkingLocationParkingFacilityId(Id<Vehicle> vehicleId) {
        return this.parkingLocations.getOrDefault(vehicleId, null);
    }

    @Override
    public boolean parkVehicleAtLinkId(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
        Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.facilitiesPerLink.get(linkId);
        if (parkingFacilitiesAtLink == null) {
            this.parkingLocationsOutsideFacilities.put(vehicleId, linkId);
            return true;
        } else {
            Id<ActivityFacility> facilityId = this.parkingReservation.remove(vehicleId);
//            this.parkingReservationLog.add(new ParkingReservationLog(time, vehicleId, facilityId, "remove_reservation"));
            if (facilityId != null) {
                this.parkingLocations.put(vehicleId, facilityId);
                return true;
            } else {
                throw new RuntimeException("no parking reservation found for vehicle " + vehicleId.toString()
                        + ", arrival on link " + linkId + " with parking restriction");
            }
        }
    }

    @Override
    public boolean parkVehicleAtParkingFacilityId(Id<Vehicle> vehicleId, Id<ActivityFacility> parkingFacilityId, double time) {
        Id<ActivityFacility> reservedParkingFacilityId = this.parkingReservation.remove(vehicleId);
//        this.parkingReservationLog.add(new ParkingReservationLog(time, vehicleId, parkingFacilityId, "remove_reservation"));
        if (reservedParkingFacilityId != null) {
            if (parkingFacilityId.equals(reservedParkingFacilityId)) {
                this.parkingLocations.put(vehicleId, parkingFacilityId);
                return true;
            }
            else {
                throw new RuntimeException("no parking reservation found for vehicle " + vehicleId.toString()
                        + "arrival at parking facility " + parkingFacilityId + " with parking restriction");
            }
        } else {
            throw new RuntimeException("no parking reservation found for vehicle " + vehicleId.toString()
                    + "arrival at parking facility " + parkingFacilityId + " with parking restriction");
        }
    }

    @Override
    public boolean unParkVehicle(Id<Vehicle> vehicleId, double time) {
        if (!this.parkingLocations.containsKey(vehicleId)) {
            this.parkingLocationsOutsideFacilities.remove(vehicleId);
        } else {
            Id<ActivityFacility> parkingFacilityId = this.parkingLocations.remove(vehicleId);
            this.occupation.get(parkingFacilityId).decrement();
        }
        return true;
    }

    @Override
    public List<String> produceStatistics() {
        List<String> stats = new ArrayList<>();
        for (Entry<Id<ActivityFacility>, MutableLong> e : this.occupation.entrySet()) {
            Id<Link> linkId = this.parkingFacilities.get(e.getKey()).getLinkId();
            double capacity = this.parkingFacilities.get(e.getKey()).getActivityOptions()
                    .get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();
            String s = linkId.toString() + ";" + e.getKey().toString() + ";" + capacity + ";" + e.getValue().toString();
            stats.add(s);
        }
        return stats;
    }

    public double getNrOfAllParkingSpacesOnLink (Id<Link> linkId){
        double allSpaces = 0;
        Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.facilitiesPerLink.get(linkId);
        if (!(parkingFacilitiesAtLink == null)) {
            for (Id<ActivityFacility> fac : parkingFacilitiesAtLink){
                allSpaces += this.parkingFacilities.get(fac).getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();
            }
        }
        return allSpaces;
    }

    public double getNrOfFreeParkingSpacesOnLink (Id<Link> linkId){
        double allFreeSpaces = 0;
        Set<Id<ActivityFacility>> parkingFacilitiesAtLink = this.facilitiesPerLink.get(linkId);
        if (parkingFacilitiesAtLink == null) {
            return 0;
        } else {
            for (Id<ActivityFacility> fac : parkingFacilitiesAtLink){
                int cap = (int) this.parkingFacilities.get(fac).getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();
                allFreeSpaces += (cap - this.occupation.get(fac).intValue());
            }
        }
        return allFreeSpaces;
    }


    @Override
    public void reset(int iteration) {
    }


    private class ParkingReservationLog {
        private double time;
        private Id<Vehicle> vehicleId;
        private Id<ActivityFacility> activityFacilityId;
        private String action;

        public ParkingReservationLog(double time, Id<Vehicle> vehicleId, Id<ActivityFacility> activityFacilityId, String action) {
            this.time = time;
            this.vehicleId = vehicleId;
            this.activityFacilityId = activityFacilityId;
            this.action = action;
        }

    }


}
