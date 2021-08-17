package org.eqasim.core.simulation.convergence.criterion;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eqasim.core.simulation.convergence.ConvergenceSignal;
import org.jfree.chart.plot.XYPlot;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.charts.XYLineChart;

public class DerivativeCriterion implements ConvergenceCriterion {
	private final OutputDirectoryHierarchy outputHierarchy;

	private final int horizon;
	private final double firstDerivativeThresholdValue;
	private final double secondDerivativeFactor;

	public DerivativeCriterion(OutputDirectoryHierarchy outputHierarchy, int horizon, double firstDerivativeThreshold,
			double secondDerivativeFactor) {
		this.horizon = horizon;
		this.firstDerivativeThresholdValue = firstDerivativeThreshold;
		this.secondDerivativeFactor = secondDerivativeFactor;
		this.outputHierarchy = outputHierarchy;
	}

	@Override
	public boolean checkConvergence(int iteration, ConvergenceSignal signal) {
		List<Double> values = new ArrayList<>(Collections.nCopies(signal.getValues().size(), Double.NaN));

		int smoothing = 10;

		for (int i = smoothing; i < values.size() - smoothing; i++) {
			values.set(i, signal.getValues().subList(i - smoothing, i + smoothing).stream().mapToDouble(d -> d)
					.average().getAsDouble());
		}

		List<Double> firstDerivative = new ArrayList<>(Collections.nCopies(values.size(), Double.NaN));
		List<Double> secondDerivative = new ArrayList<>(Collections.nCopies(values.size(), Double.NaN));
		List<Boolean> directionChange = new ArrayList<>(Collections.nCopies(values.size(), true));

		for (int i = horizon; i < values.size() - horizon; i++) {
			firstDerivative.set(i, (values.get(i + horizon) - values.get(i - horizon)) / (2 * horizon));
		}

		for (int i = horizon; i < values.size() - horizon; i++) {
			double firstValue = firstDerivative.get(i - horizon);
			double secondValue = firstDerivative.get(i + horizon);

			if (Double.isFinite(firstValue) && Double.isFinite(secondValue)) {
				if (Math.signum(firstValue) == Math.signum(secondValue)) {
					directionChange.set(i, false);
				}
			}
		}

		for (int i = horizon * 2; i < values.size() - horizon * 2; i++) {
			secondDerivative.set(i,
					(firstDerivative.get(i + horizon) - firstDerivative.get(i - horizon)) / (2 * horizon));
		}

		List<Double> firstDerivativeThreshold = new ArrayList<>(Collections.nCopies(values.size(), Double.NaN));
		List<Double> secondDerivativeThreshold = new ArrayList<>(Collections.nCopies(values.size(), Double.NaN));

		double secondDerivativeThresholdBasis = Double.NaN;

		for (int i = 0; i < values.size(); i++) {
			// firstDerivativeThreshold.set(i, Math.abs(values.get(i)) *
			// firstDerivativeThresholdValue);
			firstDerivativeThreshold.set(i, firstDerivativeThresholdValue);

			if (Double.isNaN(secondDerivativeThresholdBasis)
					&& Math.abs(firstDerivative.get(i)) < firstDerivativeThresholdValue) {
				secondDerivativeThresholdBasis = firstDerivative.get(i);
			}

			if (directionChange.get(i)) {
				firstDerivativeThreshold.set(i, 0.0); // Double.NaN);
			}
		}

		for (int i = 0; i < firstDerivative.size(); i++) {
			// secondDerivativeThreshold.set(i, Math.abs(firstDerivative.get(i)) *
			// secondDerivativeFactor);
			secondDerivativeThreshold.set(i, Math.abs(firstDerivativeThresholdValue) * secondDerivativeFactor);
		}

		List<Boolean> converged = new ArrayList<>(Collections.nCopies(values.size(), false));
		boolean isConverged = false;

		for (int i = 0; i < values.size() - horizon * 2; i++) {
			if (isConverged) {
				converged.set(i + horizon * 2, true);
			} else if (Math.abs(firstDerivative.get(i)) <= firstDerivativeThreshold.get(i)) {
				// if (Math.abs(secondDerivative.get(i)) <= secondDerivativeThreshold.get(i)) {
				if (!directionChange.get(i)) {
					// isConverged = true;
					converged.set(i + horizon * 2, true);
				}
				// }
			}
		}

		writeOutput(signal.getName(), values, firstDerivative, secondDerivative, directionChange,
				firstDerivativeThreshold, secondDerivativeThreshold, converged);
		writeGraphs(signal.getName(), values, firstDerivative, secondDerivative, firstDerivativeThreshold,
				secondDerivativeThreshold, converged);

		return converged.get(converged.size() - 1);
	}

