package org.eqasim.core.location_assignment.assignment;

import org.eqasim.core.location_assignment.assignment.discretization.DiscreteLocationSolver;
import org.eqasim.core.location_assignment.assignment.distance.FeasibleDistanceSolver;
import org.eqasim.core.location_assignment.assignment.objective.LocationAssignmentObjectiveFunction;
import org.eqasim.core.location_assignment.assignment.relaxation.RelaxedLocationSolver;

public class LocationAssignmentSolverBuilder {
	private LocationAssignmentObjectiveFunction objectiveFunction = null;
	private FeasibleDistanceSolver feasibleDistanceSolver = null;
	private RelaxedLocationSolver relaxedLocationSolver = null;
	private DiscreteLocationSolver discreteLocationSolver = null;

	private int maximumAssignmentIterations = 1000;

	public void setMaximumAssignmentIterations(int maximumAssignmentIterations) {
		this.maximumAssignmentIterations = maximumAssignmentIterations;
	}

	public void setDiscreteLocationSolver(DiscreteLocationSolver discreteLocationSolver) {
		this.discreteLocationSolver = discreteLocationSolver;
	}

	public void setRelaxedLocationSolver(RelaxedLocationSolver relaxedLocationSolver) {
		this.relaxedLocationSolver = relaxedLocationSolver;
	}

	public void setLocationAssignmentObjectiveFunction(LocationAssignmentObjectiveFunction objectiveFunction) {
		this.objectiveFunction = objectiveFunction;
	}

	public void setFeasibleDistanceSolver(FeasibleDistanceSolver feasibleDistanceSolver) {
		this.feasibleDistanceSolver = feasibleDistanceSolver;
	}

	public LocationAssignmentSolver build() {
		if (objectiveFunction == null) {
			throw new IllegalStateException("LocationAssignmentObjectiveFunction must be set");
		}

		if (feasibleDistanceSolver == null) {
			throw new IllegalStateException("FeasibleDistanceSolver must be set");
		}

		if (relaxedLocationSolver == null) {
			throw new IllegalStateException("RelaxedLocationSolver must be set");
		}

		if (discreteLocationSolver == null) {
			throw new IllegalStateException("DiscreteLocationSolver must be set");
		}

		return new LocationAssignmentSolver(objectiveFunction, feasibleDistanceSolver, relaxedLocationSolver,
				discreteLocationSolver, maximumAssignmentIterations);
	}
}
