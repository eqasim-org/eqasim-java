package org.eqasim.core.simulation.convergence;

public class ConvergenceResult {
	public final double objective;
	public final boolean converged;

	public ConvergenceResult(boolean converged, double objective) {
		this.objective = objective;
		this.converged = converged;
	}
}
