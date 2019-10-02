package org.eqasim.projects.dynamic_av.waiting_time;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;

import com.google.inject.Inject;

public class WaitingTimeAnalysisListener implements IterationStartsListener, ShutdownListener {
	private static final String WAITING_TIME_FILE_NAME = "waiting_time.csv";

	private final OutputDirectoryHierarchy outputDirectory;

	private final int lastIteration;
	private final int tripAnalysisInterval;

	private final WaitingTimeWriter writer;

	@Inject
	public WaitingTimeAnalysisListener(EqasimConfigGroup config, ControlerConfigGroup controllerConfig,
			OutputDirectoryHierarchy outputDirectory, WaitingTimeWriter writer) {
		this.outputDirectory = outputDirectory;
		this.lastIteration = controllerConfig.getLastIteration();
		this.tripAnalysisInterval = config.getTripAnalysisInterval();
		this.writer = writer;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (tripAnalysisInterval > 0 && event.getIteration() % tripAnalysisInterval == 0) {
			try {
				File iterationPath = new File(
						outputDirectory.getIterationFilename(event.getIteration(), WAITING_TIME_FILE_NAME));

				writer.write(iterationPath);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		try {
			File iterationPath = new File(outputDirectory.getIterationFilename(lastIteration, WAITING_TIME_FILE_NAME));
			File outputPath = new File(outputDirectory.getOutputFilename(WAITING_TIME_FILE_NAME));
			Files.copy(iterationPath.toPath(), outputPath.toPath());
		} catch (IOException e) {
		}
	}
}
