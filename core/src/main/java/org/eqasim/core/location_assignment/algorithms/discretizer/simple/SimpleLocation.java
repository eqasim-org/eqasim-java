package org.eqasim.core.location_assignment.algorithms.discretizer.simple;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.eqasim.core.location_assignment.assignment.discretization.DiscreteLocation;

public class SimpleLocation implements DiscreteLocation {
	final private Vector2D location;

	public SimpleLocation(Vector2D location) {
		this.location = location;
	}

	@Override
	public Vector2D getLocation() {
		return location;
	}
}
