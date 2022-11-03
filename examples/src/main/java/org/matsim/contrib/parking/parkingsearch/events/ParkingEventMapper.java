package org.matsim.contrib.parking.parkingsearch.events;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.MatsimEventsReader.CustomEventMapper;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

public class ParkingEventMapper implements CustomEventMapper {
	@Override
	public ParkingEvent apply(GenericEvent event) {
		double time = event.getTime();
		Id<Vehicle> vehicleId = Id.create(event.getAttributes().get(ParkingEvent.ATTRIBUTE_VEHICLE), Vehicle.class);
		Id<Link> linkId = Id.create(event.getAttributes().get(ParkingEvent.ATTRIBUTE_LINK), Link.class);
		Id<ActivityFacility> parkingFacilityId = Id.create(event.getAttributes().get(ParkingEvent.ATTRIBUTE_PARKING_FACILITY_ID), ActivityFacility.class);
		String parkingType = event.getAttributes().get(ParkingEvent.ATTRIBUTE_PARKING_FACILITY_TYPE);
		double x = Double.parseDouble(event.getAttributes().get(ParkingEvent.ATTRIBUTE_PARKING_COORD_X));
		double y = Double.parseDouble(event.getAttributes().get(ParkingEvent.ATTRIBUTE_PARKING_COORD_Y));
		Coord parkingCoord = new Coord(x, y);
		return new ParkingEvent(time, linkId, vehicleId, parkingFacilityId, parkingType, parkingCoord);
	}
}
