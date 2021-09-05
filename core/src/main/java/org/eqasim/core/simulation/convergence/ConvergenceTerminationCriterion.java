package org.eqasim.core.simulation.convergence;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.eqasim.core.simulation.convergence.criterion.ConvergenceCriterion;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.collections.Tuple;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ConvergenceTerminationCriterion implements TerminationCriterion {
	private final OutputDirectoryHierarchy outputHierarchy;

	private final Collection<Tuple<ConvergenceSignal, ConvergenceCriterion>> criteria = new LinkedList<>();
	private final Map<Integer, Double> history = new TreeMap<>();

	private final int firstIteration;
	private final int lastIteration;

	@Inject
	public ConvergenceTerminationCriterion(OutputDirectoryHierarchy outputHierarchy,
			ControlerConfigGroup controllerConfig) {
		this.outputHierarchy = outputHierarchy;
		this.firstIteration = controllerConfig.getFirstIteration();
		this.lastIteration = controllerConfig.getLastIteration();
	}

	public void addCriterion(ConvergenceSignal signal, ConvergenceCriterion criterion) {
		criteria.add(new Tuple<>(signal, criterion));
	}

	@Override
	public boolean mayTerminateAfterIteration(int iteration) {
		if (iteration >= lastIteration) {
			return true;
		}

		if (iteration > firstIteration) {
			int active = 0;

			for (Tuple<ConvergenceSignal, ConvergenceCriterion> pair : criteria) {
				if (!pair.getSecond().checkConvergence(iteration, pair.getFirst())) {
					active += 1;
				}
			}

			history.put(iteration, (double) active);

			writeOutput();
			createGraph();

			return active == 0;
		} else {
			return false;
		}
	}

	@Override
	public boolean doTerminate(int iteration) {
		return true;
	}

	private void writeOutput() {
		try {
			File outputPath = new File(outputHierarchy.getOutputFilename("convergence.csv"));

			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

			writer.write(String.join(";", new String[] { "iteration", "active" }) + "\n");

			for (Map.Entry<Integer, Double> entry : history.entrySet()) {
				writer.write(String.join(";", new String[] { //
						String.valueOf(entry.getKey()), //
						String.valueOf(entry.getValue()), }) + "\n");
			}

			writer.close();

		} catch (IOException e) {
		}
	}

	private void createGraph() {
		File outputPath = new File(outputHierarchy.getOutputFilename("convergence.png"));
		XYLineChart chart = new XYLineChart("Convergence", "Iteration", "Active criteria");
		chart.addSeries("Active criteria", history);
		chart.saveAsPng(outputPath.toString(), 1280, 720);
	}
}
