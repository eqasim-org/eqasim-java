package org.eqasim.core.analysis.pt;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopArea;

public class PublicTransportLegListener implements PersonDepartureEventHandler, ActivityStartEventHandler,
		GenericEventHandler, AgentWaitingForPtEventHandler {
	private final Collection<PublicTransportLegItem> trips = new LinkedList<>();
	private final Map<Id<Person>, Integer> tripIndices = new HashMap<>();
	private final Map<Id<Person>, Integer> legIndices = new HashMap<>();
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
	}

	public void handleEvent(PublicTransitEvent event) {
		Id<TransitStopArea> accessAreaId = schedule.getFacilities().get(event.getAccessStopId()).getStopAreaId();
		Id<TransitStopArea> egressAreaId = schedule.getFacilities().get(event.getEgressStopId()).getStopAreaId();

		String routeMode = schedule.getTransitLines().get(event.getTransitLineId()) //
				.getRoutes().get(event.getTransitRouteId())//
				.getTransportMode();
		
		trips.add(new PublicTransportLegItem(event.getPersonId(), //
				tripIndices.get(event.getPersonId()), //
				legIndices.get(event.getPersonId()), //
				event.getAccessStopId(), //
				event.getEgressStopId(), //
				event.getTransitLineId(), //
				event.getTransitRouteId(), //
				accessAreaId, //
				egressAreaId, //
				routeMode //
		));
	}

	@Override
	public void handleEvent(GenericEvent event) {
		if (event instanceof PublicTransitEvent) {
			handleEvent((PublicTransitEvent) event);
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().contains("interaction")) {
			tripIndices.computeIfPresent(event.getPersonId(), (k, v) -> v - 1);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (!tripIndices.containsKey(event.getPersonId())) {
			tripIndices.put(event.getPersonId(), 0);
			legIndices.put(event.getPersonId(), 0);
		} else {
			tripIndices.compute(event.getPersonId(), (k, v) -> v + 1);
			legIndices.compute(event.getPersonId(), (k, v) -> v + 1);
		}
	}

	@Override
	public void handleEvent(AgentWaitingForPtEvent event) {
		throw new RuntimeException(
				"So far, this analysis tool only works with schedule-based simulation. Your simulation input simulated public transport in the QSim.");
	}
}