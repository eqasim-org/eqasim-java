package org.eqasim.ile_de_france.analysis.flow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.analysis.AnalysisOutputListener;
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
	private final int lastIteration;

	private FlowHandler flowHandler;
	private final int flowAnalysisInterval;

	private final IdSet<Link> linkIds;

	@Inject
	public FlowListener(EqasimConfigGroup config, ControlerConfigGroup controllerConfig,
			OutputDirectoryHierarchy outputDirectory, IdSet<Link> linkIds) {
		this.outputDirectory = outputDirectory;
		this.lastIteration = controllerConfig.getLastIteration();

		this.flowAnalysisInterval = config.getTripAnalysisInterval();
		this.linkIds = linkIds;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (flowAnalysisInterval > 0 && (event.getIteration() % flowAnalysisInterval == 0
				|| event.getIteration() >= AnalysisOutputListener.convergenceIteration)) {
			flowHandler = new FlowHandler(linkIds);
			event.getServices().getEvents().addHandler(flowHandler);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			if (flowHandler != null) {
				event.getServices().getEvents().removeHandler(flowHandler);

				String path = outputDirectory.getIterationFilename(event.getIteration(), FLOWS_FILE_NAME);
				new FlowWriter(flowHandler.getCounts()).write(path);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		try {
			File iterationPath = new File(
					outputDirectory.getIterationFilename(AnalysisOutputListener.convergenceIteration, FLOWS_FILE_NAME));
			File outputPath = new File(outputDirectory.getOutputFilename(FLOWS_FILE_NAME));
			Files.copy(iterationPath.toPath(), outputPath.toPath());
		} catch (IOException e) {
		}
	}
}
