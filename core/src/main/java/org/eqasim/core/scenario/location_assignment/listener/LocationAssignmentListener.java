package org.eqasim.core.scenario.location_assignment.listener;

import java.util.Collection;

import org.eqasim.core.location_assignment.matsim.solver.MATSimSolverResult;

public interface LocationAssignmentListener {
	void process(Collection<MATSimSolverResult> result);
	void update();
}
