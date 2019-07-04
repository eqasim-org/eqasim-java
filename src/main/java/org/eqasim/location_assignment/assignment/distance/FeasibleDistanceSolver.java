package org.eqasim.location_assignment.assignment.distance;

import org.eqasim.location_assignment.assignment.LocationAssignmentProblem;

public interface FeasibleDistanceSolver {
	FeasibleDistanceResult solve(LocationAssignmentProblem problem);
}
