package org.eqasim.ile_de_france.analysis.counts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;

public class CountsListener implements IterationStartsListener, IterationEndsListener, ShutdownListener {
	static public final String OUTPUT_FILE = "eqasim_counts.csv";

	private final CountsHandler handler;
	private final EventsManager eventsManager;
	private final EqasimConfigGroup eqasimConfig;
	private final OutputDirectoryHierarchy outputDirectoryHierarchy;
	private final Network network;

	private boolean isActive = false;

	public CountsListener(EqasimConfigGroup eqasimConfig, EventsManager eventsManager,
			OutputDirectoryHierarchy outputDirectoryHierarchy, Network network, Set<Id<Link>> linkIds) {
		this.eqasimConfig = eqasimConfig;
		this.eventsManager = eventsManager;
		this.network = network;
		this.outputDirectoryHierarchy = outputDirectoryHierarchy;

		for (Id<Link> linkId : linkIds) {
			if (!network.getLinks().containsKey(linkId)) {
				throw new IllegalStateException("Link for counting does not exist: " + linkId);
			}
		}

		this.handler = new CountsHandler(linkIds);
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		isActive = eqasimConfig.getTripAnalysisInterval() > 0
				&& (event.getIteration() % eqasimConfig.getTripAnalysisInterval() == 0 || event.isLastIteration());

		if (isActive) {
			eventsManager.addHandler(handler);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (isActive) {
			eventsManager.removeHandler(handler);

			try {
				File outputPath = new File(
						outputDirectoryHierarchy.getIterationFilename(event.getIteration(), OUTPUT_FILE));
				new CountsWriter(handler.getCounts(), network, 1.0 / eqasimConfig.getSampleSize()).write(outputPath);
			} catch (IOException e) {
			}
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		try {
			File iterationPath = new File(
					outputDirectoryHierarchy.getIterationFilename(event.getIteration(), OUTPUT_FILE));
			File outputPath = new File(outputDirectoryHierarchy.getOutputFilename(OUTPUT_FILE));
			Files.copy(iterationPath.toPath(), outputPath.toPath());
		} catch (IOException e) {
		}
	}
}