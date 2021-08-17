package org.eqasim.core.simulation.convergence.criterion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.exception.ConvergenceException;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;
import org.eqasim.core.simulation.convergence.ConvergenceSignal;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.charts.XYLineChart;

public class FitCriterion implements ConvergenceCriterion {
	private final OutputDirectoryHierarchy outputHierarchy;

	private final double absoluteThreshold;
	private final double relativeThreshold;

	public FitCriterion(OutputDirectoryHierarchy outputHierarchy, double absoluteThreshold, double relativeThreshold) {
		this.outputHierarchy = outputHierarchy;
		this.relativeThreshold = relativeThreshold;
		this.absoluteThreshold = absoluteThreshold;
	}

	private Pair<RealVector, RealMatrix> evaluateModel(int start, int end, RealVector point) {
		RealVector functionValues = new ArrayRealVector(end - start);

		for (int i = 0; i < end - start; i++) {
			double value = 0.0;
			int iteration = i + start;

			value += point.getEntry(0);
			value += point.getEntry(1) * Math.exp(point.getEntry(2) * iteration);
			value += point.getEntry(3) * Math.exp(point.getEntry(4) * iteration) * iteration;

			functionValues.setEntry(i, value);
		}

		RealMatrix derivativeValues = new Array2DRowRealMatrix(end - start, 5);

		for (int i = 0; i < end - start; i++) {
			double value = Double.NaN;
			int iteration = i + start;

			value = 1.0;
			derivativeValues.setEntry(i, 0, value);

			value = Math.exp(point.getEntry(2) * iteration);
			derivativeValues.setEntry(i, 1, value);

			value = point.getEntry(1) * Math.exp(point.getEntry(2) * iteration) * iteration;
			derivativeValues.setEntry(i, 2, value);

			value = Math.exp(point.getEntry(4) * iteration) * iteration;
			derivativeValues.setEntry(i, 3, value);

			value = Math.exp(point.getEntry(3) * Math.exp(point.getEntry(4) * iteration)) * iteration * iteration;
			derivativeValues.setEntry(i, 4, value);
		}

		return Pair.create(functionValues, derivativeValues);
	}

	@Override
	public boolean checkConvergence(int iteration, ConvergenceSignal signal) {
		Optimum result = null;

		if (iteration >= 5) {
			double start[] = new double[] { -1e-3, -1e-3, -1e-3, 1e-3, 1e-3 };

			RealVector target = new ArrayRealVector(signal.getValues().size());

			for (int i = 0; i < signal.getValues().size(); i++) {
				target.setEntry(i, signal.getValues().get(i));
			}

			LeastSquaresProblem problem = new LeastSquaresBuilder() //
					.model(p -> evaluateModel(0, signal.getValues().size(), p)) //
					.start(start) //
					.target(target) //
					.maxEvaluations(100000) //
					.maxIterations(100000) //
					.lazyEvaluation(false) //
					.build();

			LeastSquaresOptimizer optimizer = new LevenbergMarquardtOptimizer()
					.withInitialStepBoundFactor(0.1);

			try {
				result = optimizer.optimize(problem);

				System.err.println( //
						result.getPoint().getEntry(0) + " " + //
								result.getPoint().getEntry(1) + " " + //
								result.getPoint().getEntry(2) + " " + //
								result.getPoint().getEntry(3) + " " + //
								result.getPoint().getEntry(4) + " " //
				);
			} catch (ConvergenceException e) {
				System.err.println("No convergence");
			}
		}

		double expectedFinalValue = Double.NaN;

		int absoluteIteration = -1;
		int relativeIteration = -1;

		if (result != null && result.getPoint().getEntry(2) <= 0.0 && result.getPoint().getEntry(4) < 0.0) {
			// Fit makes sense at this point.

			expectedFinalValue = result.getPoint().getEntry(0);
			int currentIteration = iteration;

			while (absoluteIteration == -1 || relativeIteration == -1) {
				double modelValue = evaluateModel(currentIteration, currentIteration + 1, result.getPoint()).getFirst()
						.getEntry(0);

				if (absoluteIteration == -1) {
					double absoluteError = Math.abs(modelValue - expectedFinalValue);

					if (absoluteError < absoluteThreshold) {
						absoluteIteration = currentIteration;
					}
				}

				if (relativeIteration == -1) {
					double relativeError = Math.abs(modelValue / expectedFinalValue - 1.0);

					if (relativeError < relativeThreshold) {
						relativeIteration = currentIteration;
					}
				}

				currentIteration++;
			}
		} else {
			absoluteIteration = iteration + 1;
			relativeIteration = iteration + 1;
		}

		// General convergence output

		double currentValue = signal.getValues().get(signal.getValues().size() - 1);
		double absoluteGap = Math.abs(currentValue - expectedFinalValue);
		double relativeGap = Math.abs(currentValue / expectedFinalValue - 1.0);

		boolean converged = absoluteGap < absoluteThreshold && relativeGap < relativeThreshold;

		try {
			File outputPath = new File(
					outputHierarchy.getOutputFilename("convergence_fit_" + signal.getName() + ".csv"));
			boolean append = iteration > 0;

			BufferedWriter writer = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(outputPath, append)));

