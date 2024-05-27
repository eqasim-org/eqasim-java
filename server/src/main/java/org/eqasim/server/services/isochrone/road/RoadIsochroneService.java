package org.eqasim.server.services.isochrone.road;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.eqasim.server.services.WalkConfiguration;
import org.eqasim.server.services.router.road.RoadRouterService;
import org.eqasim.server.services.router.road.RoadRouterService.WalkParameters;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTWriter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.common.collect.Sets;

public class RoadIsochroneService {
	private final GeometryFactory geometryFactory = new GeometryFactory();

	private final TravelTime travelTime = new FreeSpeedTravelTime();

	private final Network network;
	private final QuadTree<? extends Node> nodeIndex;

	private final WalkParameters walkParameters;

	RoadIsochroneService(Network network, QuadTree<? extends Node> nodeIndex, WalkParameters walkParameters) {
		this.network = network;
		this.nodeIndex = nodeIndex;
		this.walkParameters = walkParameters;
	}

	public RoadIsochroneResponse processRequest(RoadIsochroneRequest request) {
		RoadIsochroneResponse response = new RoadIsochroneResponse();
		response.requestIndex = request.requestIndex;

		PriorityQueue<Pair<Node, Destination>> queue = new PriorityQueue<>((a, b) -> {
			return Double.compare(a.getRight().travelTime, b.getRight().travelTime);
		});

		IdMap<Node, Destination> destinations = new IdMap<>(Node.class);
		IdMap<Node, Double> minimumTravelTime = new IdMap<>(Node.class);

		Set<Node> originCandidates = new HashSet<>();

		if (request.originRadius_km != null) {
			originCandidates.addAll(nodeIndex.getDisk(request.originX, request.originY, request.originRadius_km * 1e3));
		}

		if (originCandidates.size() == 0) {
			originCandidates.add(nodeIndex.getClosest(request.originX, request.originY));
		}

		IdSet<Node> restrictedNodes = getRestrictedNodes(request);

		for (Node originNode : originCandidates) {
			Coord originCoord = originNode.getCoord();

			double accessTime = 0.0;
			double accessDistance = 0.0;

			if (request.considerAccess) {
				accessDistance = CoordUtils.calcEuclideanDistance(new Coord(request.originX, request.originY),
						originCoord) * walkParameters.beelineWalkFactor();
				accessTime = accessDistance / walkParameters.beelineWalkSpeed_m_s()
						* walkParameters.beelineWalkFactor();
			}

			Destination originDestination = new Destination(originCoord.getX(), originCoord.getY(), accessTime,
					accessDistance, true, restrictedNodes.contains(originNode.getId()), originNode.getId(), accessTime,
					accessDistance);
			queue.add(Pair.of(originNode, originDestination));
			destinations.put(originNode.getId(), originDestination);
			minimumTravelTime.put(originNode.getId(), accessTime);
		}

		double maximumTravelTime = request.maximumTravelTime_min * 60.0;

		while (!queue.isEmpty()) {
			var currentItem = queue.poll();

			double currentTravelTime = currentItem.getRight().travelTime;
			double currentDistance = currentItem.getRight().distance;
			double currentAccessTime = currentItem.getRight().accessTime;
			double currentAccessDistance = currentItem.getRight().accessDistance;

			Node currentNode = currentItem.getLeft();
			boolean currentRestricted = restrictedNodes.contains(currentNode.getId());

			for (Link link : currentNode.getOutLinks().values()) {
				Node nextNode = link.getToNode();
				boolean nextRestricted = restrictedNodes.contains(nextNode.getId());

				double enterTime = request.departureTime_s + currentTravelTime;
				double nextTravelTime = currentTravelTime + travelTime.getLinkTravelTime(link, enterTime, null, null);

				if (request.segmentLength_km != null) {
					double linkLength = CoordUtils.calcEuclideanDistance(currentNode.getCoord(), nextNode.getCoord());
					double requestedSegmentLength = request.segmentLength_km * 1e3;

					if (linkLength > requestedSegmentLength) {
						double linkTravelTime = nextTravelTime - currentTravelTime;

						int segments = (int) Math.floor(linkLength / requestedSegmentLength);

						double segmentLength = linkLength / segments;
						double segmentDuration = linkTravelTime / segments;

						List<String> framingNodeIds = new LinkedList<>();
						framingNodeIds.add(currentNode.getId().toString());
						framingNodeIds.add(nextNode.getId().toString());
						Collections.sort(framingNodeIds);

						String nodePrefix = framingNodeIds.get(0) + "::" + framingNodeIds.get(1) + "::";

						for (int k = 1; k < segments; k++) {
							double segmentTravelTime = currentTravelTime + k * segmentDuration;
							double segmentDistance = currentDistance + k * segmentLength;

							if (segmentTravelTime <= maximumTravelTime) {
								Coord direction = CoordUtils.minus(nextNode.getCoord(), currentNode.getCoord());

								Coord segmentCoord = CoordUtils.plus(currentNode.getCoord(),
										CoordUtils.scalarMult((double) k / segments, direction));

								Destination segmentDestination = new Destination(segmentCoord.getX(),
										segmentCoord.getY(), segmentTravelTime, segmentDistance, false,
										currentRestricted && nextRestricted, null, currentAccessTime,
										currentAccessDistance);

								Id<Node> segmentNodeId = Id.createNodeId(nodePrefix + k);
								destinations.put(segmentNodeId, segmentDestination);
							}
						}
					}
				}

				if (nextTravelTime < minimumTravelTime.getOrDefault(nextNode.getId(), Double.POSITIVE_INFINITY)) {
					double nextDistance = currentDistance + link.getLength();

					if (nextTravelTime <= maximumTravelTime) {
						Coord nextCoord = nextNode.getCoord();
						Destination nextDestination = new Destination(nextCoord.getX(), nextCoord.getY(),
								nextTravelTime, nextDistance, false, nextRestricted, nextNode.getId(),
								currentAccessTime, currentAccessDistance);

						destinations.put(nextNode.getId(), nextDestination);
						queue.add(Pair.of(nextNode, nextDestination));
					}

					minimumTravelTime.put(nextNode.getId(), nextTravelTime);
				}
			}
		}

		for (Destination destination : destinations.values()) {
			RoadIsochroneResponse.Point point = new RoadIsochroneResponse.Point();
			response.points.add(point);

			point.arrivalTime_s = destination.travelTime + request.departureTime_s;
			point.inVehicleTime_min = (destination.travelTime - destination.accessTime) / 60.0;
			point.isOrigin = destination.isOrigin;
			point.totalTravelTime_min = destination.travelTime / 60.0;
			point.x = destination.x;
			point.y = destination.y;

			if (request.considerAccess) {
				point.accessTime_min = destination.accessTime / 60.0;
			}

			if (request.osmRestrictions != null) {
				point.isRestricted = destination.isRestricted;
			}

			if (request.provideNodes && destination.nodeId != null) {
				point.nodeId = destination.nodeId.toString();
			}

			if (request.provideGeometry) {
				WKTWriter writer = new WKTWriter();
				point.geometry = writer.write(geometryFactory.createPoint(new Coordinate(point.x, point.y)));
			}
		}

		return response;
	}

