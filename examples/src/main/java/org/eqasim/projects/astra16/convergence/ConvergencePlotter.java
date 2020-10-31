package org.eqasim.projects.astra16.convergence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.charts.XYLineChart;

public class ConvergencePlotter implements IterationEndsListener {
	private final ConvergenceManager manager;
	private final OutputDirectoryHierarchy outputHierarchy;

	private final Map<Integer, Double> unfulfilled = new HashMap<>();

	public ConvergencePlotter(ConvergenceManager manager, OutputDirectoryHierarchy outputHierarchy) {
		this.manager = manager;
		this.outputHierarchy = outputHierarchy;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		List<String> slots = manager.getCriteria().values().stream().map(ConvergenceCriterion::getSlot)
				.collect(Collectors.toList());

		for (String slot : slots) {
			for (ConvergenceCriterion criterion : manager.getCriteria().values()) {
				if (criterion.getSlot().equals(slot)) {
					XYLineChart chart = new XYLineChart(String.format("Convergence values %s", slot), "Iteration",
							slot);

					Map<Integer, Double> values = new TreeMap<>();

					for (int i = 0; i < criterion.getValues().size(); i++) {
						if (Double.isFinite(criterion.getValues().get(i))) {
							values.put(i, criterion.getValues().get(i));
						}
					}

					chart.addSeries(slot, values);

					String outputPath = outputHierarchy
							.getOutputFilename(String.format("convergence_values_%s.png", slot));
					chart.saveAsPng(outputPath, 800, 600);

					break; // Otherwise we override the same slot!
				}
			}
		}

		for (Map.Entry<String, ConvergenceCriterion> entry : manager.getCriteria().entrySet()) {
			ConvergenceCriterion criterion = entry.getValue();
			String name = entry.getKey();

			XYLineChart chart = new XYLineChart(
					String.format("Convergence %s (Conv.: %s)", name, criterion.isConverged() ? "yes" : "no"),
					"Iteration", String.format("%s(%s)", criterion.getClass().getTypeName(), criterion.getSlot()));

			Map<Integer, Double> values = new TreeMap<>();
			Map<Integer, Double> threshold = new HashMap<>();

			for (int i = 0; i < criterion.getMetricValues().size(); i++) {
				if (Double.isFinite(criterion.getMetricValues().get(i))) {
					values.put(i, criterion.getMetricValues().get(i));
					threshold.put(i, criterion.getThreshold());
				}
			}

			chart.addSeries("Metric", values);
			chart.addSeries("Threshold", threshold);

			String outputPath = outputHierarchy.getOutputFilename(String.format("convergence_metric_%s.png", name));
			chart.saveAsPng(outputPath, 800, 600);
		}

		int unfulfilledCriteria = 0;

		for (ConvergenceCriterion criterion : manager.getCriteria().values()) {
			if (!criterion.isConverged()) {
				unfulfilledCriteria++;
			}
		}

		unfulfilled.put(event.getIteration(), (double) unfulfilledCriteria);

		XYLineChart chart = new XYLineChart(String.format("Convergence criteria"), "Iteration", "Unfulfilled criteria");
		chart.addSeries("Criteria", unfulfilled);

		String outputPath = outputHierarchy.getOutputFilename("convergence_criteria.png");
		chart.saveAsPng(outputPath, 800, 600);
	}
}
