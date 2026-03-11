package org.eqasim.core.simulation.vdf.engine;

import java.util.Comparator;
import java.util.PriorityQueue;

import org.eqasim.core.simulation.vdf.FlowEquivalentProvider;
import org.eqasim.core.simulation.vdf.handlers.VDFTrafficHandler;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vehicles.Vehicle;

/**
 * This VDF engine is meant to interact with agents that are otherwise simulated
 * by the Netsim. It obtains the route of the leg of the agent and then replays
 * this route without taking into account network dynamics otherwise. In
 * particular,
 * this engine doesn't continuously query the agent for the next link to visit,
 * but
 * assumes that the route is fixed.
 * 
 * @author sebhoerl / IRT SystemX
 */
public class VDFStaticEngine implements DepartureHandler, MobsimEngine {
	private final TraversalTime traversalTime;
	private final FlowEquivalentProvider flowEquivalentProvider;
	private final Network network;

	private final PriorityQueue<Arrival> arrivals = new PriorityQueue<>(Comparator.comparing(Arrival::time));
	private final PriorityQueue<EventRecord> events = new PriorityQueue<EventRecord>(new EventRecordComprator());

	private InternalInterface internalInterface;
	private EventsManager eventsManager;

	private final VDFTrafficHandler handler;
	private final boolean generateNetworkEvents;
	private final double simulationStep;

	public VDFStaticEngine(TraversalTime traversalTime, FlowEquivalentProvider flowEquivalentProvider, Network network,
			VDFTrafficHandler handler,
			boolean generateNetworkEvents, double simulationStep) {
		this.traversalTime = traversalTime;
		this.network = network;
		this.handler = handler;
		this.generateNetworkEvents = generateNetworkEvents;
		this.simulationStep = simulationStep;
		this.flowEquivalentProvider = flowEquivalentProvider;
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		MobsimDriverAgent driverAgent = (MobsimDriverAgent) agent;
		Id<Vehicle> vehicleId = driverAgent.getPlannedVehicleId();

		/*
		 * TODO: This code can be easily parallelized. We only need to shield the access
		 * to the handler (for counting traversals) and to the events manager for the
		 * events that are sent right here (maybe not even).
		 */

		// standard departure events
		eventsManager.processEvent(
				new PersonEntersVehicleEvent(now, agent.getId(), vehicleId));

		// traverse the route to find arrival time
		// optionally, generate future evenstream

		PlanAgent planAgent = (PlanAgent) agent;
		NetworkRoute route = (NetworkRoute) ((Leg) planAgent.getCurrentPlanElement()).getRoute();

		int sequence = 0;

		// handle the start link
		if (generateNetworkEvents) {
			events.add(new EventRecord(sequence++,
					new VehicleEntersTrafficEvent(now + simulationStep, agent.getId(), agent.getCurrentLinkId(),
							vehicleId, agent.getMode(), 1.0)));

			events.add(new EventRecord(sequence++,
					new LinkLeaveEvent(now + simulationStep * 2.0, vehicleId, route.getStartLinkId())));
		}

		now += simulationStep * 2.0; // qsim correction (see above)

		// handle the links along the route
		for (Id<Link> currentLinkId : route.getLinkIds()) {
			if (generateNetworkEvents) {
				events.add(new EventRecord(sequence++, new LinkEnterEvent(now, vehicleId, currentLinkId)));
			}

			handler.processEnterLink(now, currentLinkId, flowEquivalentProvider.getFlowEquivalent(route.getVehicleId()));
			now += traversalTime.getTraversalTime(now, currentLinkId, driverAgent) + simulationStep;

			if (generateNetworkEvents) {
				events.add(new EventRecord(sequence++, new LinkLeaveEvent(now, vehicleId, currentLinkId)));
			}
		}

		// handle the end link
		if (generateNetworkEvents) {
			events.add(new EventRecord(sequence++, new LinkEnterEvent(now, vehicleId, route.getEndLinkId())));
		}

		handler.processEnterLink(now, route.getEndLinkId(), flowEquivalentProvider.getFlowEquivalent(route.getVehicleId()));
		now += traversalTime.getTraversalTime(now, route.getEndLinkId(), driverAgent);

		if (generateNetworkEvents) {
			events.add(new EventRecord(sequence++, new VehicleLeavesTrafficEvent(now, agent.getId(),
					route.getEndLinkId(),
					vehicleId, agent.getMode(), 1.0)));
		}

		// register future arrival
		Arrival arrival = new Arrival(now, driverAgent, route.getDistance(), route.getEndLinkId());
		arrivals.add(arrival);

		return true;
	}

	@Override
	public void doSimStep(double now) {
		if (generateNetworkEvents) {
			while (!events.isEmpty() && events.peek().event().getTime() <= now) {
				eventsManager.processEvent(events.poll().event);
			}
		}

		while (!arrivals.isEmpty() && arrivals.peek().time <= now) {
			Arrival arrival = arrivals.poll();

			arrival.agent.notifyArrivalOnLinkByNonNetworkMode(arrival.destinationId);

			if (!generateNetworkEvents) {
				eventsManager.processEvent(new TeleportationArrivalEvent(now, arrival.agent.getId(), arrival.distance,
						arrival.agent.getMode()));
			}

			// standard leave vehicle event
			eventsManager.processEvent(new PersonLeavesVehicleEvent(now, arrival.agent.getId(),
					arrival.agent.getPlannedVehicleId()));

			arrival.agent.endLegAndComputeNextState(now);
			internalInterface.arrangeNextAgentState(arrival.agent);
		}
	}

	@Override
	public void onPrepareSim() {
		// Nothing to do
	}

	@Override
	public void afterSim() {
		double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();

		for (Arrival arrival : arrivals) {
			eventsManager.processEvent(new PersonStuckEvent(now, arrival.agent.getId(),
					arrival.agent.getCurrentLinkId(), arrival.agent.getMode()));
		}
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
		this.eventsManager = internalInterface.getMobsim().getEventsManager();
	}

	private record Arrival(double time, MobsimDriverAgent agent, double distance, Id<Link> destinationId) {
	}

	private record EventRecord(int sequence, Event event) {
	}

	private class EventRecordComprator implements Comparator<EventRecord> {
		@Override
		public int compare(EventRecord a, EventRecord b) {
			int result = Double.compare(a.event.getTime(), b.event.getTime());

			if (result == 0) {
				result = Integer.compare(a.sequence, b.sequence);
			}

			return result;
		}
	}
}