			if (iteration == 0) {
				writer.write(String.join(";",
						new String[] { "iteration", "absolute_gap", "relative_gap", "absolute_threshold",
								"relative_threshold", "absolute_iteration", "relative_iteration", "converged", "C0",
								"C1", "C2", "C3", "C4" })
						+ "\n");
			}

			writer.write(String.join(";", new String[] { //
					String.valueOf(iteration), //
					String.valueOf(absoluteGap), //
					String.valueOf(relativeGap), //
					String.valueOf(absoluteThreshold), //
					String.valueOf(relativeThreshold), //
					String.valueOf(absoluteIteration), //
					String.valueOf(relativeIteration), //
					String.valueOf(converged), //
					String.valueOf(result == null ? Double.NaN : result.getPoint().getEntry(0)), //
					String.valueOf(result == null ? Double.NaN : result.getPoint().getEntry(1)), //
					String.valueOf(result == null ? Double.NaN : result.getPoint().getEntry(2)), //
					String.valueOf(result == null ? Double.NaN : result.getPoint().getEntry(3)), //
					String.valueOf(result == null ? Double.NaN : result.getPoint().getEntry(4)), //
			}) + "\n");

			writer.close();
		} catch (IOException e) {
		}

		{
			XYLineChart chart = new XYLineChart("Convergence: " + signal.getName(), "Iteration", "Value");

			Map<Integer, Double> dataSeries = new TreeMap<>();
			for (int i = 0; i < signal.getValues().size(); i++) {
				dataSeries.put(i, signal.getValues().get(i));
			}

			chart.addSeries("Data", dataSeries);

			int plotIteration = (int) Math.ceil(iteration * 1.3);
			plotIteration = Math.max(plotIteration, Math.max(absoluteIteration, relativeIteration));

			/*- double minimumValue = Double.POSITIVE_INFINITY;
			minimumValue = Math.min(minimumValue, signal.getValues().stream().mapToDouble(d -> d).min().getAsDouble());
			
			double maximumValue = Double.NEGATIVE_INFINITY;
			maximumValue = Math.max(maximumValue, signal.getValues().stream().mapToDouble(d -> d).max().getAsDouble());*/

			if (result != null) {
				Map<Integer, Double> modelSeries = new TreeMap<>();
				RealVector modelValues = evaluateModel(0, plotIteration + 1, result.getPoint()).getFirst();

				for (int i = 0; i < plotIteration; i++) {
					modelSeries.put(i, modelValues.getEntry(i));
					// minimumValue = Math.min(minimumValue, modelValues.getEntry(i));
					// maximumValue = Math.max(maximumValue, modelValues.getEntry(i));
				}

				chart.addSeries("Model", modelSeries);
			}

			// ((XYPlot)
			// chart.getChart().getPlot()).getRangeAxis().setLowerBound(minimumValue * 0.9);
			// ((XYPlot)
			// chart.getChart().getPlot()).getRangeAxis().setUpperBound(maximumValue * 1.1);

			File iterationOutputPath = new File(
					outputHierarchy.getIterationFilename(iteration, "convergence_fit_" + signal.getName() + ".png"));
			chart.saveAsPng(iterationOutputPath.toString(), 1280, 720);

			File generalOutputPath = new File(
					outputHierarchy.getOutputFilename("convergence_fit_" + signal.getName() + ".png"));
			chart.saveAsPng(generalOutputPath.toString(), 1280, 720);
		}

		return converged;
	}
}
