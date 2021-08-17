package org.eqasim.core.simulation.convergence.criterion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eqasim.core.simulation.convergence.ConvergenceSignal;
import org.jfree.chart.plot.XYPlot;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.charts.XYLineChart;

public class AstraCriterion implements ConvergenceCriterion {
	private final OutputDirectoryHierarchy outputHierarchy;

	private final double meanThreshold; // T_mu
	private final double lagThreshold; // T_lambda

	private final int horizon; // H
	private final int lag; // L

	public AstraCriterion(OutputDirectoryHierarchy outputHierarchy, double meanThreshold, double lagThreshold,
			int meanHorizon, int lagHorizon) {
		this.horizon = meanHorizon;
		this.lag = lagHorizon;
		this.meanThreshold = meanThreshold;
		this.lagThreshold = lagThreshold;
		this.outputHierarchy = outputHierarchy;
	}

	@Override
	public boolean checkConvergence(int iteration, ConvergenceSignal signal) {
		double mean = Double.NaN;
		double laggedMean = Double.NaN;

		int size = signal.getValues().size();
		double value = signal.getValues().get(size - 1);

		if (size > horizon) {
			mean = 0.0;

			for (int i = size - horizon; i < size; i++) {
				mean += signal.getValues().get(i);
			}

			mean /= horizon;
		}

		if (size > horizon + lag) {
			laggedMean = 0.0;

			for (int i = size - horizon - lag; i < size - lag; i++) {
				laggedMean += signal.getValues().get(i);
			}

			laggedMean /= horizon;
		}

		double meanSlack = Math.max(Math.abs(value - mean) - meanThreshold, 0.0);
		double laggedSlack = Math.max(Math.abs(mean - laggedMean) - lagThreshold, 0.0);

		boolean meanConverged = Math.abs(value - mean) < meanThreshold;
		boolean lagConverged = Math.abs(mean - laggedMean) < lagThreshold;

		boolean converged = meanConverged && lagConverged;

		File outputPath = new File(outputHierarchy.getOutputFilename("convergence_astra_" + signal.getName() + ".csv"));

		try {
			boolean append = iteration > 0;

			BufferedWriter writer = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(outputPath, append)));

			if (iteration == 0) {
				writer.write(String.join(";",
						new String[] { "iteration", "value", "mean", "lagged_mean", "mean_slack", "lagged_slack",
								"mean_threshold", "lag_threshold", "mean_converged", "lag_converged", "converged" })
						+ "\n");
			}

			writer.write(String.join(";", new String[] { //
					String.valueOf(iteration), //
					String.valueOf(value), //
					String.valueOf(mean), //
					String.valueOf(laggedMean), //
					String.valueOf(meanSlack), //
					String.valueOf(laggedSlack), //
					String.valueOf(meanThreshold), //
					String.valueOf(lagThreshold), //
					String.valueOf(meanConverged), //
					String.valueOf(lagConverged), //
					String.valueOf(converged) //
			}) + "\n");

			writer.close();
		} catch (IOException e) {
		}

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(outputPath)));

			String line = null;

			List<Double> values = new LinkedList<>();
			List<Double> means = new LinkedList<>();
			List<Double> laggedMeans = new LinkedList<>();

			while ((line = reader.readLine()) != null) {
				if (!line.startsWith("iteration")) {
					String parts[] = line.split(";");
					values.add(Double.parseDouble(parts[1]));
					means.add(Double.parseDouble(parts[2]));
					laggedMeans.add(Double.parseDouble(parts[3]));
				}
			}

			reader.close();

			XYLineChart chart = new XYLineChart("Convergence: " + signal.getName(), "Iteration", "Value");

			Map<Integer, Double> valueSeries = new TreeMap<>();
			for (int i = 0; i < values.size(); i++) {
				valueSeries.put(i, values.get(i));
			}

			Map<Integer, Double> meanSeries = new TreeMap<>();
			for (int i = 0; i < means.size(); i++) {
				if (Double.isFinite(means.get(i))) {
					meanSeries.put(i, means.get(i));
				}
			}

			Map<Integer, Double> laggedMeanSeries = new TreeMap<>();
			for (int i = 0; i < laggedMeans.size(); i++) {
				if (Double.isFinite(laggedMeans.get(i))) {
					laggedMeanSeries.put(i, laggedMeans.get(i));
				}
			}

			chart.addSeries("Value", valueSeries);
			chart.addSeries("Mean", meanSeries);
			chart.addSeries("Lagged Mean", laggedMeanSeries);

			/*- double minimumValue = Double.POSITIVE_INFINITY;
			minimumValue = Math.min(minimumValue, values.stream().mapToDouble(d -> d).min().getAsDouble());
			minimumValue = Math.min(minimumValue, means.stream().mapToDouble(d -> d).min().getAsDouble());
			minimumValue = Math.min(minimumValue, laggedMeans.stream().mapToDouble(d -> d).min().getAsDouble());

			double maximumValue = Double.NEGATIVE_INFINITY;
			maximumValue = Math.max(maximumValue, values.stream().mapToDouble(d -> d).max().getAsDouble());
			maximumValue = Math.max(maximumValue, means.stream().mapToDouble(d -> d).max().getAsDouble());
			maximumValue = Math.max(maximumValue, laggedMeans.stream().mapToDouble(d -> d).max().getAsDouble());

			((XYPlot) chart.getChart().getPlot()).getRangeAxis().setLowerBound(minimumValue * 0.9);
			((XYPlot) chart.getChart().getPlot()).getRangeAxis().setUpperBound(maximumValue * 1.1);*/

			File imageOutputPath = new File(
					outputHierarchy.getOutputFilename("convergence_astra_" + signal.getName() + ".png"));
			chart.saveAsPng(imageOutputPath.toString(), 1280, 720);
		} catch (IOException e) {
		}

		return converged;
	}
}
