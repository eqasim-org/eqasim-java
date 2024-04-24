package org.eqasim.core.tools.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.eqasim.core.components.headway.HeadwayCalculator;
import org.eqasim.core.misc.ParallelProgress;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Provider;

public class BatchPublicTransportRouter {
	private final Provider<TransitRouter> routerProvider;
	private final Provider<HeadwayCalculator> headwayCalculatorProvider;
	private final TransitSchedule schedule;
	private final Network network;

	private final int batchSize;
	private final int numberOfThreads;
	private final double interval;

	public BatchPublicTransportRouter(Provider<TransitRouter> routerProvider,
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

	public Pair<Collection<TripInformation>, Collection<LegInformation>> run(Collection<Task> tasks)
			throws InterruptedException {
		Iterator<Task> taskIterator = tasks.iterator();

		List<TripInformation> tripResults = new ArrayList<>(tasks.size());
		List<LegInformation> legResults = new ArrayList<>(tasks.size());

		ParallelProgress progress = new ParallelProgress("Routing trips ...", tasks.size());
		progress.start();

		List<Thread> threads = new ArrayList<>(numberOfThreads);

		for (int i = 0; i < numberOfThreads; i++) {
			Thread thread = new Thread(new Worker(taskIterator, tripResults, legResults, progress));
			threads.add(thread);
			thread.start();
		}

		for (Thread thread : threads) {
			thread.join();
		}

		progress.close();
		return Pair.of(tripResults, legResults);
	}

	private class Worker implements Runnable {
		private final Iterator<Task> taskIterator;
		private final Collection<TripInformation> tripResults;
		private final Collection<LegInformation> legResults;
		private final ParallelProgress progress;

		private Worker(Iterator<Task> taskIterator, Collection<TripInformation> tripResults,
				Collection<LegInformation> legResults, ParallelProgress progress) {
			this.taskIterator = taskIterator;
			this.tripResults = tripResults;
			this.legResults = legResults;
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

				List<TripInformation> localTripResults = new ArrayList<>(localTasks.size());
				List<LegInformation> localLegResults = new ArrayList<>(localTasks.size() * 3);

				for (Task task : localTasks) {
					TripInformation tripInformation = new TripInformation(task);

					Coord fromCoord = new Coord(task.originX, task.originY);
					Coord toCoord = new Coord(task.destinationX, task.destinationY);

					Facility fromFacility = new LinkWrapperFacility(NetworkUtils.getNearestLink(network, fromCoord));
					Facility toFacility = new LinkWrapperFacility(NetworkUtils.getNearestLink(network, toCoord));

					List<? extends PlanElement> planElements = router.calcRoute(DefaultRoutingRequest.withoutAttributes(fromFacility, toFacility, task.departureTime, null));

					if (planElements != null) {
						boolean isFirstVehicularLeg = true;
						tripInformation.isOnlyWalk = 1;

						if (interval > 0.0) {
							tripInformation.headway_min = headwayCalculator.calculateHeadway_min(fromFacility,
									toFacility, task.departureTime);
						} else {
							tripInformation.headway_min = Double.NaN;
						}

						int currentIndex = 0;

						for (PlanElement planElement : planElements) {
							if(!(planElement instanceof Leg)) {
								continue;
							}
							Leg leg = (Leg) planElement;
							boolean isFirstLeg = currentIndex == 0;
							boolean isLastLeg = currentIndex == planElements.size() - 1;

							if (leg.getMode().contains("walk") && isFirstLeg) {
								tripInformation.accessTravelTime_min += leg.getTravelTime().seconds() / 60.0;
								tripInformation.accessDistance_km += leg.getRoute().getDistance() * 1e-3;
							} else if (leg.getMode().contains("walk") && isLastLeg) {
								tripInformation.egressTravelTime_min += leg.getTravelTime().seconds() / 60.0;
								tripInformation.egressDistance_km += leg.getRoute().getDistance() * 1e-3;
							} else if (leg.getMode().contains("walk")) {
								tripInformation.transferTravelTime_min += leg.getTravelTime().seconds() / 60.0;
								tripInformation.transferDistance_km += leg.getRoute().getDistance() * 1e-3;
							} else if (leg.getRoute() instanceof TransitPassengerRoute) {
								TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();

								TransitLine transitLine = schedule.getTransitLines().get(route.getLineId());
								TransitRoute transitRoute = transitLine.getRoutes().get(route.getRouteId());
								String transitMode = transitRoute.getTransportMode();

								double waitingTime = route.getBoardingTime().seconds()
										- leg.getDepartureTime().seconds();

								if (isFirstVehicularLeg) {
									tripInformation.initialWaitingTime_min += waitingTime / 60.0;
									isFirstVehicularLeg = false;
								} else {
									tripInformation.numberOfTransfers += 1;
									tripInformation.transferWaitingTime_min += waitingTime / 60.0;
								}

								double inVehicleTime = route.getTravelTime().seconds() - waitingTime;

								switch (transitMode) {
								case "rail":
									tripInformation.inVehicleTimeRail_min += inVehicleTime / 60.0;
									tripInformation.inVehicleDistanceRail_km += route.getDistance() * 1e-3;
									break;
								case "subway":
									tripInformation.inVehicleTimeSubway_min += inVehicleTime / 60.0;
									tripInformation.inVehicleDistanceSubway_km += route.getDistance() * 1e-3;
									break;
								case "bus":
									tripInformation.inVehicleTimeBus_min += inVehicleTime / 60.0;
									tripInformation.inVehicleDistanceBus_km += route.getDistance() * 1e-3;
									break;
								case "tram":
									tripInformation.inVehicleTimeTram_min += inVehicleTime / 60.0;
									tripInformation.inVehicleDistanceTram_km += route.getDistance() * 1e-3;
									break;
								default:
									tripInformation.inVehicleTimeOther_min += inVehicleTime / 60.0;
									tripInformation.inVehicleDistanceOther_km += route.getDistance() * 1e-3;
								}

								tripInformation.isOnlyWalk = 0;

								{ // Legs
									Departure departure = findDeparture(route, transitRoute);

									LegInformation legInformation = new LegInformation();
									legInformation.identifier = task.identifier;
									legInformation.legIndex = currentIndex;
									legInformation.transitMode = transitMode;
									legInformation.lineId = transitLine.getId().toString();
									legInformation.routeId = transitRoute.getId().toString();
									legInformation.vehicleId = departure.getVehicleId().toString();
									legInformation.accessTime = route.getBoardingTime().seconds();
									legInformation.egressTime = leg.getDepartureTime().seconds()
											+ leg.getTravelTime().seconds();

									localLegResults.add(legInformation);
								}
							} else {
								throw new IllegalStateException("Don't know what to do with mode: " + leg.getMode());
							}

							currentIndex++;
						}

						tripInformation.inVehicleTimeTotal_min = tripInformation.inVehicleTimeRail_min
								+ tripInformation.inVehicleTimeSubway_min + tripInformation.inVehicleTimeBus_min
								+ tripInformation.inVehicleTimeTram_min + tripInformation.inVehicleTimeOther_min;
						tripInformation.inVehicleDistanceTotal_km = tripInformation.inVehicleDistanceRail_km
								+ tripInformation.inVehicleDistanceSubway_km + tripInformation.inVehicleDistanceBus_km
								+ tripInformation.inVehicleDistanceTram_km + tripInformation.inVehicleDistanceOther_km;
						tripInformation.totalWalkTravelTime_min = tripInformation.accessTravelTime_min
								+ tripInformation.egressTravelTime_min + tripInformation.transferTravelTime_min;
						tripInformation.totalWalkDistance_km = tripInformation.accessDistance_km
								+ tripInformation.egressDistance_km + tripInformation.transferDistance_km;

						localTripResults.add(tripInformation);
					}

					progress.update();
				}

				synchronized (tripResults) {
					tripResults.addAll(localTripResults);
					legResults.addAll(localLegResults);
				}
			}
		}
	}

