package org.eqasim.projects.astra16.convergence;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

public class ConvergenceWriter implements IterationEndsListener {
	private final ConvergenceManager manager;
	private final OutputDirectoryHierarchy outputHierarchy;

	public ConvergenceWriter(ConvergenceManager manager, OutputDirectoryHierarchy outputHierarchy) {
		this.manager = manager;
		this.outputHierarchy = outputHierarchy;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(outputHierarchy.getOutputFilename("convergence.csv"));

			List<String> header = Arrays.asList("slot", "name", "iteration", "value", "metric", "threshold",
					"converged", "class");
			writer.write(String.join(",", header) + "\n");

			for (Map.Entry<String, ConvergenceCriterion> entry : manager.getCriteria().entrySet()) {
				ConvergenceCriterion criterion = entry.getValue();
				String name = entry.getKey();

				for (int i = 0; i < criterion.getValues().size(); i++) {
					List<String> row = Arrays.asList( //
							criterion.getSlot(), //
							name, //
							String.valueOf(i), //
							String.valueOf(criterion.getValues().get(i)), //
							String.valueOf(criterion.getMetricValues().get(i)), //
							String.valueOf(criterion.getThreshold()), //
							String.valueOf(criterion.isConverged()), //
							criterion.getClass().getTypeName() //
					);

					writer.write(String.join(",", row) + "\n");
				}
			}

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
