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

public class LinkResultsListener
		implements IterationStartsListener, VehicleEntersTrafficEventHandler, LinkEnterEventHandler,
		LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler, IterationEndsListener {
	private final Network network;
	private final List<Id<Link>> linkIds;
	private final double binSize;

	public LinkResultsListener(Network network, List<Id<Link>> linkIds, double binSize) {
		this.network = network;
		this.linkIds = linkIds;
		this.binSize = binSize;
	}

	static public class IterationResult {
		public int iteration;
		public double binSize;
		public int numBins;
		public Id<Link> linkId;

		public int[] numberOfAgents;
		public double[] meanTravelTime;

		public IterationResult(Id<Link> linkId, double binSize, int numBins) {
			this.binSize = binSize;
			this.numBins = numBins;
			this.linkId = linkId;
			this.numberOfAgents = new int[numBins];
			this.meanTravelTime = new double[numBins];
		}
	}

	private final Map<Id<Vehicle>, Double> enterTimes = new HashMap<>();

	private Map<Id<Link>, IterationResult> iterationResults;

	private List<Map<Id<Link>, IterationResult>> results = new LinkedList<>();

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		System.out.println("Starting iteration: " + event.getIteration());

		enterTimes.clear();

		int numBins = (int) (3600 * 30 / binSize);

		iterationResults = new HashMap<>();

		// Router travel times
		for (Id<Link> linkId : linkIds) {
			IterationResult iterationResult = new IterationResult(linkId, binSize, numBins);
			iterationResult.iteration = event.getIteration();
			iterationResults.put(linkId, iterationResult);
		}

		results.add(iterationResults);
	}

	public void startIteration(int iteration) {
		System.out.println("Starting iteration: " + iteration);

		enterTimes.clear();

		int numBins = (int) (3600 * 30 / binSize);

		iterationResults = new HashMap<>();

		// Router travel times
		for (Id<Link> linkId : linkIds) {
			IterationResult iterationResult = new IterationResult(linkId, binSize, numBins);
			iterationResult.iteration = iteration;
			iterationResults.put(linkId, iterationResult);
		}

		results.add(iterationResults);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (iterationResults.containsKey(event.getLinkId())) {
			enterTimes.put(event.getVehicleId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (iterationResults.containsKey(event.getLinkId())) {
			enterTimes.put(event.getVehicleId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (iterationResults.containsKey(event.getLinkId())) {
			double enterTime = enterTimes.remove(event.getVehicleId());

			// get time bin
			int timeBin = (int) (enterTime / binSize);

			// add values
			iterationResults.get(event.getLinkId()).meanTravelTime[timeBin] += event.getTime() - enterTime;
			iterationResults.get(event.getLinkId()).numberOfAgents[timeBin] += 1;
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if (iterationResults.containsKey(event.getLinkId())) {
			double enterTime = enterTimes.remove(event.getVehicleId());

			// get time bin
			int timeBin = (int) (enterTime / binSize);

			// add values
			iterationResults.get(event.getLinkId()).meanTravelTime[timeBin] += event.getTime() - enterTime;
			iterationResults.get(event.getLinkId()).numberOfAgents[timeBin] += 1;
		}
	}

	public List<Map<Id<Link>, IterationResult>> getResults() {
		return results;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// Router travel times
		for (Id<Link> linkId : linkIds) {
			for (int i = 0; i<iterationResults.get(linkId).numBins; i++) {
				if (iterationResults.get(linkId).numberOfAgents[i] > 0) {
					iterationResults.get(linkId).meanTravelTime[i] /= iterationResults.get(linkId).numberOfAgents[i];
				} else {
					// if no one crossed the link, then set to free speed travel time
					iterationResults.get(linkId).meanTravelTime[i] = network.getLinks().get(linkId).getLength() / network.getLinks().get(linkId).getFreespeed();
				}
			}
		}

		System.out.println("Iteration done: " + event.getIteration());
	}

	public void endIteration(int iteration) {
		// Router travel times
		for (Id<Link> linkId : linkIds) {
			for (int i = 0; i<iterationResults.get(linkId).numBins; i++) {
				if (iterationResults.get(linkId).numberOfAgents[i] > 0) {
					iterationResults.get(linkId).meanTravelTime[i] /= iterationResults.get(linkId).numberOfAgents[i];
				} else {
					// if no one crossed the link, then set to free speed travel time
					iterationResults.get(linkId).meanTravelTime[i] = network.getLinks().get(linkId).getLength() / network.getLinks().get(linkId).getFreespeed();
				}
			}
		}

		System.out.println("Iteration done: " + iteration);
	}
}
