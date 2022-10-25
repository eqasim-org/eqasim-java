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
import org.eqasim.core.components.config.EqasimConfigGroup;
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
import org.matsim.core.utils.collections.QuadTree;
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
    protected Map<String, Map<Id<ActivityFacility>, ParkingFacility>> parkingFacilitiesByType = new HashMap<>();
    protected Map<Id<Link>, Set<Id<ActivityFacility>>> facilitiesPerLink = new HashMap<>();
    protected Map<Id<Link>, Map<String, Id<ActivityFacility>>> facilitiesPerLinkByType = new HashMap<>();

    // parked vehicles information
    protected Set<String> parkingFacilityTypePriorityList = new HashSet<>();
    protected Map<Id<Vehicle>, List<Id<ActivityFacility>>> parkingLocations = new HashMap<>();
    protected Map<Id<Vehicle>, List<Id<ActivityFacility>>> parkingReservation = new HashMap<>();
    protected Map<Id<Vehicle>, Id<Link>> parkingLocationsOutsideFacilities = new HashMap<>();

    // occupancy information
    protected Map<Id<ActivityFacility>, Double> capacity = new HashMap<>();
    protected Map<Id<ActivityFacility>, MutableLong> occupation = new HashMap<>();
    private QuadTree<Id<ActivityFacility>> availableParkingFacilityQuadTree = null;
    private Map<String, QuadTree<Id<ActivityFacility>>> availableParkingFacilityQuadTreeByType = null;

    // other attributes
    private static final Logger log = Logger.getLogger(ZurichParkingManager.class);
    protected Scenario scenario;
    protected double sampleSize;
    protected int vehiclesPerVehicle;
    protected Network network;

