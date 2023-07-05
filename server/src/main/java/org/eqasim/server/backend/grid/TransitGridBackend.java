package org.eqasim.server.backend.grid;

import java.util.List;
import java.util.stream.Collectors;

import org.eqasim.server.backend.BackendScenario;
import org.eqasim.server.backend.routing.TransitRouterBackend;
import org.eqasim.server.grid.GridSource;
import org.eqasim.server.grid.SquareGrid;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorCore.TravelInfo;

public class TransitGridBackend {
	private final double maximumTravelTime;
	private final double gridSize;
	private final double accessRadius;

	private final TransitRouterBackend routerBackend;
	private final TransitSchedule schedule;

	private final QuadTree<TransitStopFacility> stopIndex;

	public TransitGridBackend(BackendScenario scenario, TransitRouterBackend routerBackend) {
		this(7200.0, 200, 400.0, scenario.getSchedule(), routerBackend);
	}

	public TransitGridBackend(double maximumTravelTime, double gridSize, double accessRadius, TransitSchedule schedule,
			TransitRouterBackend routerBackend) {
		this.maximumTravelTime = maximumTravelTime;
		this.gridSize = gridSize;
		this.accessRadius = accessRadius;
		this.routerBackend = routerBackend;
		this.schedule = schedule;
		this.stopIndex = QuadTrees.createQuadTree(schedule.getFacilities().values(), e -> e.getCoord(), 0.0);
	}

	private SquareGrid<TransitCell> createGrid() {
		GridSource source = new GridSource.Builder() //
				.add(schedule.getFacilities().values()) //
				.build();

		return new SquareGrid<>( //
				TransitCell::new, //
				gridSize, //
				source //
		);
	}

	public List<TransitCell> build(double originX, double originY, double departureTime) {
		var grid = createGrid();

		TransitRouterBackend.Configuration configuration = new TransitRouterBackend.Configuration();
		SwissRailRaptor router = routerBackend.getInstance(configuration);

		TransitStopFacility originStop = stopIndex.getClosest(originX, originY);
		var tree = router.calcTree(originStop, departureTime, null);

		for (var item : tree.entrySet()) {
			TransitStopFacility destinationStop = schedule.getFacilities().get(item.getKey());
			Coord destinationCoord = destinationStop.getCoord();

			TransitCell nextCell = grid.getCell(destinationCoord.getX(), destinationCoord.getY());
			TravelInfo info = item.getValue();

			if (info.ptTravelTime < nextCell.travelTime) {
				nextCell.travelTime = info.ptTravelTime;
				nextCell.transfers = info.transferCount;
			}

			for (TransitCell accessCell : grid.getCells(destinationCoord.getX(), destinationCoord.getY(),
					accessRadius)) {
				double accessDistance = CoordUtils.calcEuclideanDistance(destinationCoord,
						new Coord(accessCell.x, accessCell.y));
				double accessTime = 1.3 * accessDistance / 1.2;

				if (info.ptTravelTime + accessTime < accessCell.travelTime) {
					accessCell.travelTime = info.ptTravelTime + accessTime;
					accessCell.transfers = info.transferCount;
				}
			}
			
			if (nextCell.travelTime < 0.0) {
				System.err.println("here");
			}
		}

		return grid.stream().filter(cell -> Double.isFinite(cell.travelTime) && cell.travelTime < maximumTravelTime)
				.collect(Collectors.toList());
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
}
