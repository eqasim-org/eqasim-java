package org.eqasim.core.simulation.convergence.criterion;

import org.eqasim.core.simulation.convergence.ConvergenceSignal;

public interface ConvergenceCriterion {
	boolean checkConvergence(int iteration, ConvergenceSignal signal);
}
