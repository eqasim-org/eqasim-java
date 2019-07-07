package org.eqasim.core.scenario.preparation;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.misc.ParallelProgress;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

public class FacilityPlacement {
	private final int numberOfThreads;
	private final int batchSize;
	private final RoadNetwork network;

	public FacilityPlacement(int numberOfThreads, int batchSize, RoadNetwork network) {
		this.network = network;
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
	}

	public void run(ActivityFacilities facilities) throws InterruptedException {
		Iterator<? extends ActivityFacility> facilityIterator = facilities.getFacilities().values().iterator();

		List<Thread> threads = new LinkedList<>();

		ParallelProgress progress = new ParallelProgress("Assigning links to facilities ...",
				facilities.getFacilities().size());

		for (int i = 0; i < numberOfThreads; i++) {
			Thread thread = new Thread(new Worker(facilityIterator, progress));
			threads.add(thread);
		}

		threads.forEach(Thread::start);
		progress.start();

		for (Thread thread : threads) {
			thread.join();
		}

		progress.close();
	}

	private class Worker implements Runnable {
		private final Iterator<? extends ActivityFacility> iterator;
		private final ParallelProgress progress;

		Worker(Iterator<? extends ActivityFacility> iterator, ParallelProgress progress) {
			this.iterator = iterator;
			this.progress = progress;
		}

		@Override
		public void run() {
			List<ActivityFacility> localTasks = new LinkedList<>();

			do {
				localTasks.clear();

				synchronized (iterator) {
					while (iterator.hasNext() && localTasks.size() < batchSize) {
						localTasks.add(iterator.next());
					}
				}

				for (ActivityFacility facility : localTasks) {
					Link link = NetworkUtils.getNearestLink(network, facility.getCoord());
					((ActivityFacilityImpl) facility).setLinkId(link.getId());
				}

				progress.update(localTasks.size());
			} while (localTasks.size() > 0);
		}
	}
}
