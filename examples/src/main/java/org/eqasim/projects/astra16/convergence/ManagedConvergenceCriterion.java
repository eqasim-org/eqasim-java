package org.eqasim.projects.astra16.convergence;

import org.eqasim.core.analysis.AstraConvergence;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

public class ManagedConvergenceCriterion implements IterationEndsListener, TerminationCriterion {
	private final ConvergenceManager manager;

	public ManagedConvergenceCriterion(ConvergenceManager manager) {
		this.manager = manager;
	}

	private int triggerIteration = -1000;

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (manager.isConverged() || event.getIteration() == 2) {
			AstraConvergence.IS_CONVERGED = true;
			triggerIteration = event.getIteration() + 2;
		}
	}

	@Override
	public boolean continueIterations(int iteration) {
		if (iteration == triggerIteration) {
			return false;
		}

		return true;
	}
}
