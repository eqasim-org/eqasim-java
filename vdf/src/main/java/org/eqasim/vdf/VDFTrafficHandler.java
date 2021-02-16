package org.eqasim.vdf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class VDFTrafficHandler implements LinkEnterEventHandler {
	private final VDFTravelTime travelTime;
	private final Network network;

	private final IdMap<Link, List<Integer>> counts = new IdMap<>(Link.class);

	public VDFTrafficHandler(Network network, VDFTravelTime travelTime) {
		this.travelTime = travelTime;
		this.network = network;

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
		travelTime.update(counts);
		
		for (Id<Link> linkId : network.getLinks().keySet()) {
			counts.put(linkId, new ArrayList<>(Collections.nCopies(travelTime.getNumberOfIntervals(), 0)));
		}
	}
}
