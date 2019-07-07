package org.eqasim.core.location_assignment.assignment.objective;

import org.eqasim.core.location_assignment.assignment.LocationAssignmentProblem;
import org.eqasim.core.location_assignment.assignment.discretization.DiscreteLocationResult;
import org.eqasim.core.location_assignment.assignment.distance.FeasibleDistanceResult;
import org.eqasim.core.location_assignment.assignment.relaxation.RelaxedLocationResult;

public interface LocationAssignmentObjectiveFunction {
	LocationAssignmentObjective computeObjective(LocationAssignmentProblem problem,
			FeasibleDistanceResult feasibleDistanceResult, RelaxedLocationResult relaxedResult,
			DiscreteLocationResult discreteResult);
}
