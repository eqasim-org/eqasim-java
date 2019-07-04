package org.eqasim.location_assignment.assignment.discretization;

import org.eqasim.location_assignment.assignment.LocationAssignmentProblem;
import org.eqasim.location_assignment.assignment.distance.FeasibleDistanceResult;
import org.eqasim.location_assignment.assignment.relaxation.RelaxedLocationResult;

public interface DiscreteLocationSolver {
	DiscreteLocationResult solve(LocationAssignmentProblem problem, FeasibleDistanceResult feasibleDistanceResult,
			RelaxedLocationResult relaxedLocationResult);
}
