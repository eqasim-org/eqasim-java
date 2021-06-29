package org.eqasim.ile_de_france.convergence;

public class ConvergenceOutputFrame {
	public long sequence;
	public ConvergenceSignal signal;

	public enum ConvergenceSignal {
		mayTerminate, doTerminate
	}
}
