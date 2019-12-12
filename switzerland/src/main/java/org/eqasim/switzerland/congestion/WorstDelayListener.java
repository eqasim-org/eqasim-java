package org.eqasim.switzerland.congestion;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class WorstDelayListener
		implements IterationStartsListener, VehicleEntersTrafficEventHandler, LinkEnterEventHandler,
		LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler, IterationEndsListener {
	private final Network network;

	public WorstDelayListener(Network network) {
		this.network = network;
	}

	static public class WorstDelays {
		public int iteration;
		public Map<Id<Link>, Double> perLink = new HashMap<>();

		public WorstDelays(Network network) {
			for (Id<Link> linkId : network.getLinks().keySet()) {
				this.perLink.put(linkId, 0.0);
			}
		}
	}

	private WorstDelays worstDelays;

	private final Map<Id<Vehicle>, Double> enterTimes = new HashMap<>();

	private List<WorstDelays> results = new LinkedList<>();

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		System.out.println("Starting iteration: " + event.getIteration());

		enterTimes.clear();

		worstDelays = new WorstDelays(network);
		worstDelays.iteration = event.getIteration();
		results.add(worstDelays);
	}

	public void startIteration(int iteration) {
		System.out.println("Starting iteration: " + iteration);

		enterTimes.clear();

		worstDelays = new WorstDelays(network);
		worstDelays.iteration = iteration;
		results.add(worstDelays);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		enterTimes.put(event.getVehicleId(), event.getTime());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		enterTimes.put(event.getVehicleId(), event.getTime());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Link> linkId = event.getLinkId();

		double enterTime = enterTimes.remove(event.getVehicleId());
		double travelTime = event.getTime() - enterTime;
		double freeflowTravelTime = network.getLinks().get(linkId).getLength() / network.getLinks().get(linkId).getFreespeed();
		double delay = travelTime - freeflowTravelTime;

		double worstDelay = worstDelays.perLink.getOrDefault(linkId, 0.0);

		if (delay >= worstDelay) {
			worstDelays.perLink.put(linkId, delay);
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		Id<Link> linkId = event.getLinkId();

		double enterTime = enterTimes.remove(event.getVehicleId());
		double travelTime = event.getTime() - enterTime;
		double freeflowTravelTime = network.getLinks().get(linkId).getLength() / network.getLinks().get(linkId).getFreespeed();
		double delay = travelTime - freeflowTravelTime;

		double worstDelay = worstDelays.perLink.getOrDefault(linkId, 0.0);

		if (delay >= worstDelay) {
			worstDelays.perLink.put(linkId, delay);
		}
	}

	public List<WorstDelays> getResults() {
		return results;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		System.out.println("Iteration done: " + event.getIteration());
	}

	public void endIteration(int iteration) {
		System.out.println("Iteration done: " + iteration);
	}
}
