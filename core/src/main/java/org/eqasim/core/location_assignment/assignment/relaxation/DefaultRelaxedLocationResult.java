package org.eqasim.core.location_assignment.assignment.relaxation;

import java.util.List;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public class DefaultRelaxedLocationResult implements RelaxedLocationResult {
	private final List<Vector2D> relaxedLocations;
	private final boolean isConverged;
	private final int iterations;

	public DefaultRelaxedLocationResult(boolean isConverged, int iterations, List<Vector2D> relaxedLocations) {
		this.isConverged = isConverged;
		this.iterations = iterations;
		this.relaxedLocations = relaxedLocations;
	}

	@Override
	public List<Vector2D> getRelaxedLocations() {
		return relaxedLocations;
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
