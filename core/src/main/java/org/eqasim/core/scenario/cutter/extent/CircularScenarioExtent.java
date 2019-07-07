package org.eqasim.core.scenario.cutter.extent;

import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;

public class CircularScenarioExtent implements ScenarioExtent {
	final private Coord center;
	final private double radius;

	public CircularScenarioExtent(Coord center, double radius) {
		this.center = center;
		this.radius = radius;
	}

	@Override
	public boolean isInside(Coord coord) {
		return CoordUtils.calcEuclideanDistance(coord, center) <= radius;
	}

	@Override
	public List<Coord> computeEuclideanIntersections(Coord from, Coord to) {
		if (isInside(from) == isInside(to)) {
			return Collections.emptyList();
		} else {
			Coord ab = CoordUtils.minus(to, from);
			Coord ca = CoordUtils.minus(center, from);

			double D = ab.getX() * ab.getX() + ab.getY() * ab.getY();
			double p = (ca.getX() * ab.getX() + ca.getY() * ab.getY()) / D;

			double q = (ca.getX() * ca.getX() + ca.getY() * ca.getY() - radius * radius) / D;

			double s1 = p + Math.sqrt(p * p - q);
			double s2 = p - Math.sqrt(p * p - q);

			if (s1 >= 0.0 && s1 <= 1.0) {
				return Collections.singletonList(CoordUtils.plus(from, CoordUtils.scalarMult(s1, ab)));
			} else if (s2 > 0 && s2 <= 1.0) {
				return Collections.singletonList(CoordUtils.plus(from, CoordUtils.scalarMult(s2, ab)));
			} else {
				throw new IllegalStateException();
			}
		}
	}

	@Override
	public Coord getInteriorPoint() {
		return center;
	}
}
