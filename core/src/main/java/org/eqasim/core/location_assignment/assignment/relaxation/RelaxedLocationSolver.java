package org.eqasim.core.location_assignment.assignment.relaxation;

import org.eqasim.core.location_assignment.assignment.LocationAssignmentProblem;
import org.eqasim.core.location_assignment.assignment.distance.FeasibleDistanceResult;

public interface RelaxedLocationSolver {
	RelaxedLocationResult solve(LocationAssignmentProblem problem, FeasibleDistanceResult feasibleDistanceResult);
}
