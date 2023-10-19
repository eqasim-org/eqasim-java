package org.eqasim.server.backend.iso;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.commons.lang3.tuple.Pair;
import org.eqasim.server.backend.iso.RoadIsochroneBackend.Response.Destination;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTrees;

public class RoadIsochroneBackend {
	private final TravelTime travelTime;
	private final QuadTree<? extends Node> nodeIndex;

	public RoadIsochroneBackend(Network network, TravelTime travelTime) {
		this.travelTime = travelTime;
		this.nodeIndex = QuadTrees.createQuadTree(network.getNodes().values(), e -> e.getCoord(), 0.0);
	}

	public static class Request {
		public final double originX;
		public final double originY;
		public final double departureTime;
		public final double maximumTravelTime;
		public final double originRadius;

		public Request(double originX, double originY, double departureTime, double maximumTravelTime,
				double originRadius) {
			this.originX = originX;
			this.originY = originY;
			this.departureTime = departureTime;
			this.maximumTravelTime = maximumTravelTime;
			this.originRadius = originRadius;
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
			public final double distance;
			public final boolean isOrigin;

			Destination(double x, double y, double travelTime, double distance, boolean isOrigin) {
				this.x = x;
				this.y = y;
				this.travelTime = travelTime;
				this.distance = distance;
				this.isOrigin = isOrigin;
			}
		}
	}

	public Response process(Request request) {
		PriorityQueue<Pair<Node, Destination>> queue = new PriorityQueue<>((a, b) -> {
			return Double.compare(a.getRight().travelTime, b.getRight().travelTime);
		});

		IdMap<Node, Destination> destinations = new IdMap<>(Node.class);
		IdMap<Node, Double> minimumTravelTime = new IdMap<>(Node.class);

		Collection<? extends Node> originCandidates = nodeIndex.getDisk(request.originX, request.originY,
				request.originRadius);
		for (Node originNode : originCandidates) {
			Coord originCoord = originNode.getCoord();

			Destination originDestination = new Destination(originCoord.getX(), originCoord.getY(), 0.0, 0.0, true);
			queue.add(Pair.of(originNode, originDestination));
			destinations.put(originNode.getId(), originDestination);
			minimumTravelTime.put(originNode.getId(), 0.0);
		}

		while (!queue.isEmpty()) {
			var currentItem = queue.poll();

			double currentTravelTime = currentItem.getRight().travelTime;
			double currentDistance = currentItem.getRight().distance;
			Node currentNode = currentItem.getLeft();

			for (Link link : currentNode.getOutLinks().values()) {
				Node nextNode = link.getToNode();

				double enterTime = request.departureTime + currentTravelTime;
				double nextTravelTime = currentTravelTime + travelTime.getLinkTravelTime(link, enterTime, null, null);

				if (nextTravelTime < minimumTravelTime.getOrDefault(nextNode.getId(), Double.POSITIVE_INFINITY)) {
					// if (!visited.contains(nextNode.getId())) {
					double nextDistance = currentDistance + link.getLength();

					if (nextTravelTime <= request.maximumTravelTime) {
						Coord nextCoord = nextNode.getCoord();
						Destination nextDestination = new Destination(nextCoord.getX(), nextCoord.getY(),
								nextTravelTime, nextDistance, false);

						destinations.put(nextNode.getId(), nextDestination);
						queue.add(Pair.of(nextNode, nextDestination));
					}

					minimumTravelTime.put(nextNode.getId(), nextTravelTime);
				}
			}
		}

		List<Destination> destinationsList = new ArrayList<>(destinations.values());
		return new Response(destinationsList);
	}
}
