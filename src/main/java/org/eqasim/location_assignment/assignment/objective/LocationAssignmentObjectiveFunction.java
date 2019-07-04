package org.eqasim.location_assignment.assignment.objective;

import org.eqasim.location_assignment.assignment.LocationAssignmentProblem;
import org.eqasim.location_assignment.assignment.discretization.DiscreteLocationResult;
import org.eqasim.location_assignment.assignment.distance.FeasibleDistanceResult;
import org.eqasim.location_assignment.assignment.relaxation.RelaxedLocationResult;

public interface LocationAssignmentObjectiveFunction {
	LocationAssignmentObjective computeObjective(LocationAssignmentProblem problem,
			FeasibleDistanceResult feasibleDistanceResult, RelaxedLocationResult relaxedResult,
			DiscreteLocationResult discreteResult);
}
