package org.eqasim.core.scenario.cutter.population.trips.crossing.network.timing;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class LinkTimingRegistryHandler implements PersonDepartureEventHandler, VehicleEntersTrafficEventHandler,
		VehicleLeavesTrafficEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {
	private final ScenarioExtent extent;
	private final Network network;

	private final LinkTimingRegistry timingRegistry;

	private final IdMap<Person, Integer> tripIndices = new IdMap<>(Person.class);

	private final IdMap<Person, LinkEnterEvent> enterEvents = new IdMap<>(Person.class);
	private final IdMap<Vehicle, Id<Person>> driverRegistry = new IdMap<>(Vehicle.class);

	public LinkTimingRegistryHandler(ScenarioExtent extent, Network network, LinkTimingRegistry timingRegistry) {
		this.extent = extent;
		this.network = network;
		this.timingRegistry = timingRegistry;
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		driverRegistry.put(event.getVehicleId(), event.getPersonId());
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		driverRegistry.remove(event.getVehicleId());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		tripIndices.compute(event.getPersonId(), (id, value) -> value == null ? 0 : value + 1);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Link link = network.getLinks().get(event.getLinkId());

		if (extent.isInside(link.getFromNode().getCoord()) ^ extent.isInside(link.getToNode().getCoord())) {
			enterEvents.put(driverRegistry.get(event.getVehicleId()), event);
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> driverId = driverRegistry.get(event.getVehicleId());
		LinkEnterEvent enterEvent = enterEvents.remove(driverId);

		if (enterEvent != null) {
			timingRegistry.register(driverRegistry.get(event.getVehicleId()), tripIndices.get(driverId),
					enterEvent.getLinkId(), enterEvent.getTime(), event.getTime());
		}
	}
}
