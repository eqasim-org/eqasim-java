package org.eqasim.core.location_assignment.assignment;

import org.eqasim.core.location_assignment.assignment.discretization.DiscreteLocationResult;
import org.eqasim.core.location_assignment.assignment.distance.FeasibleDistanceResult;
import org.eqasim.core.location_assignment.assignment.objective.LocationAssignmentObjective;
import org.eqasim.core.location_assignment.assignment.relaxation.RelaxedLocationResult;

public class LocationAssignmentResult {
	final private FeasibleDistanceResult feasibleDistanceResult;
	final private RelaxedLocationResult relaxedLocationResult;
	final private DiscreteLocationResult discreteLocationResult;
	final private LocationAssignmentObjective objective;
	final private int distanceIterations;
	final private int relaxationIterations;
	private int assignmentIterations;

	public LocationAssignmentResult(FeasibleDistanceResult feasibleDistanceResult,
			RelaxedLocationResult relaxedLocationResult, DiscreteLocationResult discreteLocationResult,
			LocationAssignmentObjective objective, int distanceIterations, int relaxationIterations,
			int assignmentIterations) {
		this.feasibleDistanceResult = feasibleDistanceResult;
		this.relaxedLocationResult = relaxedLocationResult;
		this.discreteLocationResult = discreteLocationResult;
		this.objective = objective;

		this.distanceIterations = distanceIterations;
		this.relaxationIterations = relaxationIterations;
		this.assignmentIterations = assignmentIterations;
	}

	public FeasibleDistanceResult getFeasibleDistanceResult() {
		return feasibleDistanceResult;
	}

	public RelaxedLocationResult getRelaxedLocationResult() {
		return relaxedLocationResult;
	}

	public DiscreteLocationResult getDiscreteLocationResult() {
		return discreteLocationResult;
	}

	public LocationAssignmentObjective getObjective() {
		return objective;
	}

	public int getDistanceIterations() {
		return distanceIterations;
	}

	public int getRelaxationIterations() {
		return relaxationIterations;
	}

	public int getAssignmentIterations() {
		return assignmentIterations;
	}

	public void setAssignmentIterations(int assignmentIterations) {
		this.assignmentIterations = assignmentIterations;
	}
}
