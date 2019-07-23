package org.eqasim.core.location_assignment.assignment.distance;

import java.util.List;

public class DefaultFeasibleDistanceResult implements FeasibleDistanceResult {
	private final List<Double> targetDistances;
	private final boolean isConverged;
	private final int iterations;

	public DefaultFeasibleDistanceResult(boolean isConverged, int iterations, List<Double> targetDistances) {
		this.isConverged = isConverged;
		this.iterations = iterations;
		this.targetDistances = targetDistances;
	}

	@Override
	public List<Double> getTargetDistances() {
		return targetDistances;
	}

	@Override
	public boolean isConverged() {
		return isConverged;
	}

	@Override
	public int getIterations() {
		return iterations;
	}
}
