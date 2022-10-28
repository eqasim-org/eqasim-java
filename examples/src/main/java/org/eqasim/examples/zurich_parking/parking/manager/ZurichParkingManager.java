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
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.facilities.ParkingFacility;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author  ctchervenkov
 *
 */
public class ZurichParkingManager implements ParkingSearchManager, IterationEndsListener {

    // parking facility information
    protected Map<Id<ActivityFacility>, ParkingFacility> parkingFacilities = new HashMap<>();
    protected Map<String, Map<Id<ActivityFacility>, ParkingFacility>> parkingFacilitiesByType = new HashMap<>();
    protected Map<Id<Link>, Set<Id<ActivityFacility>>> facilitiesPerLink = new HashMap<>();
    protected Map<Id<Link>, Map<String, Id<ActivityFacility>>> facilitiesPerLinkByType = new HashMap<>();

    // parked vehicles information
    protected Set<String> parkingFacilityTypePriorityList = new HashSet<>();
    protected Map<Id<Vehicle>, List<Id<ActivityFacility>>> parkingReservation = new ConcurrentHashMap<>();
    protected Map<Id<Vehicle>, List<Id<ActivityFacility>>> parkingLocations = new ConcurrentHashMap<>();
    protected Map<Id<Vehicle>, Id<Link>> parkingLocationsOutsideFacilities = new ConcurrentHashMap<>();
    protected Map<Id<Vehicle>, Id<Link>> parkingLocationsIllegal = new ConcurrentHashMap<>();

    // occupancy information
    protected Map<Id<ActivityFacility>, Double> capacity = new HashMap<>();
    protected Map<Id<ActivityFacility>, MutableLong> occupation = new ConcurrentHashMap<>();
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

