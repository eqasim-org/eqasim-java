package org.eqasim.core.scenario.cutter.network;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.eqasim.core.misc.ParallelProgress;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class MinimumNetworkFinder {
	private Link referenceLink;
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
		return run(linkIds, TransportMode.car);
	}

	public Set<Id<Link>> run(Set<Id<Link>> linkIds, String mode) throws InterruptedException {
		Iterator<Id<Link>> linkIterator = linkIds.iterator();

		List<Thread> threads = new LinkedList<>();

		ParallelProgress progress = new ParallelProgress("Finding minimum network ...", linkIds.size());
		progress.start();

		Set<Id<Link>> minimumSet = Collections.synchronizedSet(new HashSet<>());

		LeastCostPathCalculatorFactory factory = new SpeedyALTFactory();

		for (int i = 0; i < numberOfThreads; i++) {
			Thread thread = new Thread(new Worker(linkIterator, progress, minimumSet, factory, mode));
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
		private final Vehicle vehicle;

		Worker(Iterator<Id<Link>> linkIterator, ParallelProgress progress, Set<Id<Link>> minimumSet,
			   LeastCostPathCalculatorFactory routerFactory) {
			this(linkIterator, progress, minimumSet, routerFactory, TransportMode.car);
		}

		Worker(Iterator<Id<Link>> linkIterator, ParallelProgress progress, Set<Id<Link>> minimumSet,
				LeastCostPathCalculatorFactory routerFactory, String mode) {
			this.linkIterator = linkIterator;
			this.progress = progress;
			this.minimumSet = minimumSet;
			this.routerFactory = routerFactory;
			this.vehicle = createRandomVehicle(mode);
		}

		private Vehicle createRandomVehicle(String mode) {
			long n = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);

			VehicleType type = VehicleUtils.createVehicleType(
					Id.create("cutterType:" + mode, VehicleType.class));
			type.setNetworkMode(mode);

			return VehicleUtils.createVehicle(
					Id.createVehicleId("cutterVehicle:" + mode + ":" + n),
					type);
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

						Path result = calculator.calcLeastCostPath(testLink, referenceLink,
								0.0, null, vehicle);

						result.links.forEach(l -> forwardTabuSet.add(l.getId()));
					}

					if (!backwardTabuSet.contains(testLinkId)) {
						Path result = calculator.calcLeastCostPath(referenceLink, testLink,
								0.0, null, vehicle);

						result.links.forEach(l -> backwardTabuSet.add(l.getId()));
					}
				}

				progress.update(localTasks.size());

				minimumSet.addAll(forwardTabuSet);
				minimumSet.addAll(backwardTabuSet);
			} while (localTasks.size() > 0);
		}
	}

	public void referenceLinkShouldContainMode(String mode) {
		if (!referenceLink.getAllowedModes().contains(mode)){
			Coord interiorPoint = referenceLink.getToNode().getCoord();
			Link nearestLink = null;
			double shortestDistance = Double.MAX_VALUE;

			for (Link l: network.getLinks().values()) {
				if (l.getAllowedModes().contains(mode)) {
					double distance = CoordUtils.calcEuclideanDistance(interiorPoint, l.getToNode().getCoord());

					if (distance < shortestDistance) {
						shortestDistance = distance;
						nearestLink = l;
					}
				}
			}
			setReferenceLink(nearestLink);
		}
	}

	private void setReferenceLink(Link link) {
		this.referenceLink = link;
	}
}