	private static Departure findDeparture(TransitPassengerRoute passengerRoute, TransitRoute route) {
		double boardingTime = passengerRoute.getBoardingTime().seconds();

		List<Double> accessOffsets = route.getStops().stream() //
				.filter(stop -> stop.getStopFacility().getId().equals(passengerRoute.getAccessStopId())) //
				.map(stop -> stop.getArrivalOffset().seconds()).collect(Collectors.toList());

		for (Departure departure : route.getDepartures().values()) {
			if (departure.getDepartureTime() <= boardingTime) {
				for (double offset : accessOffsets) {
					if (departure.getDepartureTime() + offset == boardingTime) {
						return departure;
					}
				}
			}
		}

		throw new IllegalStateException("Departure not found");
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

	static public class TripInformation {
		@JsonProperty("identifier")
		public String identifier;

		@JsonProperty("access_travel_time_min")
		public double accessTravelTime_min;

		@JsonProperty("access_distance_km")
		public double accessDistance_km;

		@JsonProperty("egress_travel_time_min")
		public double egressTravelTime_min;

		@JsonProperty("egress_distance_km")
		public double egressDistance_km;

		@JsonProperty("transfer_travel_time_min")
		public double transferTravelTime_min;

		@JsonProperty("transfer_distance_km")
		public double transferDistance_km;

		@JsonProperty("total_walk_travel_time_min")
		public double totalWalkTravelTime_min;

		@JsonProperty("total_walk_distance_km")
		public double totalWalkDistance_km;

		@JsonProperty("in_vehicle_time_rail_min")
		public double inVehicleTimeRail_min;

		@JsonProperty("in_vehicle_time_subway_min")
		public double inVehicleTimeSubway_min;

		@JsonProperty("in_vehicle_time_bus_min")
		public double inVehicleTimeBus_min;

		@JsonProperty("in_vehicle_time_tram_min")
		public double inVehicleTimeTram_min;

		@JsonProperty("in_vehicle_time_other_min")
		public double inVehicleTimeOther_min;

		@JsonProperty("in_vehicle_time_total_min")
		public double inVehicleTimeTotal_min;

		@JsonProperty("in_vehicle_distance_rail_km")
		public double inVehicleDistanceRail_km;

		@JsonProperty("in_vehicle_distance_subway_km")
		public double inVehicleDistanceSubway_km;

		@JsonProperty("in_vehicle_distance_bus_km")
		public double inVehicleDistanceBus_km;

		@JsonProperty("in_vehicle_distance_tram_km")
		public double inVehicleDistanceTram_km;

		@JsonProperty("in_vehicle_distance_other_km")
		public double inVehicleDistanceOther_km;

		@JsonProperty("in_vehicle_distance_total_km")
		public double inVehicleDistanceTotal_km;

		@JsonProperty("transfers")
		public int numberOfTransfers;

		@JsonProperty("initial_waiting_time_min")
		public double initialWaitingTime_min;

		@JsonProperty("transfer_waiting_time_min")
		public double transferWaitingTime_min;

		@JsonProperty("headway_min")
		public double headway_min;

		@JsonProperty("is_only_walk")
		public int isOnlyWalk;

		TripInformation(Task task) {
			this.identifier = task.identifier;
		}
	}

	static public class LegInformation {
		@JsonProperty("identifier")
		public String identifier;

		@JsonProperty("leg_index")
		public int legIndex;

		@JsonProperty("line_id")
		public String lineId;

		@JsonProperty("route_id")
		public String routeId;

		@JsonProperty("vehicle_id")
		public String vehicleId;

		@JsonProperty("transit_mode")
		public String transitMode;

		@JsonProperty("access_time")
		public double accessTime;

		@JsonProperty("egress_time")
		public double egressTime;
	}
}
