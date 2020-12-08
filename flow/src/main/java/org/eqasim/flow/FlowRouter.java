package org.eqasim.flow;

import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.misc.ParallelProgress;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

class FlowRouter {
	private final LeastCostPathCalculatorFactory factory;
	private final int numberOfThreads;
	private final int batchSize;

	FlowRouter(LeastCostPathCalculatorFactory factory, int numberOfThreads, int batchSize) {
		this.factory = factory;
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
	}

	public List<Result> calculatePaths(List<FlowTrip> trips, Network network, IdMap<Link, Double> travelTimes)
			throws InterruptedException {
		List<Result> results = new LinkedList<>();
		List<FlowTrip> queue = new LinkedList<>(trips);

		List<Thread> threads = new LinkedList<>();
		ParallelProgress progress = new ParallelProgress("Routing ...", queue.size());

		for (int i = 0; i < numberOfThreads; i++) {
			threads.add(new Thread(new Worker(queue, results, progress, network, travelTimes)));
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
		private final List<FlowTrip> queue;
		private final List<Result> results;
		private final ParallelProgress progress;
		private final TravelTime travelTime;
		private final Network network;

		Worker(List<FlowTrip> queue, List<Result> results, ParallelProgress progress, Network network,
				IdMap<Link, Double> travelTimes) {
			this.queue = queue;
			this.results = results;
			this.progress = progress;
			this.network = network;

			this.travelTime = new TravelTime() {
				@Override
				public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
					return travelTimes.get(link.getId());
				}
			};
		}

		@Override
		public void run() {
			List<FlowTrip> tasks = new LinkedList<>();
			LeastCostPathCalculator calculator = factory.createPathCalculator(network,
					new OnlyTimeDependentTravelDisutility(travelTime), travelTime);

			do {
				tasks.clear();

				synchronized (queue) {
					while (queue.size() > 0 && tasks.size() < batchSize) {
						tasks.add(queue.remove(0));
					}
				}

				List<Result> taskResults = new LinkedList<>();

				for (FlowTrip trip : tasks) {
					Node fromNode = trip.originLink.getToNode();
					Node toNode = trip.destinationLink.getFromNode();

					Path path = calculator.calcLeastCostPath(fromNode, toNode, 0.0, null, null);
					taskResults.add(new Result(trip, path));
				}

				synchronized (results) {
					results.addAll(taskResults);
					progress.update(taskResults.size());
				}
			} while (tasks.size() > 0);
		}
	}

	public class Result {
		final public FlowTrip trip;
		final public Path path;

		Result(FlowTrip trip, Path path) {
			this.trip = trip;
			this.path = path;
		}
	}
}
