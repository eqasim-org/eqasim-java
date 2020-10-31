package org.eqasim.projects.astra16.convergence.metrics;

import java.util.List;

public class AbsoluteDifference implements ConvergenceMetric {
	@Override
	public double computeMetric(List<Double> values) {
		double firstValue = values.get(0);
		double lastValue = values.get(values.size() - 1);
		return Math.abs(firstValue - lastValue);
	}
}
