package org.eqasim.core.location_assignment.algorithms.angular;

import java.util.List;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public class AngularTailResult {
	final private List<Vector2D> locations;

	public AngularTailResult(List<Vector2D> locations) {
		this.locations = locations;
	}

	public List<Vector2D> getLocations() {
		return locations;
	}

	public boolean isConverged() {
		return true;
	}
}
