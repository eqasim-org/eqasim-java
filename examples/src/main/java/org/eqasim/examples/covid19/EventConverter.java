package org.eqasim.examples.covid19;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

public class EventConverter implements ActivityStartEventHandler, ActivityEndEventHandler, GenericEventHandler {
	private final VehicleFinder vehicleFinder;

	private final List<Event> events = new LinkedList<>();

	public EventConverter(VehicleFinder vehicleFinder) {
		this.vehicleFinder = vehicleFinder;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getPersonId().toString().startsWith("freight")) {
			return;
		}

		events.add(event);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getPersonId().toString().startsWith("freight")) {
			return;
		}

		events.add(event);
	}

	@Override
	public void handleEvent(GenericEvent event) {
		if (event instanceof PublicTransitEvent) {
			PublicTransitEvent transitEvent = (PublicTransitEvent) event;

			Id<Person> personId = transitEvent.getPersonId();
			Id<Vehicle> vehicleId = vehicleFinder.findVehicleId(transitEvent);

			// The event comes when the trip is finished, so the event time is the "leave
			// time".
			double enterTime = transitEvent.getVehicleDepartureTime();
			double leaveTime = transitEvent.getTime();

			PersonEntersVehicleEvent enterEvent = new PersonEntersVehicleEvent(enterTime, personId, vehicleId);
			PersonLeavesVehicleEvent leaveEvent = new PersonLeavesVehicleEvent(leaveTime, personId, vehicleId);

			events.add(enterEvent);
			events.add(leaveEvent);
		}
	}

	public void replay(EventsManager manager) {
		System.out.println("Sorting ...");
		Collections.sort(events, new Comparator<Event>() {
			@Override
			public int compare(Event a, Event b) {
				return Double.compare(a.getTime(), b.getTime());
			}
		});

		events.forEach(manager::processEvent);
	}
}
