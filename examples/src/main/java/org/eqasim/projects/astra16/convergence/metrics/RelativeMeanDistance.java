package org.eqasim.projects.astra16.convergence.metrics;

import java.util.List;

public class RelativeMeanDistance implements ConvergenceMetric {
	@Override
	public double computeMetric(List<Double> values) {
		double mean = values.stream().mapToDouble(d -> d).sum() / values.size();
		return Math.abs((values.get(values.size() - 1) - mean / mean));
	}
}
