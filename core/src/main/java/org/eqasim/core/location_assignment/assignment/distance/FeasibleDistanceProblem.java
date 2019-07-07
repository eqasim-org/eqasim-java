package org.eqasim.core.location_assignment.assignment.distance;

import java.util.Optional;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;


public interface FeasibleDistanceProblem {
	Optional<Vector2D> getOriginalLocation();

	Optional<Vector2D> getDestinationLocation();
}
