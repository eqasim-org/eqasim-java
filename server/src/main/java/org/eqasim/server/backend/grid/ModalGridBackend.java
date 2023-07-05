package org.eqasim.server.backend.grid;

import java.util.List;
import java.util.stream.Collectors;

import org.eqasim.server.backend.BackendScenario;
import org.eqasim.server.grid.GridSource;
import org.eqasim.server.grid.SquareGrid;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class ModalGridBackend {
	private final double gridSize;

	private final Network network;
	private final TransitSchedule schedule;

	private final RoadGridBackend roadBuilder;
	private final TransitGridBackend transitBuilder;

	public ModalGridBackend(BackendScenario scenario, RoadGridBackend roadBuilder, TransitGridBackend transitBuilder) {
		this(200, scenario.getCarNetwork(), scenario.getSchedule(), roadBuilder, transitBuilder);
	}

	public ModalGridBackend(double gridSize, Network network, TransitSchedule schedule, RoadGridBackend roadBuilder,
			TransitGridBackend transitBuilder) {
		this.gridSize = gridSize;

		this.network = network;
		this.schedule = schedule;

		this.roadBuilder = roadBuilder;
		this.transitBuilder = transitBuilder;
	}

	private SquareGrid<ModalCell> createGrid() {
		GridSource source = new GridSource.Builder() //
				.add(network.getNodes().values()) //
				.add(schedule.getFacilities().values()) //
				.build();

		return new SquareGrid<>( //
				ModalCell::new, //
				gridSize, //
				source);
	}

	public List<ModalCell> build(double originX, double originY, double departureTime) {
		var grid = createGrid();

		for (var roadCell : roadBuilder.build(originX, originY, departureTime)) {
			ModalCell modalCell = grid.getCell(roadCell.x, roadCell.y);
			modalCell.roadTravelTime = roadCell.travelTime;
		}

		for (var transitCell : transitBuilder.build(originX, originY, departureTime)) {
			ModalCell modalCell = grid.getCell(transitCell.x, transitCell.y);
			modalCell.transitTravelTime = transitCell.travelTime;
		}

		return grid.stream()
				.filter(cell -> Double.isFinite(cell.roadTravelTime) || Double.isFinite(cell.transitTravelTime))
				.collect(Collectors.toList());
	}

	static public class ModalCell {
		public final double x;
		public final double y;

		public final int u;
		public final int v;

		public double roadTravelTime = Double.POSITIVE_INFINITY;
		public double transitTravelTime = Double.POSITIVE_INFINITY;

		ModalCell(int u, int v, double x, double y) {
			this.x = x;
			this.y = y;
			this.u = u;
			this.v = v;
		}
	}
}
