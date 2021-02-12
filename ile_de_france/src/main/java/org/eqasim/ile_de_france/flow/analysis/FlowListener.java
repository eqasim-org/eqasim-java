package org.eqasim.ile_de_france.flow.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.analysis.AnalysisOutputListener;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;

import com.google.inject.Inject;

public class FlowListener implements IterationStartsListener, IterationEndsListener, ShutdownListener {
	private static final String FLOWS_FILE_NAME = "flows.csv";

	private final OutputDirectoryHierarchy outputDirectory;

	private FlowHandler flowHandler;
	private final int flowAnalysisInterval;

	private final IdSet<Link> linkIds;

	private final List<IdMap<Link, List<Integer>>> history = new LinkedList<>();
	private final int historySize = 10;

	@Inject
	public FlowListener(EqasimConfigGroup config, ControlerConfigGroup controllerConfig,
			OutputDirectoryHierarchy outputDirectory, IdSet<Link> linkIds) {
		this.outputDirectory = outputDirectory;

		this.flowAnalysisInterval = config.getTripAnalysisInterval();
		this.linkIds = linkIds;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		flowHandler = new FlowHandler(linkIds);
		event.getServices().getEvents().addHandler(flowHandler);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		event.getServices().getEvents().removeHandler(flowHandler);

		history.add(flowHandler.getCounts());

		if (history.size() > historySize) {
			history.remove(0);
		}

		if (flowAnalysisInterval > 0 && (event.getIteration() % flowAnalysisInterval == 0
				|| event.getIteration() >= AnalysisOutputListener.convergenceIteration)) {
			if (history.size() > 0) {
				try {
					IdMap<Link, List<Double>> aggregatedCounts = new IdMap<>(Link.class);
					double scale = (double) history.size();

					for (Id<Link> linkId : linkIds) {
						aggregatedCounts.put(linkId, new ArrayList<>(Collections.nCopies(24, 0.0)));
					}

					for (IdMap<Link, List<Integer>> counts : history) {
						for (Map.Entry<Id<Link>, List<Integer>> entry : counts.entrySet()) {
							List<Integer> countValues = entry.getValue();
							List<Double> aggregatedValues = aggregatedCounts.get(entry.getKey());

							for (int h = 0; h < 24; h++) {
								aggregatedValues.set(h, aggregatedValues.get(h) + (double) countValues.get(h) / scale);
							}
						}
					}

					String path = outputDirectory.getIterationFilename(event.getIteration(), FLOWS_FILE_NAME);
					new FlowWriter(aggregatedCounts).write(path);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		try {
			int iteration = event.getServices().getIterationNumber();
			File iterationPath = new File(outputDirectory.getIterationFilename(iteration, FLOWS_FILE_NAME));
			File outputPath = new File(outputDirectory.getOutputFilename(FLOWS_FILE_NAME));
			Files.copy(iterationPath.toPath(), outputPath.toPath());
		} catch (IOException e) {
		}
	}
}
