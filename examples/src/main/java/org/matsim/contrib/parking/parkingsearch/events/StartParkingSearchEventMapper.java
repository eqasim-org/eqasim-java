package org.matsim.contrib.parking.parkingsearch.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.MatsimEventsReader.CustomEventMapper;
import org.matsim.vehicles.Vehicle;

public class StartParkingSearchEventMapper implements CustomEventMapper {
	@Override
	public StartParkingSearchEvent apply(GenericEvent event) {
		double time = event.getTime();
		Id<Vehicle> vehicleId = Id.create(event.getAttributes().get(ParkingEvent.ATTRIBUTE_VEHICLE), Vehicle.class);
		Id<Link> linkId = Id.create(event.getAttributes().get(ParkingEvent.ATTRIBUTE_LINK), Link.class);
		return new StartParkingSearchEvent(time, vehicleId, linkId);
	}
}
