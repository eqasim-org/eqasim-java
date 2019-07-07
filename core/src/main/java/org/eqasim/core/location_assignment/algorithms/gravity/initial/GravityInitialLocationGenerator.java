package org.eqasim.core.location_assignment.algorithms.gravity.initial;

import java.util.List;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public interface GravityInitialLocationGenerator {
	List<Vector2D> generate(int numberOfLocations, Vector2D originLocation, Vector2D destinationLocation);
}
