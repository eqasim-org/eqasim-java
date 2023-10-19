package org.eqasim.server.backend.iso;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.eqasim.server.backend.iso.TransitIsochroneBackend.Response.Destination;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.IdMap;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class TransitIsochroneBackend {
	private final TransitSchedule schedule;

	private final QuadTree<TransitStopFacility> stopIndex;
	private final IdMap<TransitStopFacility, Set<TransitRoute>> stopToRoute;

	public TransitIsochroneBackend(TransitSchedule schedule) {
		this.schedule = schedule;
		this.stopIndex = QuadTrees.createQuadTree(schedule.getFacilities().values(), e -> e.getCoord(), 0.0);
		this.stopToRoute = createStopToRoute();
	}

	public static enum RequestMode {
		Default, WithoutRail, OnlyRail
	}

	public static class Request {
		public final double originX;
		public final double originY;
		public final double departureTime;
		public final double maximumTravelTime;
		public final int maximumTransfers;
		public final RequestMode requestMode;
		public final double maximumTransferDistance;
		public final double walkDistanceFactor;
		public final double walkSpeed;

		public Request(double originX, double originY, double departureTime, double maximumTravelTime,
				int maximumTransfers, RequestMode requestMode, double maximumTransferDistance,
				double walkDistanceFactor, double walkSpeed) {
			this.originX = originX;
			this.originY = originY;
			this.departureTime = departureTime;
			this.maximumTravelTime = maximumTravelTime;
			this.requestMode = requestMode;
			this.maximumTransferDistance = maximumTransferDistance;
			this.walkDistanceFactor = walkDistanceFactor;
			this.walkSpeed = walkSpeed;
			this.maximumTransfers = maximumTransfers;
		}
	}

	public static class Response {
		public final List<Destination> destinations;

		public Response(List<Destination> destinations) {
			this.destinations = Collections.unmodifiableList(destinations);
		}

		public static class Destination {
			public final double x;
			public final double y;
			public final double travelTime;
			public final int transfers;
			public final boolean isOrigin;

			Destination(double x, double y, double travelTime, int transfers, boolean isOrigin) {
				this.x = x;
				this.y = y;
				this.travelTime = travelTime;
				this.transfers = transfers;
				this.isOrigin = isOrigin;
			}
		}
	}

	public Response process(Request request) {
		PriorityQueue<Pair<TransitStopFacility, Destination>> queue = new PriorityQueue<>((a, b) -> {
			return Double.compare(a.getRight().travelTime, b.getRight().travelTime);
		});

		IdMap<TransitStopFacility, Destination> destinations = new IdMap<>(TransitStopFacility.class);
		IdMap<TransitStopFacility, Double> minimumTravelTime = new IdMap<>(TransitStopFacility.class);

		Collection<TransitStopFacility> originCandidates = stopIndex.getDisk(request.originX, request.originY,
				request.maximumTransferDistance);
		for (TransitStopFacility originFacility : originCandidates) {
			Coord facilityCoord = originFacility.getCoord();

			Destination originDestination = new Destination(facilityCoord.getX(), facilityCoord.getY(), 0.0, 0, true);
			queue.add(Pair.of(originFacility, originDestination));
			destinations.put(originFacility.getId(), originDestination);
			minimumTravelTime.put(originFacility.getId(), 0.0);
		}

		while (!queue.isEmpty()) {
			var currentItem = queue.poll();

			double currentTravelTime = currentItem.getRight().travelTime;
			int currentTransfers = currentItem.getRight().transfers;
			TransitStopFacility currentFacility = currentItem.getLeft();
			Coord currentCoord = currentFacility.getCoord();

			Collection<TransitStopFacility> nextFacilityCandidates = stopIndex.getDisk(currentCoord.getX(),
					currentCoord.getY(), request.maximumTransferDistance);

			for (TransitStopFacility nextFacilityCandidate : nextFacilityCandidates) {
				Set<TransitRoute> routes = stopToRoute.getOrDefault(currentFacility.getId(), Collections.emptySet());
				double transferTravelTime = CoordUtils.calcEuclideanDistance(currentCoord,
						nextFacilityCandidate.getCoord()) * request.walkDistanceFactor / request.walkSpeed;

				for (TransitRoute route : routes) {
					if (request.requestMode.equals(RequestMode.WithoutRail)) {
						if (route.getTransportMode().equals("rail")) {
							continue;
						}
					}

					if (request.requestMode.equals(RequestMode.OnlyRail)) {
						if (!route.getTransportMode().equals("rail")) {
							continue;
						}
					}

					TransitRouteStop currentStop = route.getStop(currentFacility);
					int stopIndex = route.getStops().indexOf(currentStop);

					for (Departure departure : route.getDepartures().values()) {
						double conectionDepartureTime = departure.getDepartureTime()
								+ currentStop.getDepartureOffset().seconds();

						if (conectionDepartureTime >= currentTravelTime + request.departureTime + transferTravelTime) {
							for (int i = stopIndex + 1; i < route.getStops().size(); i++) {
								TransitRouteStop nextStopCandidate = route.getStops().get(i);
								TransitStopFacility nextFacility = nextStopCandidate.getStopFacility();
								double connectionArrivalTime = departure.getDepartureTime()
										+ nextStopCandidate.getArrivalOffset().seconds();
								double nextTravelTime = connectionArrivalTime - request.departureTime;

								if (nextTravelTime < minimumTravelTime.getOrDefault(nextFacility.getId(),
										Double.POSITIVE_INFINITY)) {

									int nextTransfers = currentTransfers + (currentItem.getRight().isOrigin ? 0 : 1);

									if (nextTravelTime <= request.maximumTravelTime) {
										Coord nextCoord = nextStopCandidate.getStopFacility().getCoord();

										Destination nextDestination = new Destination(nextCoord.getX(),
												nextCoord.getY(), nextTravelTime, nextTransfers, false);

										destinations.put(nextFacility.getId(), nextDestination);
										queue.add(Pair.of(nextFacility, nextDestination));
									}

									minimumTravelTime.put(nextFacility.getId(), nextTravelTime);
								}
							}
						}
					}
				}
			}
		}

		List<Destination> destinationsList = new ArrayList<>(destinations.values());
		return new Response(destinationsList);
	}

	static public class TransitCell {
		public final double x;
		public final double y;

		public final int u;
		public final int v;

		public double travelTime = Double.POSITIVE_INFINITY;
		public int transfers = 0;

		TransitCell(int u, int v, double x, double y) {
			this.x = x;
			this.y = y;
			this.u = u;
			this.v = v;
		}
	}

	private IdMap<TransitStopFacility, Set<TransitRoute>> createStopToRoute() {
		IdMap<TransitStopFacility, Set<TransitRoute>> stopToRoute = new IdMap<>(TransitStopFacility.class);

		for (TransitLine transitLine : schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				for (TransitRouteStop stop : transitRoute.getStops()) {
					stopToRoute.computeIfAbsent(stop.getStopFacility().getId(), id -> new HashSet<>())
							.add(transitRoute);
				}
			}
		}

		return stopToRoute;
	}
}
