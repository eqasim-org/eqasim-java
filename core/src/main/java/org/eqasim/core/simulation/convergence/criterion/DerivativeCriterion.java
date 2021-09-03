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
	private final int smoothing;

	private final double firstDerivativeThreshold;
	private final double directionChangeThreshold;

	private final int backlogShift;

	public DerivativeCriterion(OutputDirectoryHierarchy outputHierarchy, int smoothing, int horizon,
			double firstDerivativeThreshold, double directionChangeThreshold) {
		this.smoothing = smoothing;
		this.horizon = horizon;

		this.firstDerivativeThreshold = firstDerivativeThreshold;
		this.directionChangeThreshold = directionChangeThreshold;

		this.outputHierarchy = outputHierarchy;

		this.backlogShift = smoothing + horizon * 2 + 1;
	}

	@Override
	public boolean checkConvergence(int iteration, ConvergenceSignal signal) {
		int samples = signal.getValues().size();
		int backlogIteration = samples - backlogShift;

		// Smooth values

		List<Double> values = new ArrayList<>(Collections.nCopies(samples, Double.NaN));

		for (int i = smoothing; i < samples - smoothing; i++) {
			values.set(i, signal.getValues().subList(i - smoothing, i + smoothing).stream().mapToDouble(d -> d)
					.average().getAsDouble());
		}

		// Calculate first derivative

		List<Double> firstDerivative = new ArrayList<>(Collections.nCopies(samples, Double.NaN));

		for (int i = horizon; i < samples - horizon; i++) {
			firstDerivative.set(i, (values.get(i + horizon) - values.get(i - horizon)) / (2 * horizon));
		}

		// Calculate direction change

		List<Boolean> validMask = new ArrayList<>(Collections.nCopies(samples, false));

		for (int i = horizon; i < samples - horizon; i++) {
			double firstValue = firstDerivative.get(i - horizon);
			double secondValue = firstDerivative.get(i + horizon);

			boolean firstValid = Math.abs(firstValue) < directionChangeThreshold;
			boolean secondValid = Math.abs(secondValue) < directionChangeThreshold;

			if (Double.isFinite(firstValue) && Double.isFinite(secondValue)) {
				if (firstValid && secondValid) {
					validMask.set(i, true);
				}
			}
		}

		// Finished calculating indicators

		List<Boolean> converged = new ArrayList<>(Collections.nCopies(samples, false));

		for (int i = 0; i < samples; i++) {
			boolean value = true;

			value &= Math.abs(firstDerivative.get(i)) < firstDerivativeThreshold;
			value &= validMask.get(i);

			converged.set(i, value);
		}

		writeOutput(signal.getName(), signal.getValues(), values, firstDerivative, validMask, converged);
		writeGraphs(signal.getName(), signal.getValues(), values, firstDerivative, validMask, converged,
				backlogIteration);

		return backlogIteration >= 0 && converged.get(backlogIteration) || iteration >= 500;
	}

	private void writeOutput(String name, List<Double> values, List<Double> smoothedValues,
			List<Double> firstDerivative, List<Boolean> validMask, List<Boolean> converged) {
		try {
			List<Double> firstDerivativeLowerThresholdSeries = new ArrayList<>(
					Collections.nCopies(values.size(), firstDerivativeThreshold));

			for (int i = 0; i < values.size(); i++) {
				if (!validMask.get(i)) {
					firstDerivativeLowerThresholdSeries.set(i, 0.0);
				}
			}

			File outputPath = new File(outputHierarchy.getOutputFilename("convergence_deriv_" + name + ".csv"));

			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

			writer.write(String.join(";", new String[] { "iteration", "value", "smoothed_value", "first_derivative",
					"valid_mask", "first_derivative_threshold", "converged", "backlog_converged" }) + "\n");

			List<Boolean> backlogConverged = new ArrayList<>(Collections.nCopies(values.size(), false));

			for (int i = backlogShift; i < values.size(); i++) {
				backlogConverged.set(i, converged.get(i - backlogShift));
			}

			for (int i = 0; i < values.size(); i++) {
				writer.write(String.join(";", new String[] { //
						String.valueOf(i), //
						String.valueOf(values.get(i)), //
						String.valueOf(smoothedValues.get(i)), //
						String.valueOf(firstDerivative.get(i)), //
						String.valueOf(validMask.get(i)), //
						String.valueOf(firstDerivativeLowerThresholdSeries.get(i)), //
						String.valueOf(converged.get(i)), //
						String.valueOf(backlogConverged.get(i)), //
				}) + "\n");
			}

			writer.close();

		} catch (IOException e) {
		}
	}

	private Map<Integer, Double> createSeries(List<Double> values) {
		Map<Integer, Double> series = new TreeMap<>();

		for (int i = 0; i < values.size(); i++) {
			series.put(i, values.get(i));
		}

		return series;
	}

	private void writeGraphs(String name, List<Double> values, List<Double> smoothedValues,
			List<Double> firstDerivative, List<Boolean> validMask, List<Boolean> converged, int backlogIteration) {
		// Prepare all series

		Map<Integer, Double> valueSeries = createSeries(values);
		Map<Integer, Double> smoothedValueSeries = createSeries(smoothedValues);
		Map<Integer, Double> firstDerivativeSeries = createSeries(firstDerivative);

		List<Double> firstDerivativeLowerThreshold = new ArrayList<>(
				Collections.nCopies(values.size(), -firstDerivativeThreshold));
		List<Double> firstDerivativeUpperThreshold = new ArrayList<>(
				Collections.nCopies(values.size(), firstDerivativeThreshold));

		for (int i = 0; i < values.size(); i++) {
			if (!validMask.get(i)) {
				firstDerivativeLowerThreshold.set(i, 0.0);
				firstDerivativeUpperThreshold.set(i, 0.0);
			}
		}

		Map<Integer, Double> constrainedFirstDerivativeLowerThresholdSeries = createSeries(
				firstDerivativeLowerThreshold);
		Map<Integer, Double> constrainedFirstDerivativeUpperThresholdSeries = createSeries(
				firstDerivativeUpperThreshold);

		// Prepare plots

		{
			File outputPath = new File(outputHierarchy.getOutputFilename("convergence_deriv_" + name + "_value.png"));
			XYLineChart chart = new XYLineChart("Convergence: " + name + " (Value)", "Iteration", "Value");
			chart.addSeries("Value", valueSeries);
			chart.addSeries("Smoothed", smoothedValueSeries);
			chart.saveAsPng(outputPath.toString(), 1280, 720);
		}

		{
			File outputPath = new File(outputHierarchy.getOutputFilename("convergence_deriv_" + name + "_first.png"));
			XYLineChart chart = new XYLineChart("Convergence: " + name + " (1st Derivative)", "Iteration", "Value");

			chart.addSeries("1st Derivative", firstDerivativeSeries);
			chart.addSeries("Lower bound", constrainedFirstDerivativeLowerThresholdSeries);
			chart.addSeries("Upper bound", constrainedFirstDerivativeUpperThresholdSeries);

			((XYPlot) chart.getChart().getPlot()).getRenderer().setSeriesPaint(1, Color.black);
			((XYPlot) chart.getChart().getPlot()).getRenderer().setSeriesPaint(2, Color.black);

			if (backlogIteration >= 0) {
				chart.addSeries("Indicator", new double[] { backlogIteration, backlogIteration },
						new double[] { -firstDerivativeThreshold, firstDerivativeThreshold });
				((XYPlot) chart.getChart().getPlot()).getRenderer().setSeriesPaint(3, Color.blue);
			}

			chart.saveAsPng(outputPath.toString(), 1280, 720);
		}
	}
}
