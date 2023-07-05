package org.eqasim.server.backend.grid;

import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.eqasim.server.backend.BackendScenario;
import org.eqasim.server.grid.GridSource;
import org.eqasim.server.grid.SquareGrid;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

public class RoadGridBackend {
	private final double maximumTravelTime;
	private final double gridSize;

	private final Network network;
	private final TravelTime travelTime;

	public RoadGridBackend(BackendScenario scenario) {
		this(7200.0, 200, scenario.getCarNetwork(), new FreeSpeedTravelTime());
	}

	public RoadGridBackend(double maximumTravelTime, double gridSize, Network network, TravelTime travelTime) {
		this.maximumTravelTime = maximumTravelTime;
		this.gridSize = gridSize;
		this.network = network;
		this.travelTime = travelTime;
	}

	private SquareGrid<RoadCell> createGrid() {
		GridSource source = new GridSource.Builder() //
				.add(network.getNodes().values()) //
				.build();

		return new SquareGrid<>( //
				RoadCell::new, //
				gridSize, //
				source);
	}

	public List<RoadCell> build(double originX, double originY, double departureTime) {
		var grid = createGrid();

		PriorityQueue<Pair<Double, Node>> queue = new PriorityQueue<>((a, b) -> {
			return Double.compare(a.getLeft(), b.getLeft());
		});

		Node originNode = NetworkUtils.getNearestNode(network, new Coord(originX, originY));
		queue.add(Pair.of(0.0, originNode));

		IdSet<Node> visited = new IdSet<>(Node.class);
		visited.add(originNode.getId());

		while (!queue.isEmpty()) {
			var currentItem = queue.poll();

			double currentTravelTime = currentItem.getLeft();
			Node currentNode = currentItem.getRight();

			for (Link link : currentNode.getOutLinks().values()) {
				Node nextNode = link.getToNode();

				if (!visited.contains(nextNode.getId())) {
					double enterTime = departureTime + currentTravelTime;
					double nextTravelTime = currentTravelTime
							+ travelTime.getLinkTravelTime(link, enterTime, null, null);

					if (nextTravelTime <= maximumTravelTime) {
						queue.add(Pair.of(nextTravelTime, nextNode));

						Coord nextCoord = nextNode.getCoord();
						RoadCell nextCell = grid.getCell(nextCoord.getX(), nextCoord.getY());
						nextCell.travelTime = Math.min(nextCell.travelTime, nextTravelTime);
					}
				}

				visited.add(nextNode.getId());
			}
		}

		return grid.stream().filter(cell -> Double.isFinite(cell.travelTime)).collect(Collectors.toList());
	}

	static public class RoadCell {
		public final double x;
		public final double y;

		public final int u;
		public final int v;

		public double travelTime = Double.POSITIVE_INFINITY;

		RoadCell(int u, int v, double x, double y) {
			this.x = x;
			this.y = y;
			this.u = u;
			this.v = v;
		}
	}
}
