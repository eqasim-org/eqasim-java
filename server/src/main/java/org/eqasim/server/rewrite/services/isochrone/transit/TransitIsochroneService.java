package org.eqasim.server.rewrite.services.isochrone.transit;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.eqasim.server.rewrite.services.TransitConfiguration;
import org.eqasim.server.rewrite.services.WalkConfiguration;
import org.eqasim.server.rewrite.services.router.transit.TransitRouterService;
import org.eqasim.server.rewrite.services.router.transit.TransitRouterService.WalkParameters;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTWriter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.core.config.Config;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.common.base.Verify;

public class TransitIsochroneService {
	private final static GeometryFactory geometryFactory = new GeometryFactory();

	private final QuadTree<TransitStopFacility> stopIndex;
	private final IdMap<TransitStopFacility, Set<TransitRoute>> stopToRoute;

	private final Set<String> availableModes;

	private final WalkParameters walkParameters;

	TransitIsochroneService(QuadTree<TransitStopFacility> stopIndex,
			IdMap<TransitStopFacility, Set<TransitRoute>> stopToRoute, WalkParameters walkParameters,
			Set<String> availableModes) {
		this.stopIndex = stopIndex;
		this.stopToRoute = stopToRoute;
		this.availableModes = availableModes;
		this.walkParameters = walkParameters;
	}

