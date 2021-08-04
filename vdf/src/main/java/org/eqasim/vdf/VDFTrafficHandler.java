package org.eqasim.vdf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class VDFTrafficHandler implements LinkEnterEventHandler {
	private final VDFTravelTime travelTime;
	private final Network network;
	private final int horizon;

	private final IdMap<Link, List<Integer>> counts = new IdMap<>(Link.class);
	private final List<IdMap<Link, List<Integer>>> history = new LinkedList<>();

	public VDFTrafficHandler(Network network, VDFTravelTime travelTime, int horizon) {
		this.travelTime = travelTime;
		this.network = network;
		this.horizon = horizon;

		reset(0);
	}

	@Override
	public synchronized void handleEvent(LinkEnterEvent event) {
		int i = travelTime.getInterval(event.getTime());
		int currentValue = counts.get(event.getLinkId()).get(i);
		counts.get(event.getLinkId()).set(i, currentValue + 1);
	}

	@Override
	public void reset(int iteration) {
		if (history.size() == horizon) {
			history.remove(0);
		}
		
		// Make a copy to add to the history
		
		IdMap<Link, List<Integer>> copy = new IdMap<>(Link.class);
		
		for (Map.Entry<Id<Link>, List<Integer>> entry : counts.entrySet()) {
			copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
		
		history.add(copy);
		
		IdMap<Link, List<Double>> aggregated = new IdMap<>(Link.class);
		
		for (Id<Link> linkId : network.getLinks().keySet()) {
			// Reset current counts
			counts.put(linkId, new ArrayList<>(Collections.nCopies(travelTime.getNumberOfIntervals(), 0)));
		
			// Initialize aggregated counts
			aggregated.put(linkId, new ArrayList<>(Collections.nCopies(travelTime.getNumberOfIntervals(), 0.0)));
		}
		
		// Aggregate
		
		for (IdMap<Link, List<Integer>> item : history) {
			for (Map.Entry<Id<Link>, List<Integer>> entry : item.entrySet()) {
				List<Double> aggregatedList = aggregated.get(entry.getKey());
				
				for (int i = 0; i < aggregatedList.size(); i++) {
					aggregatedList.set(i, aggregatedList.get(i) + (double) entry.getValue().get(i) / (double) history.size());
				}
			}
		}
		
		travelTime.update(aggregated);
	}
}
