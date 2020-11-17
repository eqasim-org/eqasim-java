package org.eqasim.projects.astra16.convergence.metrics;

import java.util.List;

public class RelativeMeanDistance implements ConvergenceMetric {
	private final int horizon;

	public RelativeMeanDistance(int horizon) {
		this.horizon = horizon;
	}

	@Override
	public double computeMetric(List<Double> values) {
		if (values.size() < horizon) {
			return Double.POSITIVE_INFINITY;
		}

		double mean = values.stream().skip(values.size() - horizon).mapToDouble(d -> d).sum() / horizon;
		return Math.abs((values.get(values.size() - 1) - mean) / mean);
	}
}
