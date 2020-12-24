package org.eqasim.core.simulation.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eqasim.core.analysis.DistanceUnit;
import org.eqasim.core.analysis.TripListener;
import org.eqasim.core.analysis.TripWriter;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AnalysisOutputListener implements IterationStartsListener, IterationEndsListener, ShutdownListener {
	private static final String TRIPS_FILE_NAME = "trips.csv";

	private final OutputDirectoryHierarchy outputDirectory;
	private final int lastIteration;

	private final TripListener tripAnalysisListener;
	private final int tripAnalysisInterval;
	private boolean isTripAnalysisActive = false;

	private final DistanceUnit scenarioDistanceUnit;
	private final DistanceUnit analysisDistanceUnit;

	@Inject
	public AnalysisOutputListener(EqasimConfigGroup config, ControlerConfigGroup controllerConfig,
			OutputDirectoryHierarchy outputDirectory, TripListener tripListener) {
		this.outputDirectory = outputDirectory;
		this.lastIteration = controllerConfig.getLastIteration();

		this.scenarioDistanceUnit = config.getDistanceUnit();
		this.analysisDistanceUnit = config.getTripAnalysisDistanceUnit();

		this.tripAnalysisInterval = config.getTripAnalysisInterval();
		this.tripAnalysisListener = tripListener;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (tripAnalysisInterval > 0 && event.getIteration() % tripAnalysisInterval == 0) {
			isTripAnalysisActive = true;
			event.getServices().getEvents().addHandler(tripAnalysisListener);
		} else {
			isTripAnalysisActive = false;
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			if (isTripAnalysisActive) {
				event.getServices().getEvents().removeHandler(tripAnalysisListener);

				String path = outputDirectory.getIterationFilename(event.getIteration(), TRIPS_FILE_NAME);
				new TripWriter(tripAnalysisListener.getTripItems(), scenarioDistanceUnit, analysisDistanceUnit)
						.write(path);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		try {
			File iterationPath = new File(outputDirectory.getIterationFilename(lastIteration, TRIPS_FILE_NAME));
			File outputPath = new File(outputDirectory.getOutputFilename(TRIPS_FILE_NAME));
			Files.copy(iterationPath.toPath(), outputPath.toPath());
		} catch (IOException e) {
		}
	}
}
