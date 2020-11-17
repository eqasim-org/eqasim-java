package org.eqasim.projects.astra16.convergence;

import java.util.LinkedList;
import java.util.List;

import org.eqasim.projects.astra16.convergence.metrics.ConvergenceMetric;

public class ConvergenceCriterion {
	private final ConvergenceMetric metric;
	private final double threshold;
	private final String slot;

	private final List<Double> values = new LinkedList<>();
	private final List<Double> metricValues = new LinkedList<>();

	public ConvergenceCriterion(String slot, ConvergenceMetric metric, double threshold) {
		this.metric = metric;
		this.threshold = threshold;
		this.slot = slot;
	}

	public void addValue(double value) {
		values.add(value);
		metricValues.add(metric.computeMetric(values));
	}

	public boolean isConverged() {
		if (metricValues.size() == 0) {
			return false;
		}

		return metricValues.get(metricValues.size() - 1) <= threshold;
	}

	public List<Double> getValues() {
		return values;
	}

	public List<Double> getMetricValues() {
		return metricValues;
	}

	public String getSlot() {
		return slot;
	}

	public double getThreshold() {
		return threshold;
	}
}