	private IdSet<Node> getRestrictedNodes(RoadIsochroneRequest request) {
		if (request.osmRestrictions != null) {
			IdSet<Link> restrictedLinks = new IdSet<>(Link.class);

			for (Link link : network.getLinks().values()) {
				String osm = (String) link.getAttributes().getAttribute("osm:way:highway");

				if (osm != null && request.osmRestrictions.contains(osm)) {
					restrictedLinks.add(link.getId());
				}
			}

			IdSet<Node> restrictedNodes = new IdSet<>(Node.class);

			for (Node node : network.getNodes().values()) {
				if (Sets.difference(node.getInLinks().keySet(), restrictedLinks).size() == 0) {
					if (Sets.difference(node.getOutLinks().keySet(), restrictedLinks).size() == 0) {
						restrictedNodes.add(node.getId());
					}
				}
			}

			return restrictedNodes;
		}

		return new IdSet<>(Node.class);
	}

	private record Destination(double x, double y, double travelTime, double distance, boolean isOrigin,
			boolean isRestricted, Id<Node> nodeId, double accessTime, double accessDistance) {
	}

	static public RoadIsochroneService create(Config config, Network network, WalkConfiguration configuration) {
		WalkParameters walkParameters = RoadRouterService.createWalkParameters(config, configuration);
		QuadTree<? extends Node> nodeIndex = QuadTrees.createQuadTree(network.getNodes().values());

		return new RoadIsochroneService(network, nodeIndex, walkParameters);
	}
}
