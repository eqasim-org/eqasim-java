package org.eqasim.ile_de_france.analysis.pt_routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eqasim.core.components.headway.HeadwayCalculator;
import org.eqasim.core.components.transit.routing.EnrichedTransitRoute;
import org.eqasim.core.components.transit.routing.EnrichedTransitRouter;
import org.eqasim.core.misc.ParallelProgress;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Provider;

public class BatchRouter {
	private final Provider<EnrichedTransitRouter> routerProvider;
	private final Provider<HeadwayCalculator> headwayCalculatorProvider;
	private final TransitSchedule schedule;
	private final Network network;

	private final int batchSize;
	private final int numberOfThreads;
	private final double interval;

	public BatchRouter(Provider<EnrichedTransitRouter> routerProvider,
			Provider<HeadwayCalculator> headwayCalculatorProvider, TransitSchedule schedule, Network network,
			int batchSize, int numberOfThreads, double interval) {
		this.routerProvider = routerProvider;
		this.headwayCalculatorProvider = headwayCalculatorProvider;
		this.batchSize = batchSize;
		this.numberOfThreads = numberOfThreads;
		this.schedule = schedule;
		this.network = network;
		this.interval = interval;
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
			EnrichedTransitRouter router = routerProvider.get();
			HeadwayCalculator headwayCalculator = headwayCalculatorProvider.get();

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

					Facility fromFacility = new LinkWrapperFacility(NetworkUtils.getNearestLink(network, fromCoord));
					Facility toFacility = new LinkWrapperFacility(NetworkUtils.getNearestLink(network, toCoord));

					List<Leg> legs = router.calculateRoute(fromFacility, toFacility, task.departureTime, null);

					boolean isFirstVehicularLeg = true;
					result.isOnlyWalk = 1;
					
					if (interval > 0.0) {
						result.headway_min = headwayCalculator.calculateHeadway_min(fromFacility, toFacility,
								task.departureTime);
					} else {
						result.headway_min = Double.NaN;
					}

					for (Leg leg : legs) {
						if (leg.getMode().equals(TransportMode.access_walk)) {
							result.accessTravelTime_min += leg.getTravelTime() / 60.0;
							result.accessDistance_km += leg.getRoute().getDistance() * 1e-3;
						} else if (leg.getMode().equals(TransportMode.egress_walk)) {
							result.egressTravelTime_min += leg.getTravelTime() / 60.0;
							result.egressDistance_km += leg.getRoute().getDistance() * 1e-3;
						} else if (leg.getMode().equals(TransportMode.transit_walk)) {
							result.transferTravelTime_min += leg.getTravelTime() / 60.0;
							result.transferDistance_km += leg.getRoute().getDistance() * 1e-3;
						} else if (leg.getMode().equals(TransportMode.pt)) {
							EnrichedTransitRoute route = (EnrichedTransitRoute) leg.getRoute();

							if (isFirstVehicularLeg) {
								result.initialWaitingTime_min += route.getWaitingTime() / 60.0;
								isFirstVehicularLeg = false;
							} else {
								result.numberOfTransfers += 1;
								result.transferWaitingTime_min += route.getWaitingTime() / 60.0;
							}

							String mode = schedule.getTransitLines().get(route.getTransitLineId()).getRoutes()
									.get(route.getTransitRouteId()).getTransportMode();

							switch (mode) {
							case "rail":
								result.inVehicleTimeRail_min += route.getInVehicleTime() / 60.0;
								result.inVehicleDistanceRail_km += route.getDistance() * 1e-3;
								break;
							case "subway":
								result.inVehicleTimeSubway_min += route.getInVehicleTime() / 60.0;
								result.inVehicleDistanceSubway_km += route.getDistance() * 1e-3;
								break;
							case "bus":
								result.inVehicleTimeBus_min += route.getInVehicleTime() / 60.0;
								result.inVehicleDistanceBus_km += route.getDistance() * 1e-3;
								break;
							case "tram":
								result.inVehicleTimeTram_min += route.getInVehicleTime() / 60.0;
								result.inVehicleDistanceTram_km += route.getDistance() * 1e-3;
								break;
							default:
								result.inVehicleTimeOther_min += route.getInVehicleTime() / 60.0;
								result.inVehicleDistanceOther_km += route.getDistance() * 1e-3;
							}

							result.isOnlyWalk = 0;
						} else {
							throw new IllegalStateException();
						}
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
		public String identifier;

		public double originX;
		public double originY;

		public double destinationX;
		public double destinationY;

		public double departureTime;
	}

	static public class Result {
		public String identifier;

		public double accessTravelTime_min;
		public double accessDistance_km;

		public double egressTravelTime_min;
		public double egressDistance_km;

		public double inVehicleTimeRail_min;
		public double inVehicleTimeSubway_min;
		public double inVehicleTimeBus_min;
		public double inVehicleTimeTram_min;
		public double inVehicleTimeOther_min;

		public double inVehicleDistanceRail_km;
		public double inVehicleDistanceSubway_km;
		public double inVehicleDistanceBus_km;
		public double inVehicleDistanceTram_km;
		public double inVehicleDistanceOther_km;

		public int numberOfTransfers;

		public double transferTravelTime_min;
		public double transferDistance_km;

		public double initialWaitingTime_min;
		public double transferWaitingTime_min;

		public double headway_min;

		public int isOnlyWalk;

		Result(Task task) {
			this.identifier = task.identifier;
		}
	}
}
