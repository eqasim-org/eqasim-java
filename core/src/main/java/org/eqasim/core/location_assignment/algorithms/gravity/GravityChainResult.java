package org.eqasim.core.location_assignment.algorithms.gravity;

import java.util.List;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public class GravityChainResult {
	final private boolean isConverged;
	final private boolean isFeasible;

	final private List<Vector2D> locations;
	final private long iterations;

	public GravityChainResult(boolean isFeasible, boolean isConverged, List<Vector2D> locations, long iterations) {
		this.isFeasible = isFeasible;
		this.isConverged = isConverged;
		this.locations = locations;
		this.iterations = iterations;
	}

	public List<Vector2D> getLocations() {
		return locations;
	}

	public boolean isConverged() {
		return isConverged;
	}

	public boolean isFeasible() {
		return isFeasible;
	}

	public long getIterations() {
		return iterations;
	}
}