        // get the range of the quadtrees to build based on network
        double startTime = System.currentTimeMillis();
        double minx = Double.POSITIVE_INFINITY;
        double miny = Double.POSITIVE_INFINITY;
        double maxx = Double.NEGATIVE_INFINITY;
        double maxy = Double.NEGATIVE_INFINITY;
        for (Node n : this.network.getNodes().values()) {
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


        // build general quadtree
        {
            log.info("building parking garage QuadTree for nodes: xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
            QuadTree<Id<ActivityFacility>> quadTree = new QuadTree<>(minx, miny, maxx, maxy);
            for (ParkingFacility f : this.parkingFacilities.values()) {
                quadTree.put(f.getCoord().getX(), f.getCoord().getY(), f.getId());
            }
            /* assign the quadTree at the very end, when it is complete.
             * otherwise, other threads may already start working on an incomplete quadtree
             */
            this.availableParkingFacilityQuadTree = quadTree;
            log.info("Building parking garage QuadTree took " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
        }

        // build general quadtree by parking type
        {
            this.availableParkingFacilityQuadTreeByType = new HashMap<>();

            for (String parkingFacilityType : this.parkingFacilitiesByType.keySet()) {

                log.info("building parking garage QuadTree for nodes: xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
                QuadTree<Id<ActivityFacility>> quadTree = new QuadTree<>(minx, miny, maxx, maxy);
                for (ParkingFacility f : this.parkingFacilitiesByType.get(parkingFacilityType).values()) {
                    quadTree.put(f.getCoord().getX(), f.getCoord().getY(), f.getId());
                }
                /* assign the quadTree at the very end, when it is complete.
                 * otherwise, other threads may already start working on an incomplete quadtree
                 */
                this.availableParkingFacilityQuadTreeByType.putIfAbsent(parkingFacilityType, quadTree);
                log.info("Building parking garage QuadTree took " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
            }
        }
    }

    @Override
    public boolean reserveSpaceAtLinkIdIfVehicleCanParkHere(Id<Link> linkId, double fromTime, double toTime, Id<Vehicle> vehicleId, String purpose) {

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
                if (parkingFacility.isAllowedToPark(fromTime, toTime, Id.createPersonId(vehicleId), purpose)) {

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

    private boolean isParkingIdOutsideOrIllegal(Id<ActivityFacility> parkingFacilityId) {
        return (parkingFacilityId.toString().equals("outside")) || (parkingFacilityId.toString().equals("illegal"));
    }

    @Override
    public boolean reserveSpaceAtParkingFacilityIdIfVehicleCanParkHere(Id<ActivityFacility> parkingFacilityId, double fromTime, double toTime, Id<Vehicle> vehicleId, String purpose) {
        boolean canPark = parkingFacilityIdHasAvailableParkingForVehicle(parkingFacilityId, vehicleId, fromTime, toTime, purpose);
        if (canPark) {
            // if we are parking outside the facilities or illegally, only need to park a single vehicle
            if (isParkingIdOutsideOrIllegal(parkingFacilityId)) {
                reserveSpaceAtFacilityId(vehicleId, parkingFacilityId);
            }
            // otherwise, we need to scale up the number of vehicles
            else {
                reserveNSpacesNearFacilityId(vehicleId, parkingFacilityId, new MutableLong(this.vehiclesPerVehicle));
            }
        }
        return canPark;
    }

    synchronized private void reserveNSpacesNearFacilityId(Id<Vehicle> vehicleId, Id<ActivityFacility> facilityId, MutableLong nSpacesToReserve) {
        reserveSpaceAtFacilityId(vehicleId, facilityId);
        nSpacesToReserve.decrement();

        // facility id coordinates
        double facilityX = this.parkingFacilities.get(facilityId).getCoord().getX();
        double facilityY = this.parkingFacilities.get(facilityId).getCoord().getY();
        String parkingType = this.parkingFacilities.get(facilityId).getParkingType();

        while (nSpacesToReserve.doubleValue() > 0) {
            // try parking in nearest facility of same type
            if (!availableParkingFacilityQuadTreeByType.get(parkingType).values().isEmpty()) {

                Id<ActivityFacility> closestFacilityId = availableParkingFacilityQuadTreeByType.get(parkingType).getClosest(facilityX, facilityY);
                reserveSpaceAtFacilityId(vehicleId, closestFacilityId);
                nSpacesToReserve.decrement();
            }
            // try parking in any available facility based on priority list
            else if (availableParkingFacilityQuadTree.size() > 0) {
                for (String type : parkingFacilityTypePriorityList) {
                    if (availableParkingFacilityQuadTreeByType.containsKey(type)) {
                        if (!availableParkingFacilityQuadTreeByType.get(type).values().isEmpty()) {
                            Id<ActivityFacility> closestFacilityId = availableParkingFacilityQuadTreeByType.get(type).getClosest(facilityX, facilityY);
                            reserveSpaceAtFacilityId(vehicleId, closestFacilityId);
                            nSpacesToReserve.decrement();
                            break;
                        }
                    }
                }
            }
            // otherwise park outside facilities, i.e. illegally
            else {
                log.warn("No more vacant parking facilities. Vehicle " + vehicleId.toString() + " is parking illegally.");
                reserveSpaceAtFacilityId(vehicleId, Id.create("illegal", ActivityFacility.class));
                nSpacesToReserve.setValue(0);
            }
        }
    }

    private void reserveSpaceAtFacilityId(Id<Vehicle> vehicleId, Id<ActivityFacility> parkingFacilityId){
        // adjust occupancy levels
        if (!isParkingIdOutsideOrIllegal(parkingFacilityId)) {
            this.occupation.get(parkingFacilityId).increment();

            // deal with quadtree
            double occupation = this.occupation.get(parkingFacilityId).doubleValue();
            double capacity = this.capacity.get(parkingFacilityId);
            if (occupation == capacity) {
                removeParkingFacilityFromQuadTrees(this.parkingFacilities.get(parkingFacilityId));
            }
        }

        // make reservation
        this.parkingReservation.putIfAbsent(vehicleId, new LinkedList<>());
        this.parkingReservation.get(vehicleId).add(parkingFacilityId);
    }

    synchronized private void removeParkingFacilityFromQuadTrees(ParkingFacility parkingFacility) {
        Id<ActivityFacility> parkingFacilityId = parkingFacility.getId();
        String parkingFacilityType = parkingFacility.getParkingType();
        double x = parkingFacility.getCoord().getX();
        double y = parkingFacility.getCoord().getY();

        synchronized (availableParkingFacilityQuadTree) {
            availableParkingFacilityQuadTree.remove(x, y, parkingFacilityId);
        }

        synchronized (availableParkingFacilityQuadTreeByType) {
            availableParkingFacilityQuadTreeByType.get(parkingFacilityType).remove(x, y, parkingFacilityId);
        }
    }

    synchronized private void addParkingFacilityFromQuadTrees(ParkingFacility parkingFacility) {
        Id<ActivityFacility> parkingFacilityId = parkingFacility.getId();
        String parkingFacilityType = parkingFacility.getParkingType();
        double x = parkingFacility.getCoord().getX();
        double y = parkingFacility.getCoord().getY();

        synchronized (availableParkingFacilityQuadTree) {
            availableParkingFacilityQuadTree.put(x, y, parkingFacilityId);
        }

        synchronized (availableParkingFacilityQuadTreeByType) {
            availableParkingFacilityQuadTreeByType.get(parkingFacilityType).put(x, y, parkingFacilityId);
        }
    }



    private boolean parkingFacilityIdHasAvailableParkingForVehicle(Id<ActivityFacility> parkingFacilityId, Id<Vehicle> vehicleId, double startTime, double endTime, String purpose){
        // first check if the facility is actually in the list of parking facilities
        if (isParkingIdOutsideOrIllegal(parkingFacilityId)) {
            return true;
        } else if (!this.parkingFacilities.containsKey(parkingFacilityId)){
            return false;
        }
        ParkingFacility parkingFacility = this.parkingFacilities.get(parkingFacilityId);

        // check if the vehicle is allowed to park in this facility
        if (!parkingFacility.isAllowedToPark(startTime, endTime, Id.createPersonId(vehicleId), purpose)) {
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
        } else if (this.parkingLocationsIllegal.containsKey(vehicleId)) {
            return this.parkingLocationsIllegal.get(vehicleId);
        } else {
            return this.parkingLocationsOutsideFacilities.getOrDefault(vehicleId, null);
        }
    }

    @Override
    public Id<ActivityFacility> getVehicleParkingLocationParkingFacilityId(Id<Vehicle> vehicleId) {
        if (this.parkingLocations.containsKey(vehicleId)) {
            return this.parkingLocations.get(vehicleId).get(0);
        } else if (this.parkingLocationsOutsideFacilities.containsKey(vehicleId)) {
            return Id.create("outside", ActivityFacility.class);
        } else if (this.parkingLocationsIllegal.containsKey(vehicleId)) {
            return Id.create("illegal", ActivityFacility.class);
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
            } else if (reservedParkingFacilityId.toString().equals("illegal")) {
                this.parkingLocationsIllegal.put(vehicleId, linkId);
            } else {
                this.parkingLocations.putIfAbsent(vehicleId, new LinkedList<>());
                this.parkingLocations.get(vehicleId).add(reservedParkingFacilityId);
            }
        }
//        log.info(produceOverallStats());
        return true;
    }

    @Override
    public boolean parkVehicleAtParkingFacilityId(Id<Vehicle> vehicleId, Id<ActivityFacility> parkingFacilityId, double time) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public boolean unParkVehicle(Id<Vehicle> vehicleId, double time) {
        boolean foundVehicle = false;

        // check within legal facilities
        if (this.parkingLocations.containsKey(vehicleId)) {
            List<Id<ActivityFacility>> parkingFacilityIds = this.parkingLocations.remove(vehicleId);
            foundVehicle = true;
            for (Id<ActivityFacility> parkingFacilityId : parkingFacilityIds) {
                this.occupation.get(parkingFacilityId).decrement();

                // add facility to quadtree if missing
                if (!this.availableParkingFacilityQuadTree.values().contains(parkingFacilityId)) {
                    addParkingFacilityFromQuadTrees(this.parkingFacilities.get(parkingFacilityId));
                }
            }
        }

        // check illegal facilities
        if (this.parkingLocationsIllegal.containsKey(vehicleId)) {
            this.parkingLocationsIllegal.remove(vehicleId);
            foundVehicle = true;
        }

        // check outside facilities
        if (this.parkingLocationsOutsideFacilities.containsKey(vehicleId)) {
            this.parkingLocationsOutsideFacilities.remove(vehicleId);
            foundVehicle = true;
        }

//        log.info(produceOverallStats());
        return foundVehicle;
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

    private String produceOverallStats() {
        double overallOccupancy = 0.0;
        double overallCapacity = 0.0;

        for (Id<ActivityFacility> facilityId : this.parkingFacilities.keySet()) {
            overallOccupancy += this.occupation.getOrDefault(facilityId, new MutableLong(0)).doubleValue();
            overallCapacity += this.capacity.getOrDefault(facilityId, 0.0);
        }
        return "Parked vehicles : " + (int) overallOccupancy +
                "; Capacity: " + (int) overallCapacity +
                "; Occupancy level: " + Math.round(overallOccupancy / overallCapacity * 1000.0) / 10.0 ;
    }

    private String produceDetailedStats() {
        List<String> stats = new ArrayList<>();

        for (String parkingType : this.parkingFacilitiesByType.keySet()) {

            StringBuilder s = new StringBuilder();
            s.append(parkingType).append(" ");

            double occupancy = 0.0;
            double capacity = 0.0;

            for (Id<ActivityFacility> facilityId : this.parkingFacilitiesByType.get(parkingType).keySet()) {
                occupancy += this.occupation.get(facilityId).doubleValue();
                capacity += this.capacity.get(facilityId);
            }

            s.append("parked: ").append((int) occupancy).append(", ");
            s.append("capacity: ").append((int) capacity).append(", ");
            s.append("occupancy level: ").append(Math.round(occupancy / capacity * 1000.0) / 10.0);

            stats.add(s.toString());
        }

        return String.join("; ", stats);
    }

    private String produceNumberOfFacilitiesInQuadtree() {
        List<String> stats = new ArrayList<>();

        for (String parkingType : this.availableParkingFacilityQuadTreeByType.keySet()) {
            int numFacilities = this.availableParkingFacilityQuadTreeByType.get(parkingType).size();
            String s = parkingType + " -  number facilities: " + numFacilities;
            stats.add(s);
        }

        return String.join("; ", stats);
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
        // clear containers
        this.occupation.clear();
        this.parkingReservation.clear();
        this.parkingLocations.clear();
        this.parkingLocationsIllegal.clear();
        this.parkingLocationsOutsideFacilities.clear();
        this.availableParkingFacilityQuadTree.clear();
        this.availableParkingFacilityQuadTreeByType.values().forEach(QuadTree::clear);

        for (Id<ActivityFacility> parkingId : this.parkingFacilities.keySet()) {

            // reset occupancy values
            this.occupation.put(parkingId, new MutableLong(0));

            // reset quadtrees
            ParkingFacility parkingFacility = this.parkingFacilities.get(parkingId);
            double x = parkingFacility.getCoord().getX();
            double y = parkingFacility.getCoord().getY();
            String parkingType = parkingFacility.getParkingType();
            this.availableParkingFacilityQuadTree.put(x, y, parkingId);
            this.availableParkingFacilityQuadTreeByType.get(parkingType).put(x, y, parkingId);
        }
//        log.info(produceDetailedStats());
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        this.reset(event.getIteration());
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
