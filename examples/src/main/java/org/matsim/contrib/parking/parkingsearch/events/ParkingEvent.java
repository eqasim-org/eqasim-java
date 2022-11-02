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

package org.matsim.contrib.parking.parkingsearch.events;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

/**
 * @author  ctchervenkov
 *
 */

public class ParkingEvent extends Event {
	public static final String EVENT_TYPE = "parked vehicle";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_PARKING_FACILITY_ID = "parking facility id";
	public static final String ATTRIBUTE_PARKING_FACILITY_TYPE = "parking type";
	public static final String ATTRIBUTE_PARKING_COORD = "parking coord";
	private final Id<Link> linkId;
	private final Id<Vehicle> vehicleId;
	private final Id<ActivityFacility> parkingFacilityId;
	private final String parkingFacilityType;
	private final Coord parkingFacilityCoord;


	public ParkingEvent(double time, Id<Link> linkId, Id<Vehicle> vehicleId, Id<ActivityFacility> parkingFacilityId,
						String parkingFacilityType, Coord parkingFacilityCoord) {
		super(time);
		this.linkId = linkId;
		this.vehicleId = vehicleId;
		this.parkingFacilityId = parkingFacilityId;
		this.parkingFacilityType = parkingFacilityType;
		this.parkingFacilityCoord = parkingFacilityCoord;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}

	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	public Id<ActivityFacility> getParkingFacilityId() {
		return parkingFacilityId;
	}

	public String getParkingFacilityType() {
		return parkingFacilityType;
	}

	public Coord getParkingFacilityCoord() {
		return parkingFacilityCoord;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		attr.put(ATTRIBUTE_PARKING_FACILITY_ID, this.parkingFacilityId.toString());
		attr.put(ATTRIBUTE_PARKING_FACILITY_TYPE, this.parkingFacilityType);
		attr.put(ATTRIBUTE_PARKING_COORD, this.parkingFacilityCoord.toString());
		return attr;
	}

}
