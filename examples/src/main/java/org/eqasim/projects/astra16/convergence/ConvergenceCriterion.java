package org.eqasim.projects.astra16.convergence;

import java.util.LinkedList;
import java.util.List;

import org.eqasim.projects.astra16.convergence.metrics.ConvergenceMetric;

public class ConvergenceCriterion {
	private final ConvergenceMetric metric;
	private final int horizon;
	private final double threshold;
	private final String slot;

	private final List<Double> values = new LinkedList<>();
	private final List<Double> metricValues = new LinkedList<>();

	public ConvergenceCriterion(String slot, ConvergenceMetric metric, double threshold, int horizon) {
		this.metric = metric;
		this.horizon = horizon;
		this.threshold = threshold;
		this.slot = slot;
	}

	public void addValue(double value) {
		values.add(value);

		if (horizon == 0) {
			metricValues.add(metric.computeMetric(values));
		} else if (values.size() < horizon) {
			metricValues.add(Double.POSITIVE_INFINITY);
		} else {
			List<Double> segment = values.subList(values.size() - horizon, values.size());
			metricValues.add(metric.computeMetric(segment));
		}
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

	public int getHorizon() {
		return horizon;
	}

	public double getThreshold() {
		return threshold;
	}
}
