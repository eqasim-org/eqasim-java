package org.eqasim.core.simulation.termination;

import java.util.List;

public interface TerminationCriterionCalculator {
	double calculate(List<TerminationData> history, IterationData iteration);
}
