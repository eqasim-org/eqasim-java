package org.eqasim.core.tools.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.misc.ParallelProgress;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Provider;

public class BatchRoadRouter {
	private final Provider<LeastCostPathCalculatorFactory> routerFactoryProvider;
	private final Network network;

	private final int batchSize;
	private final int numberOfThreads;
	private final boolean writePaths;

	public BatchRoadRouter(Provider<LeastCostPathCalculatorFactory> routerFactoryProvider, Network network,
			int batchSize, int numberOfThreads, boolean writePaths) {
		this.routerFactoryProvider = routerFactoryProvider;
		this.batchSize = batchSize;
		this.numberOfThreads = numberOfThreads;
		this.network = network;
		this.writePaths = writePaths;
	}

	public Collection<Result> run(Collection<Task> tasks) throws InterruptedException {
		Iterator<Task> taskIterator = tasks.iterator();
		List<Result> results = new ArrayList<>(tasks.size());

		ParallelProgress progress = new ParallelProgress("Routing trips ...", tasks.size());
		progress.start();

		List<Thread> threads = new ArrayList<>(numberOfThreads);

		for (int i = 0; i < numberOfThreads; i++) {
			Thread thread = new Thread(new Worker(taskIterator, results, progress));
			threads.add(thread);
			thread.start();
		}

		for (Thread thread : threads) {
			thread.join();
		}

		progress.close();
		return results;
	}

	private class Worker implements Runnable {
		private final Iterator<Task> taskIterator;
		private final Collection<Result> results;
		private final ParallelProgress progress;

		private Worker(Iterator<Task> taskIterator, Collection<Result> results, ParallelProgress progress) {
			this.taskIterator = taskIterator;
			this.results = results;
			this.progress = progress;
		}

		@Override
		public void run() {
			LeastCostPathCalculatorFactory factory = routerFactoryProvider.get();

			TravelTime travelTime = new FreeSpeedTravelTime();
			TravelDisutility travelDisutility = new OnlyTimeDependentTravelDisutility(travelTime);

			LeastCostPathCalculator router = factory.createPathCalculator(network, travelDisutility, travelTime);

			while (true) {
				List<Task> localTasks = new ArrayList<>(batchSize);

				synchronized (taskIterator) {
					while (taskIterator.hasNext() && localTasks.size() < batchSize) {
						localTasks.add(taskIterator.next());
					}

					if (localTasks.size() == 0) {
						return;
					}
				}

				List<Result> localResults = new ArrayList<>(localTasks.size());

				for (Task task : localTasks) {
					Result result = new Result(task);

					Coord fromCoord = new Coord(task.originX, task.originY);
					Coord toCoord = new Coord(task.destinationX, task.destinationY);

					Link fromLink = NetworkUtils.getNearestLink(network, fromCoord);
					Link toLink = NetworkUtils.getNearestLink(network, toCoord);

					Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(), task.departureTime,
							null, null);

					result.inVehicleTime_min = path.travelTime / 60.0;
					result.inVehicleDistance_km = path.links.stream().mapToDouble(Link::getLength).sum() * 1e-3;

					result.accessEuclideanDistance_km = CoordUtils.calcEuclideanDistance(fromCoord,
							fromLink.getToNode().getCoord()) * 1e-3;
					result.egressEuclideanDistance_km = CoordUtils.calcEuclideanDistance(toCoord,
							toLink.getFromNode().getCoord()) * 1e-3;

					if (writePaths) {
						path.links.forEach(link -> result.path.add(link.getId().toString()));
					}

					localResults.add(result);
					progress.update();
				}

				synchronized (results) {
					results.addAll(localResults);
				}
			}
		}
	}

	static public class Task {
		@JsonProperty("identifier")
		public String identifier;

		@JsonProperty("origin_x")
		public double originX;

		@JsonProperty("origin_y")
		public double originY;

		@JsonProperty("destination_x")
		public double destinationX;

		@JsonProperty("destination_y")
		public double destinationY;

		@JsonProperty("departure_time")
		public double departureTime;
	}

	static public class Result {
		@JsonProperty("identifier")
		public String identifier;

		@JsonProperty("access_euclidean_distance_km")
		public double accessEuclideanDistance_km;

		@JsonProperty("egress_euclidean_distance_km")
		public double egressEuclideanDistance_km;

		@JsonProperty("in_vehicle_time_min")
		public double inVehicleTime_min;

		@JsonProperty("in_vehicle_distance_km")
		public double inVehicleDistance_km;

		@JsonProperty("path")
		public List<String> path = new LinkedList<>();

		Result(Task task) {
			this.identifier = task.identifier;
		}
	}
}
