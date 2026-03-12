package org.eqasim.core.analysis.pt;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.eqasim.core.components.transit.events.PublicTransitEventHandler;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopArea;
import org.matsim.vehicles.Vehicle;

import com.google.common.base.Verify;

public class PublicTransportLegListener implements PersonDepartureEventHandler,
		PersonEntersVehicleEventHandler, PublicTransitEventHandler, AgentWaitingForPtEventHandler,
		TransitDriverStartsEventHandler {
	private final Collection<PublicTransportLegItem> trips = new LinkedList<>();

	private final Map<Id<Person>, Integer> tripIndices = new HashMap<>();
	private final Map<Id<Person>, Integer> legIndices = new HashMap<>();

	// dynamic transit tracking
	private final Map<Id<Vehicle>, TransitDriverStartsEvent> vehicles = new HashMap<>();
	private final Map<Id<Person>, PublicTransportLegItem> ongoing = new HashMap<>();

	private final TransitSchedule schedule;

	public PublicTransportLegListener(TransitSchedule schedule) {
		this.schedule = schedule;
	}

	public Collection<PublicTransportLegItem> getTripItems() {
		return trips;
	}

	@Override
	public void reset(int iteration) {
		trips.clear();
		tripIndices.clear();
		vehicles.clear();
		ongoing.clear();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		Verify.verify(vehicles.put(event.getVehicleId(), event) == null);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		int tripIndex = 0;
		int legIndex = 0;

		if (!tripIndices.containsKey(event.getPersonId())) {
			tripIndices.put(event.getPersonId(), 0);
			legIndices.put(event.getPersonId(), 0);
		} else {
			tripIndex = tripIndices.compute(event.getPersonId(), (k, v) -> v + 1);
			legIndex = legIndices.compute(event.getPersonId(), (k, v) -> v + 1);
		}

		if (event.getLegMode().equals(TransportMode.pt)) {
			PublicTransportLegItem item = new PublicTransportLegItem(event.getPersonId(), tripIndex, legIndex);

			Verify.verify(ongoing.put(event.getPersonId(), item) == null);
			trips.add(item);
		}
	}

	@Override
	public void handleEvent(PublicTransitEvent event) {
		Id<TransitStopArea> accessAreaId = schedule.getFacilities().get(event.getAccessStopId()).getStopAreaId();
		Id<TransitStopArea> egressAreaId = schedule.getFacilities().get(event.getEgressStopId()).getStopAreaId();

		String transitMode = schedule.getTransitLines().get(event.getTransitLineId()) //
				.getRoutes().get(event.getTransitRouteId())//
				.getTransportMode();

		PublicTransportLegItem item = Objects.requireNonNull(ongoing.remove(event.getPersonId()));
		item.accessStopId = event.getAccessStopId();
		item.egressStopId = event.getEgressStopId();
		item.accessAreaId = accessAreaId;
		item.egressAreaId = egressAreaId;
		item.transitLineId = event.getTransitLineId();
		item.transitRouteId = event.getTransitRouteId();
		item.departureId = event.getDepartureId();
		item.transitMode = transitMode;
		item.boardingTime = event.getBoardingTime();
	}

	@Override
	public void handleEvent(AgentWaitingForPtEvent event) {
		PublicTransportLegItem item = Objects.requireNonNull(ongoing.get(event.getPersonId()));

		Id<TransitStopArea> accessAreaId = schedule.getFacilities().get(event.getWaitingAtStopId()).getStopAreaId();
		Id<TransitStopArea> egressAreaId = schedule.getFacilities().get(event.getDestinationStopId()).getStopAreaId();

		item.accessStopId = event.getWaitingAtStopId();
		item.egressStopId = event.getDestinationStopId();
		item.accessAreaId = accessAreaId;
		item.egressAreaId = egressAreaId;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		PublicTransportLegItem item = ongoing.remove(event.getPersonId());

		if (item != null) {
			TransitDriverStartsEvent vehicle = Objects.requireNonNull(vehicles.get(event.getVehicleId()));

			String transitMode = schedule.getTransitLines().get(vehicle.getTransitLineId()) //
					.getRoutes().get(vehicle.getTransitRouteId())//
					.getTransportMode();

			item.transitLineId = vehicle.getTransitLineId();
			item.transitRouteId = vehicle.getTransitRouteId();
			item.departureId = vehicle.getDepartureId();
			item.transitMode = transitMode;
			item.boardingTime = event.getTime();
		}
	}
}