package org.eqasim.core.location_assignment.algorithms.angular;

import java.util.List;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public class AngularTailProblem {
	final private Vector2D anchorLocation;
	final private List<Double> targetDistances;

	public AngularTailProblem(Vector2D anchorLocation, List<Double> targetDistances) {
		if (targetDistances.size() == 0) {
			throw new IllegalArgumentException("AngularTailProblem must have at least one trip");
		}

		this.anchorLocation = anchorLocation;
		this.targetDistances = targetDistances;
	}

	public Vector2D getAnchorLocation() {
		return anchorLocation;
	}

	public List<Double> getTargetDistances() {
		return targetDistances;
	}
}
