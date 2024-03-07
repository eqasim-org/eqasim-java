package org.eqasim.vdf.handlers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.vdf.VDFScope;
import org.eqasim.vdf.io.VDFReaderInterface;
import org.eqasim.vdf.io.VDFWriterInterface;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;

import com.google.common.base.Verify;

public class VDFHorizonHandler implements VDFTrafficHandler, LinkEnterEventHandler {
	private final VDFScope scope;

	private final Network network;
	private final int horizon;
	private final int numberOfThreads;

	private final IdMap<Link, List<Double>> counts = new IdMap<>(Link.class);
	private final List<IdMap<Link, List<Double>>> state = new LinkedList<>();

	private final static Logger logger = LogManager.getLogger(VDFHorizonHandler.class);

	public VDFHorizonHandler(Network network, VDFScope scope, int horizon, int numberOfThreads) {
		this.scope = scope;
		this.network = network;
		this.horizon = horizon;
		this.numberOfThreads = numberOfThreads;

		for (Id<Link> linkId : network.getLinks().keySet()) {
			counts.put(linkId, new ArrayList<>(Collections.nCopies(scope.getIntervals(), 0.0)));
		}
	}

	@Override
	public synchronized void handleEvent(LinkEnterEvent event) {
		processEnterLink(event.getTime(), event.getLinkId());
	}

	public void processEnterLink(double time, Id<Link> linkId) {
		int i = scope.getIntervalIndex(time);
		double currentValue = counts.get(linkId).get(i);
		counts.get(linkId).set(i, currentValue + 1);
	}

	@Override
	public IdMap<Link, List<Double>> aggregate() {
		while (state.size() > horizon) {
			state.remove(0);
		}

		logger.info(String.format("Starting aggregation of %d slices", state.size()));

		// Make a copy to add to the history

		IdMap<Link, List<Double>> copy = new IdMap<>(Link.class);

		for (Map.Entry<Id<Link>, List<Double>> entry : counts.entrySet()) {
			copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}

		state.add(copy);

		IdMap<Link, List<Double>> aggregated = new IdMap<>(Link.class);

		for (Id<Link> linkId : network.getLinks().keySet()) {
			// Reset current counts
			counts.put(linkId, new ArrayList<>(Collections.nCopies(scope.getIntervals(), 0.0)));

			// Initialize aggregated counts
			aggregated.put(linkId, new ArrayList<>(Collections.nCopies(scope.getIntervals(), 0.0)));
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
				for (int k = 0; k < state.size(); k++) {
					IdMap<Link, List<Double>> historyItem = state.get(k);
					List<Double> linkValues = historyItem.get(currentLinkId);
					List<Double> linkAggregator = aggregated.get(currentLinkId);

					for (int i = 0; i < linkValues.size(); i++) {
						linkAggregator.set(i,
								linkAggregator.get(i) + (double) linkValues.get(i) / (double) state.size());
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

		logger.info(String.format("  Finished aggregation"));

		return aggregated;
	}

	@Override
	public VDFReaderInterface getReader() {
		return new Reader();
	}

	@Override
	public VDFWriterInterface getWriter() {
		return new Writer();
	}

	public class Reader implements VDFReaderInterface {
		@Override
		public void readFile(URL inputFile) {
			state.clear();

			try {
				DataInputStream inputStream = new DataInputStream(IOUtils.getInputStream(inputFile));

				Verify.verify(inputStream.readDouble() == scope.getStartTime());
				Verify.verify(inputStream.readDouble() == scope.getEndTime());
				Verify.verify(inputStream.readDouble() == scope.getIntervalTime());
				Verify.verify(inputStream.readInt() == scope.getIntervals());
				Verify.verify(inputStream.readInt() == horizon);

				int slices = (int) inputStream.readInt();
				int links = (int) inputStream.readInt();

				List<Id<Link>> linkIds = new ArrayList<>(links);
				for (int linkIndex = 0; linkIndex < links; linkIndex++) {
					linkIds.add(Id.createLinkId(inputStream.readUTF()));
				}

				logger.info(String.format("Loading %d slices with %d links", slices, links));

				for (int sliceIndex = 0; sliceIndex < slices; sliceIndex++) {
					IdMap<Link, List<Double>> slice = new IdMap<>(Link.class);
					state.add(slice);

					double totalLinkValue = 0.0;
					double maximumLinkValue = 0.0;

					for (int linkIndex = 0; linkIndex < links; linkIndex++) {
						List<Double> linkValues = new LinkedList<>();

						Id<Link> linkId = linkIds.get(linkIndex);
						slice.put(linkId, linkValues);

						for (int valueIndex = 0; valueIndex < scope.getIntervals(); valueIndex++) {
							double linkValue = inputStream.readDouble();

							linkValues.add(linkValue);
							totalLinkValue += linkValue;
							maximumLinkValue = Math.max(maximumLinkValue, linkValue);
						}
					}

					logger.info(String.format("  Slice %d: avg. value %f; max. value %f", sliceIndex,
							totalLinkValue / links, maximumLinkValue));
				}

				Verify.verify(inputStream.available() == 0);
				inputStream.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public class Writer implements VDFWriterInterface {
		@Override
		public void writeFile(File outputFile) {
			try {
				DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(outputFile.toString()));

				outputStream.writeDouble(scope.getStartTime());
				outputStream.writeDouble(scope.getEndTime());
				outputStream.writeDouble(scope.getIntervalTime());
				outputStream.writeInt(scope.getIntervals());
				outputStream.writeInt(horizon);
				outputStream.writeInt(state.size());
				outputStream.writeInt(counts.size());

				List<Id<Link>> linkIds = new ArrayList<>(counts.keySet());
				for (int linkIndex = 0; linkIndex < linkIds.size(); linkIndex++) {
					outputStream.writeUTF(linkIds.get(linkIndex).toString());
				}

				for (int sliceIndex = 0; sliceIndex < state.size(); sliceIndex++) {
					IdMap<Link, List<Double>> slice = state.get(sliceIndex);

					for (Id<Link> linkId : linkIds) {
						List<Double> linkValues = slice.get(linkId);

						for (int valueIndex = 0; valueIndex < scope.getIntervals(); valueIndex++) {
							outputStream.writeDouble(linkValues.get(valueIndex));
						}
					}
				}

				outputStream.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
