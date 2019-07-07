package org.eqasim.core.scenario.cutter.extent;

import java.util.List;

import org.matsim.api.core.v01.Coord;

public interface ScenarioExtent {
	boolean isInside(Coord coord);

	List<Coord> computeEuclideanIntersections(Coord from, Coord to);

	Coord getInteriorPoint();
}
