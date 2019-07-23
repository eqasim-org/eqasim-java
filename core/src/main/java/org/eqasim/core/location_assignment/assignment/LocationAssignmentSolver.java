package org.eqasim.core.location_assignment.assignment;

import org.eqasim.core.location_assignment.assignment.discretization.DiscreteLocationResult;
import org.eqasim.core.location_assignment.assignment.discretization.DiscreteLocationSolver;
import org.eqasim.core.location_assignment.assignment.distance.FeasibleDistanceResult;
import org.eqasim.core.location_assignment.assignment.distance.FeasibleDistanceSolver;
import org.eqasim.core.location_assignment.assignment.objective.LocationAssignmentObjective;
import org.eqasim.core.location_assignment.assignment.objective.LocationAssignmentObjectiveFunction;
import org.eqasim.core.location_assignment.assignment.relaxation.RelaxedLocationResult;
import org.eqasim.core.location_assignment.assignment.relaxation.RelaxedLocationSolver;

public class LocationAssignmentSolver {
	final private LocationAssignmentObjectiveFunction objectiveFunction;

	final private FeasibleDistanceSolver feasibleDistanceSolver;
	final private RelaxedLocationSolver relaxedLocationSolver;
	final private DiscreteLocationSolver discreteLocationSolver;

	final private int maximumIterations;

	public LocationAssignmentSolver(LocationAssignmentObjectiveFunction objectiveFunction,
			FeasibleDistanceSolver feasibleDistanceSolver, RelaxedLocationSolver relaxedLocationSolver,
			DiscreteLocationSolver discreteLocationSolver, int maximumIterations) {
		this.objectiveFunction = objectiveFunction;
		this.feasibleDistanceSolver = feasibleDistanceSolver;
		this.relaxedLocationSolver = relaxedLocationSolver;
		this.discreteLocationSolver = discreteLocationSolver;
		this.maximumIterations = maximumIterations;
	}

	public LocationAssignmentResult solve(LocationAssignmentProblem problem) {
		LocationAssignmentResult assignmentResult = null;
		double bestObjective = Double.POSITIVE_INFINITY;

		int assignmentIterations = 0;
		int distanceIterations = 0;
		int relaxationIterations = 0;

		boolean isConverged;

		do {
			isConverged = true;

			// Sample a feasible chain of distances
			FeasibleDistanceResult feasibleDistanceResult = feasibleDistanceSolver.solve(problem);
			distanceIterations += feasibleDistanceResult.getIterations();
			isConverged &= feasibleDistanceResult.isConverged();

			// Sample new realization of locations based on feasible distances
			RelaxedLocationResult relaxedLocationResult = relaxedLocationSolver.solve(problem, feasibleDistanceResult);
			relaxationIterations += relaxedLocationResult.getIterations();
			isConverged &= relaxedLocationResult.isConverged();

			// Discretized relaxed locations
			DiscreteLocationResult discreteLocationResult = discreteLocationSolver.solve(problem,
					feasibleDistanceResult, relaxedLocationResult);

			// Compute objective
			LocationAssignmentObjective objective = objectiveFunction.computeObjective(problem, feasibleDistanceResult,
					relaxedLocationResult, discreteLocationResult);
			isConverged &= objective.isConverged();

			if (assignmentResult == null || objective.getValue() < bestObjective) {
				bestObjective = objective.getValue();
				assignmentResult = new LocationAssignmentResult(feasibleDistanceResult, relaxedLocationResult,
						discreteLocationResult, objective, distanceIterations, relaxationIterations,
						assignmentIterations);
			}

			assignmentIterations++;
		} while (assignmentIterations < maximumIterations && !isConverged);

		assignmentResult.setAssignmentIterations(assignmentIterations);
		return assignmentResult;
	}
}
