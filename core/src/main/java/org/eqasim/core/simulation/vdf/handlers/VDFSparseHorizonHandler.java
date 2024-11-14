package org.eqasim.core.simulation.vdf.handlers;

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
import org.eqasim.core.simulation.vdf.VDFScope;
import org.eqasim.core.simulation.vdf.io.VDFReaderInterface;
import org.eqasim.core.simulation.vdf.io.VDFWriterInterface;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;

import com.google.common.base.Verify;

public class VDFSparseHorizonHandler implements VDFTrafficHandler, LinkEnterEventHandler {
	private final VDFScope scope;

	private final Network network;
	private final int horizon;
	private final int numberOfThreads;

	private final IdMap<Link, List<Double>> counts = new IdMap<>(Link.class);

	private final static Logger logger = LogManager.getLogger(VDFSparseHorizonHandler.class);

	private record LinkState(List<Integer> time, List<Double> count) {
	}

	private List<IdMap<Link, LinkState>> state = new LinkedList<>();

	public VDFSparseHorizonHandler(Network network, VDFScope scope, int horizon, int numberOfThreads) {
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
	public IdMap<Link, List<Double>> aggregate(boolean ignoreIteration) {
		while (state.size() > horizon) {
			state.remove(0);
		}

		logger.info(String.format("Starting aggregation of %d slices", state.size()));

		// Transform counts into state object
		if (!ignoreIteration) {
			IdMap<Link, LinkState> newState = new IdMap<>(Link.class);
			state.add(newState);

			for (Map.Entry<Id<Link>, List<Double>> entry : counts.entrySet()) {
				double total = 0.0;

				for (double value : entry.getValue()) {
					total += value;
				}

				if (total > 0.0) {
					LinkState linkState = new LinkState(new ArrayList<>(), new ArrayList<>());
					newState.put(entry.getKey(), linkState);

					int timeIndex = 0;
					for (double count : entry.getValue()) {
						if (count > 0.0) {
							linkState.time.add(timeIndex);
							linkState.count.add(count);
						}

						timeIndex++;
					}
				}
			}
		}

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
					LinkState historyItem = state.get(k).get(currentLinkId);
					List<Double> linkAggregator = aggregated.get(currentLinkId);

					if (historyItem != null) {
						for (int i = 0; i < historyItem.count.size(); i++) {
							int timeIndex = historyItem.time.get(i);
							linkAggregator.set(timeIndex,
									linkAggregator.get(timeIndex) + historyItem.count.get(i) / (double) state.size());
						}
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
					IdMap<Link, LinkState> slice = new IdMap<>(Link.class);
					state.add(slice);

					int sliceLinkCount = inputStream.readInt();

					logger.info(String.format("Slice %d/%d, Reading %d link states", sliceIndex+1, slices, sliceLinkCount));

					for (int sliceLinkIndex = 0; sliceLinkIndex < sliceLinkCount; sliceLinkIndex++) {
						int linkIndex = inputStream.readInt();
						int linkStateSize = inputStream.readInt();

						LinkState linkState = new LinkState(new ArrayList<>(linkStateSize),
								new ArrayList<>(linkStateSize));
						slice.put(linkIds.get(linkIndex), linkState);

						for (int i = 0; i < linkStateSize; i++) {
							linkState.time.add(inputStream.readInt());
							linkState.count.add(inputStream.readDouble());
						}
					}

					logger.info(String.format("  Slice %d: %d obs", sliceIndex,
							sliceLinkCount));
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

				logger.info(String.format("About to write %d slices", state.size()));

				for (int sliceIndex = 0; sliceIndex < state.size(); sliceIndex++) {
					IdMap<Link, LinkState> slice = state.get(sliceIndex);
					outputStream.writeInt(slice.size());

					int sliceLinkIndex = 0;
					for (Id<Link> linkId : linkIds) {
						LinkState linkState = slice.get(linkId);
						if(linkState == null) {
							continue;
						}
						outputStream.writeInt(linkIds.indexOf(linkId));
						outputStream.writeInt(linkState.count.size());

						for (int i = 0; i < linkState.count.size(); i++) {
							outputStream.writeInt(linkState.time.get(i));
							outputStream.writeDouble(linkState.count.get(i));
						}
						sliceLinkIndex += 1;
					}
					assert sliceLinkIndex == slice.size();
				}

				outputStream.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