	public TransitIsochroneResponse processRequest(TransitIsochroneRequest request) {
		TransitIsochroneResponse response = new TransitIsochroneResponse();
		response.requestIndex = request.requestIndex;

		PriorityQueue<Pair<TransitStopFacility, Destination>> queue = new PriorityQueue<>((a, b) -> {
			return Double.compare(a.getRight().travelTime, b.getRight().travelTime);
		});

		IdMap<TransitStopFacility, Destination> destinations = new IdMap<>(TransitStopFacility.class);
		IdMap<TransitStopFacility, Double> minimumTravelTime = new IdMap<>(TransitStopFacility.class);

		Collection<TransitStopFacility> originCandidates = stopIndex.getDisk(request.originX, request.originY,
				request.maximumAccessDistance_km * 1e3);

		if (originCandidates.size() == 0) {
			originCandidates.add(stopIndex.getClosest(request.originX, request.originY));
		}

		for (TransitStopFacility originFacility : originCandidates) {
			Coord facilityCoord = originFacility.getCoord();

			double accessTime = 0.0;

			if (request.considerAccess) {
				accessTime = CoordUtils.calcEuclideanDistance(new Coord(request.originX, request.originY),
						facilityCoord) * walkParameters.beelineWalkFactor() / walkParameters.beelineWalkSpeed_m_s();
			}

			Destination originDestination = new Destination(facilityCoord.getX(), facilityCoord.getY(), accessTime, 0,
					true, originFacility.getId(), originFacility.getId(), accessTime, 0.0, 0.0);
			queue.add(Pair.of(originFacility, originDestination));
			destinations.put(originFacility.getId(), originDestination);
			minimumTravelTime.put(originFacility.getId(), accessTime);
		}

		Set<String> restrictedModes = getRestrictedModes(request);

		if (request.maximumTravelTime_min == null) {
			Verify.verifyNotNull(request.maximumTransfers);
		}

		if (request.maximumTransfers == null) {
			Verify.verifyNotNull(request.maximumTravelTime_min);
		}

		while (!queue.isEmpty()) {
			var currentItem = queue.poll();

			double currentTravelTime = currentItem.getRight().travelTime;
			int currentTransfers = currentItem.getRight().transfers;
			double currentAccessTime = currentItem.getRight().accessTime;
			double currentTransferTime = currentItem.getRight().transferTime;
			double currentWaitTime = currentItem.getRight().waitTime;

			TransitStopFacility currentFacility = currentItem.getLeft();
			Coord currentCoord = currentFacility.getCoord();

			Collection<TransitStopFacility> nextFacilityCandidates = stopIndex.getDisk(currentCoord.getX(),
					currentCoord.getY(), request.maximumTransferDistance_km * 1e3);

			for (TransitStopFacility nextFacilityCandidate : nextFacilityCandidates) {
				Set<TransitRoute> routes = stopToRoute.getOrDefault(currentFacility.getId(), Collections.emptySet());
				double transferTravelTime = CoordUtils.calcEuclideanDistance(currentCoord,
						nextFacilityCandidate.getCoord()) * walkParameters.beelineWalkFactor()
						/ walkParameters.beelineWalkSpeed_m_s();
				double nextTransferTime = currentTransferTime + transferTravelTime;

				for (TransitRoute route : routes) {
					if (restrictedModes.contains(route.getTransportMode())) {
						continue;
					}

					TransitRouteStop currentStop = route.getStop(currentFacility);
					int stopIndex = route.getStops().indexOf(currentStop);

					for (Departure departure : route.getDepartures().values()) {
						double conectionDepartureTime = departure.getDepartureTime()
								+ currentStop.getDepartureOffset().seconds();

						if (conectionDepartureTime >= currentTravelTime + request.departureTime_s
								+ transferTravelTime) {
							double waitTime = conectionDepartureTime - currentTravelTime - transferTravelTime
									- request.departureTime_s;
							double nextWaitTime = currentWaitTime + waitTime;

							for (int i = stopIndex + 1; i < route.getStops().size(); i++) {
								TransitRouteStop nextStopCandidate = route.getStops().get(i);
								TransitStopFacility nextFacility = nextStopCandidate.getStopFacility();
								double connectionArrivalTime = departure.getDepartureTime()
										+ nextStopCandidate.getArrivalOffset().seconds();
								double nextTravelTime = connectionArrivalTime - request.departureTime_s;

								if (nextTravelTime < minimumTravelTime.getOrDefault(nextFacility.getId(),
										Double.POSITIVE_INFINITY)) {

									int nextTransfers = currentTransfers + (currentItem.getRight().isOrigin ? 0 : 1);

									boolean explore = true;

									if (request.maximumTravelTime_min != null
											&& nextTravelTime > request.maximumTravelTime_min * 60.0) {
										explore = false;
									}

									if (request.maximumTransfers != null && nextTransfers > request.maximumTransfers) {
										explore = false;
									}

									if (explore) {
										Coord nextCoord = nextStopCandidate.getStopFacility().getCoord();

										Destination nextDestination = new Destination(nextCoord.getX(),
												nextCoord.getY(), nextTravelTime, nextTransfers, false,
												currentFacility.getId(), nextFacility.getId(), currentAccessTime,
												nextTransferTime, nextWaitTime);

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

		for (Destination destination : destinations.values()) {
			TransitIsochroneResponse.Stop stop = new TransitIsochroneResponse.Stop();
			response.stops.add(stop);

			stop.x = destination.x;
			stop.y = destination.y;
			stop.arrivalTime_s = destination.travelTime + request.departureTime_s;
			stop.totalTravelTime_min = destination.travelTime / 60.0;
			stop.isOrigin = destination.isOrigin;
			stop.transferTime_min = destination.transferTime / 60.0;
			stop.waitTime_min = destination.waitTime / 60.0;
			stop.inVehicleTime_min = (destination.travelTime - destination.accessTime - destination.waitTime
					- destination.transferTime) / 60.0;
			stop.transfers = destination.transfers;

			if (request.considerAccess) {
				stop.acessTime_min = destination.accessTime / 60.0;
			}

			if (request.provideStops) {
				stop.accessStopId = destination.accessId.toString();
				stop.egressStopId = destination.egressId.toString();
			}

			if (request.provideGeometry) {
				WKTWriter writer = new WKTWriter();
				stop.geometry = writer.write(geometryFactory.createPoint(new Coordinate(stop.x, stop.y)));
			}
		}

		return response;
	}

	private Set<String> getRestrictedModes(TransitIsochroneRequest request) {
		if (request.allowedModes == null && request.restrictedModes == null) {
			return Collections.emptySet();
		} else if (request.allowedModes != null) {
			Verify.verify(request.restrictedModes == null);

			Set<String> restrictedModes = new HashSet<>(availableModes);
			restrictedModes.retainAll(request.allowedModes);
			return restrictedModes;
		} else {
			Verify.verify(request.allowedModes == null);
			return new HashSet<>(request.restrictedModes);
		}
	}

	private record Destination(double x, double y, double travelTime, int transfers, boolean isOrigin,
			Id<TransitStopFacility> accessId, Id<TransitStopFacility> egressId, double accessTime, double transferTime,
			double waitTime) {
	}

	static public TransitIsochroneService create(Config config, TransitSchedule schedule,
			TransitConfiguration configuration, WalkConfiguration walkConfiguration) {
		QuadTree<TransitStopFacility> stopIndex = QuadTrees.createQuadTree(schedule.getFacilities().values(),
				e -> e.getCoord(), 0.0);

		IdMap<TransitStopFacility, Set<TransitRoute>> stopToRoute = new IdMap<>(TransitStopFacility.class);
		Set<String> availableModes = new HashSet<>();

		for (TransitLine transitLine : schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				availableModes.add(transitRoute.getTransportMode());

				for (TransitRouteStop stop : transitRoute.getStops()) {
					stopToRoute.computeIfAbsent(stop.getStopFacility().getId(), id -> new HashSet<>())
							.add(transitRoute);
				}
			}
		}

		WalkParameters walkParameters = TransitRouterService.createWalkParameters(config, walkConfiguration);
		return new TransitIsochroneService(stopIndex, stopToRoute, walkParameters, availableModes);
	}
}
