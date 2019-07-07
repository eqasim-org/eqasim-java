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

	final private boolean useIterativeFeasibleSolution;
	final private int maximumIterations;

	public LocationAssignmentSolver(LocationAssignmentObjectiveFunction objectiveFunction,
			FeasibleDistanceSolver feasibleDistanceSolver, RelaxedLocationSolver relaxedLocationSolver,
			DiscreteLocationSolver discreteLocationSolver, boolean useIterativeFeasibleSolution,
			int maximumIterations) {
		this.objectiveFunction = objectiveFunction;
		this.feasibleDistanceSolver = feasibleDistanceSolver;
		this.relaxedLocationSolver = relaxedLocationSolver;
		this.discreteLocationSolver = discreteLocationSolver;
		this.useIterativeFeasibleSolution = useIterativeFeasibleSolution;
		this.maximumIterations = maximumIterations;
	}

	public LocationAssignmentResult solve(LocationAssignmentProblem problem) {
		FeasibleDistanceResult feasibleDistanceResult = null;
		LocationAssignmentResult bestResult = null;

		int discretizationIteration = 0;

		do {
			if (discretizationIteration == 0 || useIterativeFeasibleSolution) {
				// Sample a feasible chain of distances
				feasibleDistanceResult = feasibleDistanceSolver.solve(problem);
			}

			// Sample new realization of locations based on feasible distances
			RelaxedLocationResult relaxedLocationResult = relaxedLocationSolver.solve(problem, feasibleDistanceResult);

			// Discretized relaxed locations
			DiscreteLocationResult discreteLocationResult = discreteLocationSolver.solve(problem,
					feasibleDistanceResult, relaxedLocationResult);

			// Compute objective
			LocationAssignmentObjective objective = objectiveFunction.computeObjective(problem, feasibleDistanceResult,
					relaxedLocationResult, discreteLocationResult);
			LocationAssignmentResult result = new LocationAssignmentResult(feasibleDistanceResult,
					relaxedLocationResult, discreteLocationResult, objective);

			if (bestResult == null || result.getObjective().getValue() < bestResult.getObjective().getValue()) {
				bestResult = result;
			}

			discretizationIteration++;
		} while (discretizationIteration < maximumIterations && !bestResult.getObjective().isConverged());

		return bestResult;
	}
}
