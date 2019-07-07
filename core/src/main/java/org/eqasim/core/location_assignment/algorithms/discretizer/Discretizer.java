package org.eqasim.core.location_assignment.algorithms.discretizer;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public interface Discretizer {
	DiscretizerResult discretize(Vector2D location);
}
