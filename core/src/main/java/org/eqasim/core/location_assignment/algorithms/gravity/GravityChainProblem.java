package org.eqasim.core.location_assignment.algorithms.gravity;

import java.util.List;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public class GravityChainProblem {
	final private Vector2D originLocation;
	final private Vector2D destinationLocation;
	final private List<Double> targetDistances;

	public GravityChainProblem(Vector2D originLocation, Vector2D destinationLocation, List<Double> targetDistances) {
		this.originLocation = originLocation;
		this.destinationLocation = destinationLocation;
		this.targetDistances = targetDistances;
	}

	public Vector2D getOriginLocation() {
		return originLocation;
	}

	public Vector2D getDestinationLocation() {
		return destinationLocation;
	}

	public List<Double> getTargetDistances() {
		return targetDistances;
	}
}
