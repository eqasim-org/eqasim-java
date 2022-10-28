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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

import java.util.List;

/**
 * @author  jbischoff
 *
 */
public interface ParkingSearchManager {

	boolean reserveSpaceAtLinkIdIfVehicleCanParkHere(Id<Link> linkId, double fromTime, double toTime, Id<Vehicle> vehicleId, String purpose);
	boolean reserveSpaceAtParkingFacilityIdIfVehicleCanParkHere(Id<ActivityFacility> parkingFacilityId, double fromTime, double toTime, Id<Vehicle> vehicleId, String purpose);

	Id<Link> getVehicleParkingLocationLinkId(Id<Vehicle> vehicleId);
	Id<ActivityFacility> getVehicleParkingLocationParkingFacilityId(Id<Vehicle> vehicleId);

	boolean parkVehicleAtLinkId(Id<Vehicle> vehicleId, Id<Link> linkId, double time);
	boolean parkVehicleAtParkingFacilityId(Id<Vehicle> vehicleId, Id<ActivityFacility> parkingFacilityId, double time);

	boolean unParkVehicle(Id<Vehicle> vehicleId, double time);

	List<String> produceStatistics();
	void reset(int iteration);


}
