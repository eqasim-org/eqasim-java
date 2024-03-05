package org.eqasim.vdf.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import org.eqasim.vdf.handlers.VDFTrafficHandler;
import org.eqasim.vdf.travel_time.VDFTravelTime;
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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vehicles.Vehicle;

public class VDFEngine implements DepartureHandler, MobsimEngine {
	private final List<String> modes;

	private final VDFTravelTime travelTime;
	private final Network network;

	private final PriorityQueue<Traversal> traversals = new PriorityQueue<>(new TraversalComparator());

	private InternalInterface internalInterface;

	private final VDFTrafficHandler handler;
	private final boolean generateNetworkEvents;

	public VDFEngine(Collection<String> modes, VDFTravelTime travelTime, Network network, VDFTrafficHandler handler,
			boolean generateNetworkEvents) {
		this.modes = new ArrayList<>(modes);
		this.travelTime = travelTime;
		this.network = network;
		this.handler = handler;
		this.generateNetworkEvents = generateNetworkEvents;
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		Leg leg = (Leg) ((PlanAgent) agent).getCurrentPlanElement();

		if (!modes.contains(leg.getMode())) {
			return false;
		}

		MobsimDriverAgent driverAgent = (MobsimDriverAgent) agent;

		if (generateNetworkEvents) {
			EventsManager eventsManager = internalInterface.getMobsim().getEventsManager();

			eventsManager.processEvent(
					new PersonEntersVehicleEvent(now, driverAgent.getId(), driverAgent.getPlannedVehicleId()));

			Traversal traversal = new Traversal();
			traversal.agent = (MobsimDriverAgent) agent;
			traversal.linkId = agent.getCurrentLinkId();
			traversal.arrivalTime = now + getTraversalTime(now, linkId, driverAgent);
			traversal.modeIndex = modes.indexOf(leg.getMode());
			traversals.add(traversal);

			eventsManager.processEvent(new VehicleEntersTrafficEvent(now, traversal.agent.getId(), traversal.linkId,
					Id.createVehicleId(agent.getId()), modes.get(traversal.modeIndex), 1.0));
		} else { // We have a handler and register traversals directly
			NetworkRoute route = (NetworkRoute) leg.getRoute();
			now += getTraversalTime(now, route.getStartLinkId(), driverAgent);

			for (Id<Link> nextLinkId : route.getLinkIds()) {
				handler.processEnterLink(now, nextLinkId);
				now += getTraversalTime(now, nextLinkId, driverAgent);
			}

			Traversal traversal = new Traversal();
			traversal.agent = (MobsimDriverAgent) agent;
			traversal.linkId = route.getEndLinkId();
			traversal.arrivalTime = now;
			traversal.modeIndex = modes.indexOf(leg.getMode());
			traversal.distance = route.getDistance();
			traversals.add(traversal);
		}

		return true;
	}

	@Override
	public void doSimStep(double now) {

		EventsManager eventsManager = internalInterface.getMobsim().getEventsManager();
		Traversal traversal = null;

		while (!traversals.isEmpty() && traversals.peek().arrivalTime <= now) {
			traversal = traversals.poll();

			if (generateNetworkEvents) {
				if (traversal.agent.isWantingToArriveOnCurrentLink()) {
					eventsManager
							.processEvent(new VehicleLeavesTrafficEvent(now, traversal.agent.getId(), traversal.linkId,
									Id.createVehicleId(traversal.agent.getId()), modes.get(traversal.modeIndex), 1.0));

					eventsManager.processEvent(new PersonLeavesVehicleEvent(now, traversal.agent.getId(),
							Id.createVehicleId(traversal.agent.getId())));

					traversal.agent.endLegAndComputeNextState(now);
					internalInterface.arrangeNextAgentState(traversal.agent);
				} else {
					eventsManager.processEvent(
							new LinkLeaveEvent(now, Id.createVehicleId(traversal.agent.getId()), traversal.linkId));

					traversal.linkId = traversal.agent.chooseNextLinkId();
					traversal.arrivalTime = now + getTraversalTime(now, traversal.linkId, traversal.agent);
					traversals.add(traversal);

					traversal.agent.notifyMoveOverNode(traversal.linkId);

					eventsManager.processEvent(
							new LinkEnterEvent(now, Id.createVehicleId(traversal.agent.getId()), traversal.linkId));
				}
			} else {
				eventsManager.processEvent(new TeleportationArrivalEvent(now, traversal.agent.getId(),
						traversal.distance, modes.get(traversal.modeIndex)));
				traversal.agent.notifyArrivalOnLinkByNonNetworkMode(traversal.linkId);

				traversal.agent.endLegAndComputeNextState(now);
				internalInterface.arrangeNextAgentState(traversal.agent);
			}
		}
	}

	@Override
	public void onPrepareSim() {
		// Nothing to do
	}

	@Override
	public void afterSim() {
		EventsManager eventsManager = internalInterface.getMobsim().getEventsManager();
		double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();

		for (Traversal traversal : traversals) {
			eventsManager.processEvent(new PersonStuckEvent(now, traversal.agent.getId(),
					traversal.agent.getCurrentLinkId(), modes.get(traversal.modeIndex)));
		}
	}

	double getTraversalTime(double now, Id<Link> linkId, MobsimDriverAgent agent) {
		Person person = ((HasPerson) agent).getPerson();
		Vehicle vehicle = null; // agent.getVehicle().getVehicle();
		Link link = network.getLinks().get(linkId);

		return travelTime.getLinkTravelTime(link, now, person, vehicle);
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	private class Traversal {
		MobsimDriverAgent agent;
		Id<Link> linkId;
		double arrivalTime;
		int modeIndex;
		double distance;
	}

	private class TraversalComparator implements Comparator<Traversal> {
		@Override
		public int compare(Traversal a, Traversal b) {
			return Double.compare(a.arrivalTime, b.arrivalTime);
		}
	}
}