//    private List<ParkingReservationLog> parkingReservationLog = new LinkedList<>();


    @Inject
    public ZurichParkingManager(Scenario scenario) {
        this.scenario = scenario;
        this.sampleSize = ((EqasimConfigGroup) this.scenario.getConfig().getModules().get(EqasimConfigGroup.GROUP_NAME)).getSampleSize();
        this.vehiclesPerVehicle = ((int) Math.floor(1.0 / this.sampleSize));
        this.network = scenario.getNetwork();

        // fill priority list
        parkingFacilityTypePriorityList.add(ParkingFacilityType.BlueZone.toString());
        parkingFacilityTypePriorityList.add(ParkingFacilityType.LowTariffWhiteZone.toString());
        parkingFacilityTypePriorityList.add(ParkingFacilityType.HighTariffWhiteZone.toString());
        parkingFacilityTypePriorityList.add(ParkingFacilityType.Garage.toString());

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
                    throw new IllegalStateException("Unexpected value: " + parkingFacilityType.toString());
            }

            // add to parkingFacilities container
            this.parkingFacilities.putIfAbsent(facility.getId(), parkingFacility);
            this.parkingFacilitiesByType.putIfAbsent(parkingFacilityType.toString(), new HashMap<>());
            this.parkingFacilitiesByType.get(parkingFacilityType.toString()).putIfAbsent(facility.getId(), parkingFacility);

            // add to facilitiesPerLink container
            this.facilitiesPerLink.putIfAbsent(parkingLinkId, new HashSet<>());
            this.facilitiesPerLink.get(parkingLinkId).add(parkingId);
            this.facilitiesPerLinkByType.putIfAbsent(parkingLinkId, new HashMap<>());
            this.facilitiesPerLinkByType.get(parkingLinkId).putIfAbsent(parkingFacilityType.toString(), parkingId);

            // fill occupancy container
            this.occupation.putIfAbsent(parkingId, new MutableLong(0));
            this.capacity.putIfAbsent(parkingId, parkingCapacity);
        }

        log.info(parkingFacilities.toString());
        log.info(facilitiesPerLink.toString());
        log.info(occupation.toString());
        log.info(capacity.toString());

        // build quad tree
        this.buildQuadTree();
    }

    synchronized private void buildQuadTree() {
        /* the method must be synchronized to ensure we only build one quadTree
         * in case that multiple threads call a method that requires the quadTree.
         */
        if (this.availableParkingFacilityQuadTree != null) {
            return;
        }

        // build general quadtree
        {
            double startTime = System.currentTimeMillis();
            double minx = Double.POSITIVE_INFINITY;
            double miny = Double.POSITIVE_INFINITY;
            double maxx = Double.NEGATIVE_INFINITY;
            double maxy = Double.NEGATIVE_INFINITY;
            for (ParkingFacility n : this.parkingFacilities.values()) {
                if (n.getCoord().getX() < minx) { minx = n.getCoord().getX(); }
                if (n.getCoord().getY() < miny) { miny = n.getCoord().getY(); }
                if (n.getCoord().getX() > maxx) { maxx = n.getCoord().getX(); }
                if (n.getCoord().getY() > maxy) { maxy = n.getCoord().getY(); }
            }
            minx -= 1.0;
            miny -= 1.0;
            maxx += 1.0;
            maxy += 1.0;
            // yy the above four lines are problematic if the coordinate values are much smaller than one. kai, oct'15

            log.info("building parking garage QuadTree for nodes: xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
            QuadTree<Id<ActivityFacility>> quadTree = new QuadTree<>(minx, miny, maxx, maxy);
            for (ParkingFacility n : this.parkingFacilities.values()) {
                quadTree.put(n.getCoord().getX(), n.getCoord().getY(), n.getId());
            }
            /* assign the quadTree at the very end, when it is complete.
             * otherwise, other threads may already start working on an incomplete quadtree
             */
            this.availableParkingFacilityQuadTree = quadTree;
            log.info("Building parking garage QuadTree took " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
            log.info(availableParkingFacilityQuadTree.toString());
        }

        // build general quadtree by parking type
        {
            this.availableParkingFacilityQuadTreeByType = new HashMap<>();

            for (String parkingFacilityType : this.parkingFacilitiesByType.keySet()) {

                double startTime = System.currentTimeMillis();
                double minx = Double.POSITIVE_INFINITY;
                double miny = Double.POSITIVE_INFINITY;
                double maxx = Double.NEGATIVE_INFINITY;
                double maxy = Double.NEGATIVE_INFINITY;
                for (ParkingFacility n : this.parkingFacilitiesByType.get(parkingFacilityType).values()) {
                    if (n.getCoord().getX() < minx) { minx = n.getCoord().getX(); }
                    if (n.getCoord().getY() < miny) { miny = n.getCoord().getY(); }
                    if (n.getCoord().getX() > maxx) { maxx = n.getCoord().getX(); }
                    if (n.getCoord().getY() > maxy) { maxy = n.getCoord().getY(); }
                }
                minx -= 1.0;
                miny -= 1.0;
                maxx += 1.0;
                maxy += 1.0;
                // yy the above four lines are problematic if the coordinate values are much smaller than one. kai, oct'15

                log.info("building parking garage QuadTree for nodes: xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
                QuadTree<Id<ActivityFacility>> quadTree = new QuadTree<>(minx, miny, maxx, maxy);
                for (ParkingFacility n : this.parkingFacilitiesByType.get(parkingFacilityType).values()) {
                    quadTree.put(n.getCoord().getX(), n.getCoord().getY(), n.getId());
                }
                /* assign the quadTree at the very end, when it is complete.
                 * otherwise, other threads may already start working on an incomplete quadtree
                 */
                this.availableParkingFacilityQuadTreeByType.putIfAbsent(parkingFacilityType, quadTree);
                log.info(availableParkingFacilityQuadTreeByType.toString());
                log.info("Building parking garage QuadTree took " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
            }
        }
    }

    @Override
    public boolean reserveSpaceAtLinkIdIfVehicleCanParkHere(Id<Vehicle> vehicleId, Id<Link> linkId, double fromTime, double toTime) {
        // check if there is any parking on this link
        if (!this.facilitiesPerLinkByType.containsKey(linkId)) {
            return false;
        }

        for (String parkingType : parkingFacilityTypePriorityList) {

            // check if type of parking is on this link
            if (this.facilitiesPerLinkByType.get(linkId).containsKey(parkingType)) {

                // if yes, get the corresponding facility
                Id<ActivityFacility> parkingFacilityId = this.facilitiesPerLinkByType.get(linkId).get(parkingType);
                ParkingFacility parkingFacility = this.parkingFacilities.get(parkingFacilityId);

                // check if the vehicle is allowed to park in this facility
                if (parkingFacility.isAllowedToPark(fromTime, toTime, Id.createPersonId(vehicleId))) {

                    // check if there is remaining capacity and reserve if available
                    double capacity = this.capacity.get(parkingFacilityId);
                    if (this.occupation.get(parkingFacilityId).doubleValue() < capacity) {
                        reserveNSpacesNearFacilityId(vehicleId, parkingFacilityId, new MutableLong(this.vehiclesPerVehicle));
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean reserveSpaceAtParkingFacilityIdIfVehicleCanParkHere(Id<Vehicle> vehicleId, Id<ActivityFacility> parkingFacilityId, double fromTime, double toTime) {
        boolean canPark = parkingFacilityIdHasAvailableParkingForVehicle(parkingFacilityId, vehicleId, fromTime, toTime);
        if (canPark) {
            // if we are parking outside the facilities, only need to park a single vehicle
            if (parkingFacilityId.toString().equals("outside")) {
                reserveSpaceAtFacilityId(vehicleId, parkingFacilityId);
            }
            // otherwise, we need to scale up the number of vehicles
            else {
                reserveNSpacesNearFacilityId(vehicleId, parkingFacilityId, new MutableLong(this.vehiclesPerVehicle));
            }
        }
        return canPark;
    }

    private void reserveNSpacesNearFacilityId(Id<Vehicle> vehicleId, Id<ActivityFacility> facilityId, MutableLong nSpacesToReserve) {
        reserveSpaceAtFacilityId(vehicleId, facilityId);
        nSpacesToReserve.decrement();

        // facility id coordinates
        double facilityX = this.parkingFacilities.get(facilityId).getCoord().getX();
        double facilityY = this.parkingFacilities.get(facilityId).getCoord().getY();

        String parkingType = this.parkingFacilities.get(facilityId).getParkingType();
        while (nSpacesToReserve.doubleValue() > 0) {
            // try parking in nearest facility of same type
            if (availableParkingFacilityQuadTreeByType.get(parkingType).size() > 0) {
                Id<ActivityFacility> closestFacilityId = availableParkingFacilityQuadTreeByType.get(parkingType).getClosest(facilityX, facilityY);
                reserveSpaceAtFacilityId(vehicleId, closestFacilityId);
                nSpacesToReserve.decrement();
            }
            // try parking in any available facility based on priority list
            else if (availableParkingFacilityQuadTree.size() > 0) {
                for (String type : parkingFacilityTypePriorityList) {
                    if (availableParkingFacilityQuadTreeByType.get(type).size() > 0) {
                        Id<ActivityFacility> closestFacilityId = availableParkingFacilityQuadTreeByType.get(type).getClosest(facilityX, facilityY);
                        reserveSpaceAtFacilityId(vehicleId, closestFacilityId);
                        nSpacesToReserve.decrement();
                        break;
                    }
                }
            }
            // otherwise park outside facilities, i.e. illegally
            else {
                reserveSpaceAtFacilityId(vehicleId, Id.create("outside", ActivityFacility.class));
                nSpacesToReserve.setValue(0);
            }
        }
    }

    private void reserveSpaceAtFacilityId(Id<Vehicle> vehicleId, Id<ActivityFacility> parkingFacilityId){
        // adjust occupancy levels
        if (!parkingFacilityId.toString().equals("outside")) {
            this.occupation.get(parkingFacilityId).increment();

            // deal with quadtree
            double occupation = this.occupation.get(parkingFacilityId).doubleValue();
            double capacity = this.capacity.get(parkingFacilityId);
            String parkingFacilityType = this.parkingFacilities.get(parkingFacilityId).getParkingType();

            if (occupation == capacity) {
                double x = this.parkingFacilities.get(parkingFacilityId).getCoord().getX();
                double y = this.parkingFacilities.get(parkingFacilityId).getCoord().getY();
                this.availableParkingFacilityQuadTree.remove(x, y, parkingFacilityId);
                this.availableParkingFacilityQuadTreeByType.get(parkingFacilityType).remove(x, y, parkingFacilityId);
            }
        }

        // make reservation
        this.parkingReservation.putIfAbsent(vehicleId, new LinkedList<>());
        this.parkingReservation.get(vehicleId).add(parkingFacilityId);
    }



    private boolean parkingFacilityIdHasAvailableParkingForVehicle(Id<ActivityFacility> parkingFacilityId, Id<Vehicle> vehicleId, double startTime, double endTime){
        // first check if the facility is actually in the list of parking facilities
        if (parkingFacilityId.toString().equals("outside")) {
            return true;
        } else if (!this.parkingFacilities.containsKey(parkingFacilityId)){
            return false;
        }
        ParkingFacility parkingFacility = this.parkingFacilities.get(parkingFacilityId);

        // check if the vehicle is allowed to park in this facility
        if (!parkingFacility.isAllowedToPark(startTime, endTime, Id.createPersonId(vehicleId))) {
            return false;
        }

        // check if there is remaining capacity
        double capacity = this.capacity.get(parkingFacilityId);
        return this.occupation.get(parkingFacilityId).doubleValue() < capacity;
    }

    @Override
    public Id<Link> getVehicleParkingLocationLinkId(Id<Vehicle> vehicleId) {
        if (this.parkingLocations.containsKey(vehicleId)) {
            return this.parkingFacilities.get(this.parkingLocations.get(vehicleId).get(0)).getLinkId();
        } else return this.parkingLocationsOutsideFacilities.getOrDefault(vehicleId, null);
    }

    @Override
    public Id<ActivityFacility> getVehicleParkingLocationParkingFacilityId(Id<Vehicle> vehicleId) {
        if (this.parkingLocations.containsKey(vehicleId)) {
            return this.parkingLocations.get(vehicleId).get(0);
        } else if (this.parkingLocationsOutsideFacilities.containsKey(vehicleId)) {
            return Id.create("outside", ActivityFacility.class);
        } else {
            return null;
        }
    }

    @Override
    public boolean parkVehicleAtLinkId(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
        List<Id<ActivityFacility>> reservedParkingFacilityIds = this.parkingReservation.remove(vehicleId);
        if (reservedParkingFacilityIds == null) {
            throw new RuntimeException("no parking reservation found for vehicle " + vehicleId.toString());
        }

        for (Id<ActivityFacility> reservedParkingFacilityId : reservedParkingFacilityIds) {
            if (reservedParkingFacilityId.toString().equals("outside")) {
                this.parkingLocationsOutsideFacilities.put(vehicleId, linkId);
            } else {
                this.parkingLocations.putIfAbsent(vehicleId, new LinkedList<>());
                this.parkingLocations.get(vehicleId).add(reservedParkingFacilityId);
            }
        }
        return true;
    }

    @Override
    public boolean parkVehicleAtParkingFacilityId(Id<Vehicle> vehicleId, Id<ActivityFacility> parkingFacilityId, double time) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public boolean unParkVehicle(Id<Vehicle> vehicleId, double time) {
        if (this.parkingLocationsOutsideFacilities.containsKey(vehicleId)) {
            this.parkingLocationsOutsideFacilities.remove(vehicleId);
        } else if (this.parkingLocations.containsKey(vehicleId)) {
            List<Id<ActivityFacility>> parkingFacilityIds = this.parkingLocations.remove(vehicleId);
            for (Id<ActivityFacility> parkingFacilityId : parkingFacilityIds) {
                this.occupation.get(parkingFacilityId).decrement();

                // add facility to quadtree if missing
                if (!this.availableParkingFacilityQuadTree.values().contains(parkingFacilityId)) {
                    ParkingFacility parkingFacility = this.parkingFacilities.get(parkingFacilityId);
                    double x = parkingFacility.getCoord().getX();
                    double y = parkingFacility.getCoord().getY();
                    this.availableParkingFacilityQuadTree.put(x, y, parkingFacilityId);

                    String parkingFacilityType = parkingFacility.getParkingType();
                    if (!this.availableParkingFacilityQuadTreeByType.get(parkingFacilityType).values().contains(parkingFacilityId)) {
                        this.availableParkingFacilityQuadTreeByType.get(parkingFacilityType).put(x, y, parkingFacilityId);
                    }
                }
            }
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
