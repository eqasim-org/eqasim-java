package org.eqasim.core.location_assignment.assignment;

import java.util.Optional;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public interface LocationAssignmentProblem {
	Optional<Vector2D> getOriginLocation();

	Optional<Vector2D> getDestinationLocation();
}