	private void writeOutput(String name, List<Double> values, List<Double> firstDerivative,
			List<Double> secondDerivative, List<Boolean> directionChange, List<Double> firstDerivativeThreshold,
			List<Double> secondDerivativeThreshold, List<Boolean> converged) {
		try {
			File outputPath = new File(outputHierarchy.getOutputFilename("convergence_deriv_" + name + ".csv"));

			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

			writer.write(String.join(";", new String[] { "iteration", "value", "first_derivative", "second_derivative",
					"direction_change", "first_derivative_threshold", "second_derivative_threshold", "converged" })
					+ "\n");

			for (int i = 0; i < values.size(); i++) {
				writer.write(String.join(";", new String[] { //
						String.valueOf(i), //
						String.valueOf(values.get(i)), //
						String.valueOf(firstDerivative.get(i)), //
						String.valueOf(secondDerivative.get(i)), //
						String.valueOf(directionChange.get(i)), //
						String.valueOf(firstDerivativeThreshold.get(i)), //
						String.valueOf(secondDerivativeThreshold.get(i)), //
						String.valueOf(converged.get(i)), //
				}) + "\n");
			}

			writer.close();

		} catch (IOException e) {
		}
	}

	private Map<Integer, Double> createSeries(List<Double> values, boolean invert) {
		Map<Integer, Double> series = new TreeMap<>();

		for (int i = 0; i < values.size(); i++) {
			series.put(i, values.get(i) * (invert ? -1.0 : 1.0));
		}

		return series;
	}

	private void writeGraphs(String name, List<Double> values, List<Double> firstDerivative,
			List<Double> secondDerivative, List<Double> firstDerivativeThreshold,
			List<Double> secondDerivativeThreshold, List<Boolean> converged) {
		Map<Integer, Double> valueSeries = createSeries(values, false);
		Map<Integer, Double> firstDerivativeSeries = createSeries(firstDerivative, false);
		Map<Integer, Double> secondDerivativeSeries = createSeries(secondDerivative, false);
		Map<Integer, Double> firstDerivativeLowerBoundSeries = createSeries(firstDerivativeThreshold, true);
		Map<Integer, Double> firstDerivativeUpperBoundSeries = createSeries(firstDerivativeThreshold, false);
		Map<Integer, Double> secondDerivativeLowerBoundSeries = createSeries(secondDerivativeThreshold, true);
		Map<Integer, Double> secondDerivativeUpperBoundSeries = createSeries(secondDerivativeThreshold, false);

		{
			File outputPath = new File(outputHierarchy.getOutputFilename("convergence_deriv_" + name + "_value.png"));
			XYLineChart chart = new XYLineChart("Convergence: " + name + " (Value)", "Iteration", "Value");
			chart.addSeries("Value", valueSeries);
			chart.saveAsPng(outputPath.toString(), 1280, 720);
		}

		{
			File outputPath = new File(outputHierarchy.getOutputFilename("convergence_deriv_" + name + "_first.png"));
			XYLineChart chart = new XYLineChart("Convergence: " + name + " (1st Derivative)", "Iteration", "Value");
			chart.addSeries("1st Derivative", firstDerivativeSeries);
			chart.addSeries("Lower bound", firstDerivativeLowerBoundSeries);
			chart.addSeries("Upper bound", firstDerivativeUpperBoundSeries);
			((XYPlot) chart.getChart().getPlot()).getRenderer().setSeriesPaint(1, Color.black);
			((XYPlot) chart.getChart().getPlot()).getRenderer().setSeriesPaint(2, Color.black);
			chart.saveAsPng(outputPath.toString(), 1280, 720);
		}

		{
			File outputPath = new File(outputHierarchy.getOutputFilename("convergence_deriv_" + name + "_second.png"));
			XYLineChart chart = new XYLineChart("Convergence: " + name + " (2nd Derivative)", "Iteration", "Value");
			chart.addSeries("2nd Derivative", secondDerivativeSeries);
			chart.addSeries("Lower bound", secondDerivativeLowerBoundSeries);
			chart.addSeries("Upper bound", secondDerivativeUpperBoundSeries);
			((XYPlot) chart.getChart().getPlot()).getRenderer().setSeriesPaint(1, Color.black);
			((XYPlot) chart.getChart().getPlot()).getRenderer().setSeriesPaint(2, Color.black);
			chart.saveAsPng(outputPath.toString(), 1280, 720);
		}
	}
}
