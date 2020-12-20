package org.eqasim.odyssee;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eqasim.core.misc.ParallelProgress;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.Facility;

import com.google.inject.Injector;

class BatchRouter {
	private final Injector injector;
	private final int numberOfThreads;
	private final int batchSize;
	private final boolean saveLinks;

	public BatchRouter(int numberOfThreads, int batchSize, boolean saveLinks, Injector injector) {
		this.injector = injector;
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
		this.saveLinks = saveLinks;
	}

	public List<RoutingResult> process(List<RoutingTask> tasks) throws InterruptedException {
		List<RoutingResult> results = new LinkedList<>();
		List<RoutingTask> queue = new LinkedList<>(tasks);

		List<Thread> threads = new LinkedList<>();
		ParallelProgress progress = new ParallelProgress("Routing ...", queue.size());

		for (int i = 0; i < numberOfThreads; i++) {
			TripRouter router = injector.getInstance(TripRouter.class);
			Scenario scenario = injector.getInstance(Scenario.class);

			threads.add(new Thread(new Worker(queue, results, progress, scenario, router)));
		}

		progress.start();

		for (Thread thread : threads) {
			thread.start();
		}

		for (Thread thread : threads) {
			thread.join();
		}

		progress.close();

		return results;
	}

	private class Worker implements Runnable {
		private final List<RoutingTask> queue;
		private final List<RoutingResult> results;
		private final ParallelProgress progress;

		private final TripRouter router;
		private final Network roadNetwork;

		Worker(List<RoutingTask> queue, List<RoutingResult> results, ParallelProgress progress, Scenario scenario,
				TripRouter router) {
			this.queue = queue;
			this.results = results;
			this.progress = progress;
			this.router = router;

			this.roadNetwork = NetworkUtils.createNetwork();
			new TransportModeNetworkFilter(scenario.getNetwork()).filter(roadNetwork, Collections.singleton("car"));
		}

		@Override
		public void run() {
			List<RoutingTask> tasks = new LinkedList<>();

			do {
				tasks.clear();

				synchronized (queue) {
					while (queue.size() > 0 && tasks.size() < batchSize) {
						tasks.add(queue.remove(0));
					}
				}

				List<RoutingResult> taskResults = new LinkedList<>();

				for (RoutingTask task : tasks) {
					Link originLink = NetworkUtils.getNearestLink(roadNetwork, new Coord(task.originX, task.originY));
					Link destinationLink = NetworkUtils.getNearestLink(roadNetwork,
							new Coord(task.destinationX, task.destinationY));

					Facility originFacility = new LinkWrapperFacility(originLink);
					Facility destinationFacility = new LinkWrapperFacility(destinationLink);

					List<? extends PlanElement> carPlan = router.calcRoute("car", originFacility, destinationFacility,
							9.0 * 3600.0, null);

					double carTravelTime = 0.0;
					double carDistance = 0.0;
					Set<Id<Link>> linkIds = new HashSet<>();

					for (Leg leg : TripStructureUtils.getLegs(carPlan)) {
						carTravelTime += leg.getTravelTime().seconds();
						carDistance += leg.getRoute().getDistance();

						if (saveLinks) {
							NetworkRoute route = (NetworkRoute) leg.getRoute();
							linkIds.add(route.getStartLinkId());
							linkIds.add(route.getEndLinkId());
							linkIds.addAll(route.getLinkIds());
						}
					}

					List<? extends PlanElement> ptPlan = router.calcRoute("pt", originFacility, destinationFacility,
							9.0 * 3600.0, null);

					double ptTravelTime = 0.0;
					double ptDistance = 0.0;

					for (Leg leg : TripStructureUtils.getLegs(ptPlan)) {
						ptTravelTime += leg.getTravelTime().seconds();
						ptDistance += leg.getRoute().getDistance();
					}

					taskResults.add(new RoutingResult(task.personId, task.officeId, carTravelTime, carDistance,
							ptTravelTime, ptDistance, linkIds));
				}

				synchronized (results) {
					results.addAll(taskResults);
					progress.update(taskResults.size());
				}
			} while (tasks.size() > 0);
		}
	}
}
