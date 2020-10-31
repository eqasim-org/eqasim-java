package org.eqasim.projects.astra16.convergence.metrics;

import java.util.List;

public class AbsoluteDifference implements ConvergenceMetric {
	private final int lag;
	
	public AbsoluteDifference(int lag) {
		this.lag = lag;
	}
	
	@Override
	public double computeMetric(List<Double> values) {
		if (values.size() - 1 < lag) {
			return Double.POSITIVE_INFINITY;
		}
		
		double firstValue = values.get(values.size() - lag - 1);
		double lastValue = values.get(values.size() - 1);
		
		return Math.abs(firstValue - lastValue);
	}
}
