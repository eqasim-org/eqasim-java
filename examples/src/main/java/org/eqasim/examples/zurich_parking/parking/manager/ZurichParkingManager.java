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

package org.eqasim.examples.zurich_parking.parking.manager;

import com.google.inject.Inject;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.log4j.Logger;
import org.eqasim.examples.zurich_parking.parking.manager.facilities.ParkingFacilityType;
import org.eqasim.examples.zurich_parking.parking.manager.facilities.ZurichBlueZoneParking;
import org.eqasim.examples.zurich_parking.parking.manager.facilities.ZurichParkingGarage;
import org.eqasim.examples.zurich_parking.parking.manager.facilities.ZurichWhiteZoneParking;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.facilities.ParkingFacility;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author  ctchervenkov
 *
 */
public class ZurichParkingManager implements ParkingSearchManager {

    // parking facility information
    protected Map<Id<ActivityFacility>, ParkingFacility> parkingFacilities = new HashMap<>();
    protected Map<ParkingFacilityType, Map<Id<ActivityFacility>, ParkingFacility>> parkingFacilitiesByType = new HashMap<>();
    protected Map<Id<Link>, Set<Id<ActivityFacility>>> facilitiesPerLink = new HashMap<>();
    protected Map<Id<Link>, Map<ParkingFacilityType, Id<ActivityFacility>>> facilitiesPerLinkByType = new HashMap<>();

    // parked vehicles information
    protected Set<ParkingFacilityType> parkingFacilityTypePriorityList = new HashSet<>();
    protected Map<Id<Vehicle>, Id<ActivityFacility>> parkingLocations = new HashMap<>();
    protected Map<Id<Vehicle>, Id<ActivityFacility>> parkingReservation = new HashMap<>();
    protected Map<Id<Vehicle>, Id<Link>> parkingLocationsOutsideFacilities = new HashMap<>();

    // occupancy-related information
    protected Map<Id<Link>, Integer> capacity = new HashMap<>();
    protected Map<Id<ActivityFacility>, MutableLong> occupation = new HashMap<>();

//    private List<ParkingReservationLog> parkingReservationLog = new LinkedList<>();

    protected Network network;

