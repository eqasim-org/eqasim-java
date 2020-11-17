package org.eqasim.projects.astra16.convergence.metrics;

import java.util.List;

public interface ConvergenceMetric {
	double computeMetric(List<Double> values);
}
