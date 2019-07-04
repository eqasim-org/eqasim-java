package org.eqasim.location_assignment.assignment.objective;

public interface LocationAssignmentObjective {
	boolean isConverged();

	double getValue();
}
