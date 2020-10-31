package org.eqasim.projects.astra16.convergence.metrics;

import java.util.List;

public class RelativeMeanDifference implements ConvergenceMetric {
	private final int horizon;
	private final int lag;

	public RelativeMeanDifference(int horizon, int lag) {
		this.horizon = horizon;
		this.lag = lag;
	}

	@Override
	public double computeMetric(List<Double> values) {
		if (values.size() < horizon + lag) {
			return Double.POSITIVE_INFINITY;
		}

		double firstMean = values.stream().skip(values.size() - horizon - lag).limit(horizon).mapToDouble(d -> d).sum()
				/ horizon;
		double secondMean = values.stream().skip(values.size() - horizon).mapToDouble(d -> d).sum() / horizon;

		return Math.abs((secondMean - firstMean) / firstMean);
	}
}
