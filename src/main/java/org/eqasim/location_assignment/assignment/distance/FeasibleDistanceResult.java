package org.eqasim.location_assignment.assignment.distance;

import java.util.List;

public interface FeasibleDistanceResult {
	List<Double> getTargetDistances();

	boolean isConverged();
}
