package org.eqasim.core.simulation.vdf.engine;

import java.util.Comparator;
import java.util.PriorityQueue;

import org.eqasim.core.simulation.vdf.FlowEquivalentProvider;
import org.eqasim.core.simulation.vdf.handlers.VDFTrafficHandler;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

/**
 * This VDF engine is meant to interact with agents that are otherwise simulated
 * by the Netsim. This implementation iteratively queries the agent for the next
 * link along the route and notifies the agent when traversing links. This means
 * that the engine handles well dynamic changes of the route (within-day or
 * dynamic agents such as in DRT).
 * 
 * Note that this is independent of generating network events or not: The
 * dynamic routes of a DRT vehicel will be followed adaptively, but simply no
 * link enter and leave events will be generated.
 * 
 * @author sebhoerl / IRT SystemX
 */
public class VDFDynamicEngine implements DepartureHandler, MobsimEngine {
	private final TraversalTime traversalTime;
	private final FlowEquivalentProvider flowEquivalentProvider;
	private final Network network;

	private final PriorityQueue<ActiveRecord> queue = new PriorityQueue<>(Comparator.comparing(a -> a.triggerTime));
	private final PriorityQueue<DepartureRecord> departures = new PriorityQueue<>(
			Comparator.comparing(a -> a.triggerTime));

	private InternalInterface internalInterface;
	private EventsManager eventsManager;

	private final VDFTrafficHandler handler;
	private final boolean generateNetworkEvents;
	private final double simulationStep;

	public VDFDynamicEngine(TraversalTime traversalTime, FlowEquivalentProvider flowEquivalentProvider, Network network,
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

		eventsManager.processEvent(
				new PersonEntersVehicleEvent(now, agent.getId(), driverAgent.getPlannedVehicleId()));

		DepartureRecord record = new DepartureRecord();
		record.agent = driverAgent;
		record.triggerTime = now + simulationStep; // correction for qsim compatibility
		departures.add(record);

		return true;
	}

	@Override
	public void doSimStep(double now) {
		while (!queue.isEmpty() && queue.peek().triggerTime <= now) {
			ActiveRecord record = queue.poll();
			processRecord(now, record);
		}

		while (!departures.isEmpty() && departures.peek().triggerTime <= now) {
			DepartureRecord record = departures.poll();

			if (generateNetworkEvents) {
				eventsManager.processEvent(
						new VehicleEntersTrafficEvent(now, record.agent.getId(), record.agent.getCurrentLinkId(),
								record.agent.getPlannedVehicleId(), record.agent.getMode(), 1.0));
			}

			ActiveRecord activeRecord = new ActiveRecord();
			activeRecord.agent = record.agent;
			activeRecord.triggerTime = now + simulationStep;
			queue.add(activeRecord);
		}
	}

	private void processRecord(double now, ActiveRecord record) {
		if (record.agent.isWantingToArriveOnCurrentLink()) {
			// agent wants to arrive

			if (generateNetworkEvents) {
				eventsManager
						.processEvent(new VehicleLeavesTrafficEvent(now, record.agent.getId(),
								record.agent.getCurrentLinkId(),
								record.agent.getPlannedVehicleId(), record.agent.getMode(), 1.0));
			} else {
				eventsManager.processEvent(new TeleportationArrivalEvent(now, record.agent.getId(), record.distance,
						record.agent.getMode()));
			}

			eventsManager.processEvent(new PersonLeavesVehicleEvent(now, record.agent.getId(),
					record.agent.getPlannedVehicleId()));

			record.agent.endLegAndComputeNextState(now);
			internalInterface.arrangeNextAgentState(record.agent);

			// we are done with this agent until next departure
		} else {
			// agents wants to continue route
			Id<Link> currentLinkId = record.agent.getCurrentLinkId();

			if (generateNetworkEvents) {
				eventsManager.processEvent(
						new LinkLeaveEvent(now, record.agent.getPlannedVehicleId(), currentLinkId));
			}

			Id<Link> nextLinkId = record.agent.chooseNextLinkId();
			record.agent.notifyMoveOverNode(nextLinkId);
			handler.processEnterLink(record.triggerTime, nextLinkId,
					flowEquivalentProvider.getFlowEquivalent(record.agent.getPlannedVehicleId()));

			double qsimCorrection = record.agent.isWantingToArriveOnCurrentLink() ? 0.0 : simulationStep;
			record.triggerTime += traversalTime.getTraversalTime(now, nextLinkId, record.agent) + qsimCorrection;
			record.distance += network.getLinks().get(nextLinkId).getLength();
			queue.add(record);

			if (generateNetworkEvents) {
				eventsManager.processEvent(
						new LinkEnterEvent(now, record.agent.getPlannedVehicleId(), nextLinkId));
			}
		}
	}

	@Override
	public void onPrepareSim() {
		// Nothing to do
	}

	@Override
	public void afterSim() {
		double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();

		for (ActiveRecord record : queue) {
			eventsManager.processEvent(new PersonStuckEvent(now, record.agent.getId(),
					record.agent.getCurrentLinkId(), record.agent.getMode()));
		}
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
		this.eventsManager = internalInterface.getMobsim().getEventsManager();
	}

	private class ActiveRecord {
		MobsimDriverAgent agent;
		double triggerTime;
		double distance;
	}

	private class DepartureRecord {
		MobsimDriverAgent agent;
		double triggerTime;
	}
}
