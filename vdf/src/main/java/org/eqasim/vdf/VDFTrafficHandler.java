package org.eqasim.vdf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
	private final int numberOfThreads;

	private final IdMap<Link, List<Integer>> counts = new IdMap<>(Link.class);
	private final List<IdMap<Link, List<Integer>>> history = new LinkedList<>();

	public VDFTrafficHandler(Network network, VDFTravelTime travelTime, int horizon, int numberOfThreads) {
		this.travelTime = travelTime;
		this.network = network;
		this.horizon = horizon;
		this.numberOfThreads = numberOfThreads;

		reset(0);
	}

	@Override
	public synchronized void handleEvent(LinkEnterEvent event) {
		processEnterLink(event.getTime(), event.getLinkId());
	}

	public void processEnterLink(double time, Id<Link> linkId) {
		int i = travelTime.getInterval(time);
		int currentValue = counts.get(linkId).get(i);
		counts.get(linkId).set(i, currentValue + 1);
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
		Iterator<Id<Link>> linkIterator = network.getLinks().keySet().iterator();

		Runnable worker = () -> {
			Id<Link> currentLinkId = null;

			while (true) {
				// Fetch new link in queue
				synchronized (linkIterator) {
					if (linkIterator.hasNext()) {
						currentLinkId = linkIterator.next();
					} else {
						break; // Done
					}
				}

				// Go through history for this link and aggregate by time slot
				for (int k = 0; k < history.size(); k++) {
					IdMap<Link, List<Integer>> historyItem = history.get(k);
					List<Integer> linkValues = historyItem.get(currentLinkId);
					List<Double> linkAggregator = aggregated.get(currentLinkId);

					for (int i = 0; i < linkValues.size(); i++) {
						linkAggregator.set(i,
								linkAggregator.get(i) + (double) linkValues.get(i) / (double) history.size());
					}
				}
			}
		};

		if (numberOfThreads < 2) {
			worker.run();
		} else {
			List<Thread> threads = new ArrayList<>(numberOfThreads);

			for (int k = 0; k < numberOfThreads; k++) {
				threads.add(new Thread(worker));
			}

			for (int k = 0; k < numberOfThreads; k++) {
				threads.get(k).start();
			}

			try {
				for (int k = 0; k < numberOfThreads; k++) {
					threads.get(k).join();
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		travelTime.update(aggregated);
	}
}
