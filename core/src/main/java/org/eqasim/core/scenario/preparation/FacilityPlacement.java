package org.eqasim.core.scenario.preparation;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.eqasim.core.misc.ParallelProgress;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

public class FacilityPlacement {
	private final int numberOfThreads;
	private final int batchSize;
	private final QuadTree<Link> spatialIndex;

	public FacilityPlacement(int numberOfThreads, int batchSize, RoadNetwork network, FacilityPlacementVoter voter) {
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;

		this.spatialIndex = QuadTrees.createQuadTree(
				network.getLinks().values().stream().filter(voter::canPlaceFacility).collect(Collectors.toList()));
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
					Link link = spatialIndex.getClosest(facility.getCoord().getX(), facility.getCoord().getY());
					((ActivityFacilityImpl) facility).setLinkId(link.getId());
				}

				progress.update(localTasks.size());
			} while (localTasks.size() > 0);
		}
	}

	static public interface FacilityPlacementVoter {
		boolean canPlaceFacility(Link link);
	}

	static public class OSMFacilityPlacementVoter implements FacilityPlacementVoter {
		private final static String HIGHWAY_TAG = "osm:way:highway";
		
		public OSMFacilityPlacementVoter(RoadNetwork network) {
			boolean foundAttribute = false;

			for (Link link : network.getLinks().values()) {
				if (link.getAttributes().getAttribute(HIGHWAY_TAG) != null) {
					foundAttribute = true;
					break;
				}
			}

			if (!foundAttribute) {
				throw new IllegalStateException("Did not find osm:highway attribute in network");
			}
		}

		@Override
		public boolean canPlaceFacility(Link link) {
			String highway = (String) link.getAttributes().getAttribute(HIGHWAY_TAG);

			if (highway != null) {
				if (highway.contains("motorway")) {
					return false;
				}

				if (highway.contains("trunk")) {
					return false;
				}
				
				if (highway.contains("_link")) {
					return false;
				}
			}

			return true;
		}
	}
}
