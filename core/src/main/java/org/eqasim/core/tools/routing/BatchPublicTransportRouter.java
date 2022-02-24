package org.eqasim.core.tools.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.components.headway.HeadwayCalculator;
import org.eqasim.core.misc.ParallelProgress;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provider;

public class BatchPublicTransportRouter {
	private final Provider<TransitRouter> routerProvider;
	private final Provider<HeadwayCalculator> headwayCalculatorProvider;
	private final TransitSchedule schedule;
	private final Network network;

	private final int batchSize;
	private final int numberOfThreads;
	private final double interval;
	private final boolean writeRoute;

	public BatchPublicTransportRouter(Provider<TransitRouter> routerProvider,
			Provider<HeadwayCalculator> headwayCalculatorProvider, TransitSchedule schedule, Network network,
			int batchSize, int numberOfThreads, double interval, boolean writeRoute) {
		this.routerProvider = routerProvider;
		this.headwayCalculatorProvider = headwayCalculatorProvider;
		this.batchSize = batchSize;
		this.numberOfThreads = numberOfThreads;
		this.schedule = schedule;
		this.network = network;
		this.interval = interval;
		this.writeRoute = writeRoute;
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
			TransitRouter router = routerProvider.get();
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

					List<Leg> legs = router.calcRoute(fromFacility, toFacility, task.departureTime, null);
					List<RouteInformation> routeInformation = new LinkedList<>();

					if (legs != null) {
						boolean isFirstVehicularLeg = true;
						result.isOnlyWalk = 1;

						if (interval > 0.0) {
							result.headway_min = headwayCalculator.calculateHeadway_min(fromFacility, toFacility,
									task.departureTime);
						} else {
							result.headway_min = Double.NaN;
						}

						int currentIndex = 0;

						for (Leg leg : legs) {
							boolean isFirstLeg = currentIndex == 0;
							boolean isLastLeg = currentIndex == legs.size() - 1;
							currentIndex++;

							if (leg.getMode().equals(TransportMode.access_walk)
									|| (leg.getMode().equals(TransportMode.walk) && isFirstLeg)) {
								result.accessTravelTime_min += leg.getTravelTime().seconds() / 60.0;
								result.accessDistance_km += leg.getRoute().getDistance() * 1e-3;
							} else if (leg.getMode().equals(TransportMode.egress_walk)
									|| (leg.getMode().equals(TransportMode.walk) && isLastLeg)) {
								result.egressTravelTime_min += leg.getTravelTime().seconds() / 60.0;
								result.egressDistance_km += leg.getRoute().getDistance() * 1e-3;
							} else if (leg.getMode().equals(TransportMode.transit_walk)
									|| (leg.getMode().equals(TransportMode.walk) && !isFirstLeg && !isLastLeg)) {
								result.transferTravelTime_min += leg.getTravelTime().seconds() / 60.0;
								result.transferDistance_km += leg.getRoute().getDistance() * 1e-3;
							} else if (leg.getMode().equals(TransportMode.pt)) {
								TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();

								double waitingTime = route.getBoardingTime().seconds()
										- leg.getDepartureTime().seconds();

								if (isFirstVehicularLeg) {
									result.initialWaitingTime_min += waitingTime / 60.0;
									isFirstVehicularLeg = false;
								} else {
									result.numberOfTransfers += 1;
									result.transferWaitingTime_min += waitingTime / 60.0;
								}

								TransitLine transitLine = schedule.getTransitLines().get(route.getLineId());
								TransitRoute transitRoute = transitLine.getRoutes().get(route.getRouteId());
								String transitMode = transitRoute.getTransportMode();

								double inVehicleTime = route.getTravelTime().seconds() - waitingTime;

								switch (transitMode) {
								case "rail":
									result.inVehicleTimeRail_min += inVehicleTime / 60.0;
									result.inVehicleDistanceRail_km += route.getDistance() * 1e-3;
									break;
								case "subway":
									result.inVehicleTimeSubway_min += inVehicleTime / 60.0;
									result.inVehicleDistanceSubway_km += route.getDistance() * 1e-3;
									break;
								case "bus":
									result.inVehicleTimeBus_min += inVehicleTime / 60.0;
									result.inVehicleDistanceBus_km += route.getDistance() * 1e-3;
									break;
								case "tram":
									result.inVehicleTimeTram_min += inVehicleTime / 60.0;
									result.inVehicleDistanceTram_km += route.getDistance() * 1e-3;
									break;
								default:
									result.inVehicleTimeOther_min += inVehicleTime / 60.0;
									result.inVehicleDistanceOther_km += route.getDistance() * 1e-3;
								}

								result.isOnlyWalk = 0;

								if (writeRoute) {
									RouteInformation partialInformation = new RouteInformation();
									partialInformation.mode = transitMode;
									partialInformation.lineId = transitLine.getId().toString();
									partialInformation.routeId = transitRoute.getId().toString();
									partialInformation.accessTime = route.getBoardingTime().seconds();
									partialInformation.egressTime = leg.getDepartureTime().seconds()
											+ leg.getTravelTime().seconds();
									routeInformation.add(partialInformation);
								}
							} else {
								throw new IllegalStateException();
							}
						}

						result.inVehicleTimeTotal_min = result.inVehicleTimeRail_min + result.inVehicleTimeSubway_min
								+ result.inVehicleTimeBus_min + result.inVehicleTimeTram_min
								+ result.inVehicleTimeOther_min;
						result.inVehicleDistanceTotal_km = result.inVehicleDistanceRail_km
								+ result.inVehicleDistanceSubway_km + result.inVehicleDistanceBus_km
								+ result.inVehicleDistanceTram_km + result.inVehicleDistanceOther_km;
						result.totalWalkTravelTime_min = result.accessTravelTime_min + result.egressTravelTime_min
								+ result.transferTravelTime_min;
						result.totalWalkDistance_km = result.accessDistance_km + result.egressDistance_km
								+ result.transferDistance_km;

						if (writeRoute) {
							try {
								result.route = new ObjectMapper().writeValueAsString(routeInformation);
							} catch (JsonProcessingException e) {
								e.printStackTrace();
							}
						}

						localResults.add(result);
					}

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

		public double transferTravelTime_min;
		public double transferDistance_km;

		public double totalWalkTravelTime_min;
		public double totalWalkDistance_km;

		public double inVehicleTimeRail_min;
		public double inVehicleTimeSubway_min;
		public double inVehicleTimeBus_min;
		public double inVehicleTimeTram_min;
		public double inVehicleTimeOther_min;
		public double inVehicleTimeTotal_min;

		public double inVehicleDistanceRail_km;
		public double inVehicleDistanceSubway_km;
		public double inVehicleDistanceBus_km;
		public double inVehicleDistanceTram_km;
		public double inVehicleDistanceOther_km;
		public double inVehicleDistanceTotal_km;

		public int numberOfTransfers;

		public double initialWaitingTime_min;
		public double transferWaitingTime_min;

		public double headway_min;

		public int isOnlyWalk;

		public String route;

		Result(Task task) {
			this.identifier = task.identifier;
		}
	}

	static public class RouteInformation {
		public String lineId;
		public String routeId;
		public String mode;

		public double accessTime;
		public double egressTime;
	}
}
