package org.eqasim.core.scenario.spatial;

import org.eqasim.core.misc.ParallelProgress;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class AdjustCapacity {
	private final Geometry geometry;
	private final GeometryFactory factory = new GeometryFactory();

	private final double factor;

	public AdjustCapacity(Geometry geometry, double factor) {
		this.geometry = geometry;
		this.factor = factor;
	}

	public void run(Network network) throws InterruptedException {
		ParallelProgress progress = new ParallelProgress("Adjusting network capacity ...", network.getLinks().size());

		for (Link link : network.getLinks().values()) {
			Point point = factory.createPoint(new Coordinate(link.getCoord().getX(), link.getCoord().getY()));

			if (geometry.contains(point)) {
				link.setCapacity(link.getCapacity() * factor);
			}
		}

		progress.close();
	}
}