    @Inject
    public ZurichParkingManager(Scenario scenario) {
        this.network = scenario.getNetwork();

        // fill priority list
        parkingFacilityTypePriorityList.add(ParkingFacilityType.BlueZone);
        parkingFacilityTypePriorityList.add(ParkingFacilityType.LowTariffWhiteZone);
        parkingFacilityTypePriorityList.add(ParkingFacilityType.HighTariffWhiteZone);
        parkingFacilityTypePriorityList.add(ParkingFacilityType.Garage);

        for (ActivityFacility facility : scenario.getActivityFacilities().getFacilitiesForActivityType(ParkingUtils.PARKACTIVITYTYPE).values()) {
            Id<ActivityFacility> parkingId = facility.getId();
            Coord parkingCoord = facility.getCoord();
            Id<Link> parkingLinkId = facility.getLinkId();
            ParkingFacilityType parkingFacilityType = ParkingFacilityType.valueOf(facility.getAttributes().getAttribute("parkingFacilityType").toString());
            double maxParkingDuration = Double.parseDouble(facility.getAttributes().getAttribute("maxParkingDuration").toString());
            double parkingCapacity = facility.getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();

            ParkingFacility parkingFacility;

            switch (parkingFacilityType) {
                case BlueZone:
                    parkingFacility = new ZurichBlueZoneParking(parkingId, parkingCoord, parkingLinkId,
                            parkingCapacity, maxParkingDuration);
                    break;
                case LowTariffWhiteZone:
                    parkingFacility = new ZurichWhiteZoneParking(parkingId, parkingCoord, parkingLinkId,
                            maxParkingDuration, ParkingFacilityType.LowTariffWhiteZone.toString(), parkingCapacity);
                    break;
                case HighTariffWhiteZone:
                    parkingFacility = new ZurichWhiteZoneParking(parkingId, parkingCoord, parkingLinkId,
                            maxParkingDuration, ParkingFacilityType.HighTariffWhiteZone.toString(), parkingCapacity);
                    break;
                case Garage:
                    parkingFacility = new ZurichParkingGarage(parkingId, parkingCoord, parkingLinkId,
                            parkingCapacity, maxParkingDuration);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + parkingFacilityType);
            }

            // add to parkingFacilities container
            this.parkingFacilities.putIfAbsent(facility.getId(), parkingFacility);
            this.parkingFacilitiesByType.putIfAbsent(parkingFacilityType, new HashMap<>());
            this.parkingFacilitiesByType.get(parkingFacilityType).putIfAbsent(facility.getId(), parkingFacility);

            // add to facilitiesPerLink container
            this.facilitiesPerLink.putIfAbsent(parkingLinkId, new HashSet<>());
            this.facilitiesPerLink.get(parkingLinkId).add(parkingId);
            this.facilitiesPerLinkByType.putIfAbsent(parkingLinkId, new HashMap<>());
            this.facilitiesPerLinkByType.get(parkingLinkId).putIfAbsent(parkingFacilityType, parkingId);

            // fill occupancy container
            this.occupation.putIfAbsent(parkingId, new MutableLong(0));

        }
        Logger.getLogger(getClass()).info(parkingFacilitiesByType.toString());
    }

    @Override
    public boolean reserveSpaceAtLinkIdIfVehicleCanParkHere(Id<Vehicle> vehicleId, Id<Link> linkId, double fromTime, double toTime) {
        // check if there is any parking on this link
        if (!this.facilitiesPerLinkByType.containsKey(linkId)) {
            return false;
        }

        // if there are, loop through them by order of priority
        for (ParkingFacilityType parkingFacilityType : this.parkingFacilityTypePriorityList) {
            if (this.facilitiesPerLinkByType.get(linkId).containsKey(parkingFacilityType)) {

                Id<ActivityFacility> facilityId = this.facilitiesPerLinkByType.get(linkId).get(parkingFacilityType);
                ParkingFacility parkingFacility = this.parkingFacilities.get(facilityId);

                // check if the vehicle is allowed to park in this facility
                if (!parkingFacility.isAllowedToPark(fromTime, toTime, Id.createPersonId(vehicleId))) {
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
        }
        return false;
    }

    @Override
    public boolean reserveSpaceAtParkingFacilityIdIfVehicleCanParkHere(Id<Vehicle> vehicleId, Id<ActivityFacility> parkingFacilityId, double fromTime, double toTime) {
        boolean canPark = parkingFacilityIdHasAvailableParkingForVehicle(parkingFacilityId, vehicleId, fromTime, toTime);
        if (canPark) {
            reserveSpaceAtFacilityId(vehicleId, parkingFacilityId);
//            this.parkingReservationLog.add(new ParkingReservationLog(fromTime, vehicleId, parkingFacilityId, "reservation"));
        }
        return canPark;
    }

    private void reserveSpaceAtFacilityId(Id<Vehicle> vehicleId, Id<ActivityFacility> facilityId){
        if (!facilityId.toString().equals("outside")) {
            this.occupation.get(facilityId).increment();
        }
        this.parkingReservation.put(vehicleId, facilityId);
    }

    private boolean parkingFacilityIdHasAvailableParkingForVehicle(Id<ActivityFacility> facilityId, Id<Vehicle> vehicleId, double startTime, double endTime){
        // first check if the facility is actually in the list of parking facilities
        if (facilityId.toString().equals("outside")) {
            return true;
        } else if (!this.parkingFacilities.containsKey(facilityId)){
            return false;
        }
        ParkingFacility parkingFacility = this.parkingFacilities.get(facilityId);

        // check if the vehicle is allowed to park in this facility
        if (!parkingFacility.isAllowedToPark(startTime, endTime, Id.createPersonId(vehicleId))) {
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
        Id<ActivityFacility> reservedParkingFacilityId = this.parkingReservation.remove(vehicleId);
        if (reservedParkingFacilityId == null) {
            throw new RuntimeException("no parking reservation found for vehicle " + vehicleId.toString());
        }

        if (reservedParkingFacilityId.toString().equals("outside")) {
            this.parkingLocationsOutsideFacilities.put(vehicleId, linkId);
        } else {
            this.parkingLocations.put(vehicleId, reservedParkingFacilityId);
        }
        return true;
    }

    @Override
    public boolean parkVehicleAtParkingFacilityId(Id<Vehicle> vehicleId, Id<ActivityFacility> parkingFacilityId, double time) {
        throw new RuntimeException("method not implemented");
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
