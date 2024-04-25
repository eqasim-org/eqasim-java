package org.eqasim.server.services.router.road;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eqasim.server.services.WalkConfiguration;
import org.eqasim.server.services.router.road.RoadRouterResponse.LinkRecord;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTWriter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.RoutingConfigGroup.TeleportedModeParams;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.core.utils.geometry.CoordUtils;

public class RoadRouterService {
	private final static GeometryFactory geometryFactory = new GeometryFactory();

	private final TravelTime travelTime = new FreeSpeedTravelTime();
	private final TravelDisutility travelDisutility = new OnlyTimeDependentTravelDisutility(travelTime);

	private final WalkParameters walkParameters;

	private final QuadTree<? extends Link> linkIndex;

	private final SpeedyALTFactory routerFactory = new SpeedyALTFactory();
	private final ConcurrentLinkedQueue<LeastCostPathCalculator> routerPool = new ConcurrentLinkedQueue<>();

	RoadRouterService(Network network, QuadTree<? extends Link> linkIndex, WalkParameters walkParameters, int threads) {
		this.walkParameters = walkParameters;
		this.linkIndex = linkIndex;

		for (int k = 0; k < threads; k++) {
			routerPool.add(createRouterInstance(network));
		}
	}

	private LeastCostPathCalculator createRouterInstance(Network network) {
		return routerFactory.createPathCalculator(network, travelDisutility, travelTime);
	}

	public RoadRouterResponse processRequest(RoadRouterRequest request) {
		RoadRouterResponse bestResponse = null;

		Coord fromCoord = new Coord(request.originX, request.originY);
		Coord toCoord = new Coord(request.destinationX, request.destinationY);

		List<Link> fromLinks = new LinkedList<>();
		List<Link> toLinks = new LinkedList<>();

		if (request.accessEgressRadius_km != null) {
			fromLinks
					.addAll(linkIndex.getDisk(fromCoord.getX(), fromCoord.getY(), request.accessEgressRadius_km * 1e3));
			toLinks.addAll(linkIndex.getDisk(toCoord.getX(), toCoord.getY(), request.accessEgressRadius_km * 1e3));
		}

		if (fromLinks.size() == 0) {
			fromLinks.add(linkIndex.getClosest(fromCoord.getX(), fromCoord.getY()));
		}

		if (toLinks.size() == 0) {
			toLinks.add(linkIndex.getClosest(toCoord.getX(), toCoord.getY()));
		}

		for (Link fromLink : fromLinks) {
			for (Link toLink : toLinks) {
				RoadRouterResponse response = new RoadRouterResponse();
				response.requestIndex = request.requestIndex;

				if (request.provideLinks) {
					response.originId = fromLink.getId().toString();
					response.destinationId = toLink.getId().toString();
				}

				Node fromNode = fromLink.getToNode();
				Node toNode = toLink.getFromNode();

				response.accessDistance_km = CoordUtils.calcEuclideanDistance(fromCoord, fromNode.getCoord()) * 1e-3;
				response.egressDistance_km = CoordUtils.calcEuclideanDistance(toNode.getCoord(), toCoord) * 1e-3;

				response.accessTime_min = walkParameters.beelineWalkFactor * response.accessDistance_km
						/ walkParameters.beelineWalkSpeed_m_s / 60.0;

				response.egressTime_min = walkParameters.beelineWalkFactor * response.egressDistance_km
						/ walkParameters.beelineWalkSpeed_m_s / 60.0;

				double departureTime = request.departureTime_s + response.accessTime_min * 60.0;

				LeastCostPathCalculator router = routerPool.poll();
				Path path = router.calcLeastCostPath(fromNode, toNode, departureTime, null, null);
				routerPool.add(router);

				response.inVehicleTime_min = path.travelTime / 60.0;
				response.inVehicleDistance_km = RouteUtils.calcDistance(path) * 1e-3;
				response.arrivalTime_s = departureTime + path.travelTime;
				response.totalTravelTime_min = (response.arrivalTime_s - request.departureTime_s) / 60.0;

				if (request.provideLinks) {
					response.links = new LinkedList<>();

					double currentTime = departureTime;

					for (Link link : path.links) {
						LinkRecord linkRecord = new LinkRecord();
						linkRecord.id = link.getId().toString();
						linkRecord.enterTime_s = currentTime;

						currentTime += travelTime.getLinkTravelTime(link, currentTime, null, null);
						linkRecord.exitTime_s = currentTime;

						response.links.add(linkRecord);
					}
				}

				if (request.provideGeometry) {
					WKTWriter writer = new WKTWriter();

					Coordinate[] roadCoordinates = new Coordinate[path.nodes.size()];
					for (int k = 0; k < path.nodes.size(); k++) {
						Node node = path.nodes.get(k);
						roadCoordinates[k] = new Coordinate(node.getCoord().getX(), node.getCoord().getY());
					}

					response.roadGeometry = writer.write(geometryFactory.createLineString(roadCoordinates));

					Coordinate[] accessCoordinates = new Coordinate[2];
					accessCoordinates[0] = new Coordinate(request.originX, request.originY);
					accessCoordinates[1] = new Coordinate(fromNode.getCoord().getX(), fromNode.getCoord().getY());
					response.accessGeometry = writer.write(geometryFactory.createLineString(accessCoordinates));

					Coordinate[] egressCoordinates = new Coordinate[2];
					egressCoordinates[0] = new Coordinate(toNode.getCoord().getX(), toNode.getCoord().getY());
					egressCoordinates[1] = new Coordinate(request.destinationX, request.destinationY);
					response.egressGeometry = writer.write(geometryFactory.createLineString(egressCoordinates));
				}

				if (bestResponse == null || bestResponse.totalTravelTime_min > response.totalTravelTime_min) {
					bestResponse = response;
				}
			}
		}

		return bestResponse;
	}

	static public RoadRouterService create(Config config, Network network, WalkConfiguration configuration,
			int threads) {
		WalkParameters walkParameters = createWalkParameters(config, configuration);
		QuadTree<? extends Link> linkIndex = QuadTrees.createQuadTree(network.getLinks().values());

		return new RoadRouterService(network, linkIndex, walkParameters, threads);
	}

	static public record WalkParameters(double beelineWalkFactor, double beelineWalkSpeed_m_s) {
	}

	static public WalkParameters createWalkParameters(Config config, WalkConfiguration configuration) {
		TeleportedModeParams params = config.routing().getTeleportedModeParams().get(TransportMode.walk);

		double beelineWalkFactor = params.getBeelineDistanceFactor();
		double beelineWalkSpeed_m_s = params.getTeleportedModeSpeed();

		if (configuration.beelineWalkFactor != null) {
			beelineWalkFactor = configuration.beelineWalkFactor;
		}

		if (configuration.beelineWalkSpeed_m_s != null) {
			beelineWalkSpeed_m_s = configuration.beelineWalkSpeed_m_s;
		}

		return new WalkParameters(beelineWalkFactor, beelineWalkSpeed_m_s);
	}
}
