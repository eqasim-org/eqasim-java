package org.eqasim.core.scenario.cutter.population.trips.crossing.network.event_based;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.NetworkCrossingPoint;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.vehicles.Vehicle;

class CrossingPointHandler implements ActivityEndEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler,
		VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler {
	private final ScenarioExtent extent;
	private final Network network;

	private final IdMap<Person, Integer> tripIndices = new IdMap<>(Person.class);
	private final IdMap<Vehicle, Set<Id<Person>>> passengers = new IdMap<>(Vehicle.class);
	private final IdMap<Vehicle, Double> enterTimes = new IdMap<>(Vehicle.class);

	private final IdMap<Person, List<Map<Id<Link>, NetworkCrossingPoint>>> data = new IdMap<>(Person.class);

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (!TripStructureUtils.isStageActivityType(event.getActType())) {
			tripIndices.compute(event.getPersonId(), (id, v) -> v == null ? 0 : v + 1);
		}
	}

	private boolean isRelevantLink(Id<Link> linkId) {
		Link link = network.getLinks().get(linkId);

		boolean fromInside = extent.isInside(link.getFromNode().getCoord());
		boolean toInside = extent.isInside(link.getToNode().getCoord());

		return fromInside ^ toInside;
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (isRelevantLink(event.getLinkId())) {
			enterTimes.put(event.getVehicleId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (isRelevantLink(event.getLinkId())) {
			enterTimes.put(event.getVehicleId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Double enterTime = enterTimes.remove(event.getVehicleId());

		if (enterTime != null) {
			registerCrossingPoint(event.getVehicleId(), event.getLinkId(), enterTime, event.getTime());
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		Double enterTime = enterTimes.remove(event.getVehicleId());

		if (enterTime != null) {
			registerCrossingPoint(event.getVehicleId(), event.getLinkId(), enterTime, event.getTime());
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		passengers.computeIfAbsent(event.getVehicleId(), id -> new HashSet<>()).add(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		passengers.get(event.getVehicleId()).remove(event.getPersonId());
	}

	private void registerCrossingPoint(Id<Vehicle> vehicleId, Id<Link> linkId, double enterTime, double leaveTime) {
		Link link = network.getLinks().get(linkId);
		boolean outgoing = !extent.isInside(link.getToNode().getCoord());

		for (Id<Person> passengerId : passengers.get(vehicleId)) {
			int tripIndex = tripIndices.get(passengerId);

			List<Map<Id<Link>, NetworkCrossingPoint>> personTrips = data.computeIfAbsent(passengerId,
					id -> new LinkedList<>());

			while (personTrips.size() < tripIndex - 1) {
				personTrips.add(Collections.emptyMap());
			}

			if (personTrips.size() < tripIndex) {
				personTrips.add(new IdMap<>(Link.class));
			}

			Map<Id<Link>, NetworkCrossingPoint> tripPoints = personTrips.get(tripIndex);
			tripPoints.put(linkId, new NetworkCrossingPoint(0, link, enterTime, leaveTime, outgoing));
		}
	}
	
	public CrossingTime getCrossingTime(Id<Person> personId, int tripIndex, Id<Link> linkId, boolean outgoing) {
		
	}
}
