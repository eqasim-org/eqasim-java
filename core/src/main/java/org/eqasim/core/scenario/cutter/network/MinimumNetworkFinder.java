package org.eqasim.core.scenario.cutter.network;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eqasim.core.misc.ParallelProgress;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.AStarLandmarksFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

public class MinimumNetworkFinder {
	private final Link referenceLink;
	private final RoadNetwork network;
	private final int numberOfThreads;
	private final int batchSize;

	public MinimumNetworkFinder(ScenarioExtent extent, RoadNetwork network, int numberOfThreads, int batchSize) {
		this.network = network;
		this.numberOfThreads = numberOfThreads;
		this.referenceLink = NetworkUtils.getNearestLink(network, extent.getInteriorPoint());
		this.batchSize = batchSize;
	}

	public Set<Id<Link>> run(Set<Id<Link>> linkIds) throws InterruptedException {
		Iterator<Id<Link>> linkIterator = linkIds.iterator();

		List<Thread> threads = new LinkedList<>();

		ParallelProgress progress = new ParallelProgress("Finding minimum network ...", linkIds.size());
		progress.start();

		Set<Id<Link>> minimumSet = Collections.synchronizedSet(new HashSet<>());

		LeastCostPathCalculatorFactory factory = new AStarLandmarksFactory(numberOfThreads);

		for (int i = 0; i < numberOfThreads; i++) {
			Thread thread = new Thread(new Worker(linkIterator, progress, minimumSet, factory));
			thread.start();
			threads.add(thread);
		}

		for (Thread thread : threads) {
			thread.join();
		}

		progress.close();

		return minimumSet;
	}

	private class Worker implements Runnable {
		private final Iterator<Id<Link>> linkIterator;
		private final ParallelProgress progress;
		private final LeastCostPathCalculatorFactory routerFactory;
		private final Set<Id<Link>> minimumSet;

		Worker(Iterator<Id<Link>> linkIterator, ParallelProgress progress, Set<Id<Link>> minimumSet,
				LeastCostPathCalculatorFactory routerFactory) {
			this.linkIterator = linkIterator;
			this.progress = progress;
			this.minimumSet = minimumSet;
			this.routerFactory = routerFactory;
		}

		@Override
		public void run() {
			List<Id<Link>> localTasks = new LinkedList<>();

			TravelTime travelTime = new FreeSpeedTravelTime();
			TravelDisutility travelDisutility = new OnlyTimeDependentTravelDisutility(travelTime);
			LeastCostPathCalculator calculator = routerFactory.createPathCalculator(network, travelDisutility,
					travelTime);

			Set<Id<Link>> forwardTabuSet = new HashSet<>();
			Set<Id<Link>> backwardTabuSet = new HashSet<>();

			do {
				localTasks.clear();

				synchronized (linkIterator) {
					while (linkIterator.hasNext() && localTasks.size() < batchSize) {
						localTasks.add(linkIterator.next());
					}
				}

				for (Id<Link> testLinkId : localTasks) {
					Link testLink = network.getLinks().get(testLinkId);

					if (testLink == null) {
						throw new IllegalStateException("Cannot find link " + testLinkId);
					}

					if (!forwardTabuSet.contains(testLinkId)) {
						Path result = calculator.calcLeastCostPath(testLink.getToNode(), referenceLink.getFromNode(),
								0.0, null, null);

						result.links.forEach(l -> forwardTabuSet.add(l.getId()));
					}

					if (!backwardTabuSet.contains(testLinkId)) {
						Path result = calculator.calcLeastCostPath(referenceLink.getToNode(), testLink.getFromNode(),
								0.0, null, null);

						result.links.forEach(l -> backwardTabuSet.add(l.getId()));
					}
				}

				progress.update(localTasks.size());

				minimumSet.addAll(forwardTabuSet);
				minimumSet.addAll(backwardTabuSet);
			} while (localTasks.size() > 0);
		}
	}
}
