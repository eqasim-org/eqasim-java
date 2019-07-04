package org.eqasim.location_assignment.matsim.solver;

import org.eqasim.location_assignment.assignment.LocationAssignmentResult;
import org.eqasim.location_assignment.matsim.MATSimAssignmentProblem;

public class MATSimSolverResult {
	final private MATSimAssignmentProblem problem;
	final private LocationAssignmentResult result;

	public MATSimSolverResult(MATSimAssignmentProblem problem, LocationAssignmentResult result) {
		this.problem = problem;
		this.result = result;
	}

	public MATSimAssignmentProblem getProblem() {
		return problem;
	}

	public LocationAssignmentResult getResult() {
		return result;
	}
}
