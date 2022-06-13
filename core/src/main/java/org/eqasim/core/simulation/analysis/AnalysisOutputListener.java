package org.eqasim.core.simulation.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eqasim.core.analysis.DistanceUnit;
import org.eqasim.core.analysis.LegListener;
import org.eqasim.core.analysis.LegWriter;
import org.eqasim.core.analysis.TripListener;
import org.eqasim.core.analysis.TripWriter;
import org.eqasim.core.analysis.pt.PublicTransportTripListener;
import org.eqasim.core.analysis.pt.PublicTransportTripWriter;
import org.eqasim.core.components.config.EqasimConfigGroup;
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
	private static final String TRIPS_FILE_NAME = "eqasim_trips.csv";
	private static final String LEGS_FILE_NAME = "eqasim_legs.csv";
	private static final String PT_FILE_NAME = "eqasim_pt.csv";

	private final OutputDirectoryHierarchy outputDirectory;

	private final TripListener tripAnalysisListener;
	private final int tripAnalysisInterval;
	private boolean isTripAnalysisActive = false;

	private final LegListener legAnalysisListener;
	private final int legAnalysisInterval;
	private boolean isLegAnalysisActive = false;

	private final PublicTransportTripListener ptAnalysisListener;
	private final int ptAnalysisInterval;
	private boolean isPtAnalysisActive = false;

	private final DistanceUnit scenarioDistanceUnit;
	private final DistanceUnit analysisDistanceUnit;

	@Inject
	public AnalysisOutputListener(EqasimConfigGroup config, OutputDirectoryHierarchy outputDirectory,
			TripListener tripListener, LegListener legListener, PublicTransportTripListener ptListener) {
		this.outputDirectory = outputDirectory;

		this.scenarioDistanceUnit = config.getDistanceUnit();
		this.analysisDistanceUnit = config.getTripAnalysisDistanceUnit();

		this.tripAnalysisInterval = config.getTripAnalysisInterval();
		this.tripAnalysisListener = tripListener;

		this.legAnalysisInterval = config.getLegAnalysisInterval();
		this.legAnalysisListener = legListener;

		this.ptAnalysisInterval = config.getPublicTransportAnalysisInterval();
		this.ptAnalysisListener = ptListener;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		isTripAnalysisActive = false;

		if (tripAnalysisInterval > 0) {
			if (event.getIteration() % tripAnalysisInterval == 0 || event.isLastIteration()) {
				isTripAnalysisActive = true;
				event.getServices().getEvents().addHandler(tripAnalysisListener);
			}
		}

		isLegAnalysisActive = false;

		if (legAnalysisInterval > 0) {
			if (event.getIteration() % legAnalysisInterval == 0 || event.isLastIteration()) {
				isLegAnalysisActive = true;
				event.getServices().getEvents().addHandler(legAnalysisListener);
			}
		}

		isPtAnalysisActive = false;

		if (ptAnalysisInterval > 0) {
			if (event.getIteration() % ptAnalysisInterval == 0 || event.isLastIteration()) {
				isPtAnalysisActive = true;
				event.getServices().getEvents().addHandler(ptAnalysisListener);
			}
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

			if (isLegAnalysisActive) {
				event.getServices().getEvents().removeHandler(legAnalysisListener);

				String path = outputDirectory.getIterationFilename(event.getIteration(), LEGS_FILE_NAME);
				new LegWriter(legAnalysisListener.getLegItems(), scenarioDistanceUnit, analysisDistanceUnit)
						.write(path);
			}

			if (isPtAnalysisActive) {
				event.getServices().getEvents().removeHandler(ptAnalysisListener);

				String path = outputDirectory.getIterationFilename(event.getIteration(), PT_FILE_NAME);
				new PublicTransportTripWriter(ptAnalysisListener.getTripItems()).write(path);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		try {
			File iterationPath = new File(outputDirectory.getIterationFilename(event.getIteration(), TRIPS_FILE_NAME));
			File outputPath = new File(outputDirectory.getOutputFilename(TRIPS_FILE_NAME));
			Files.copy(iterationPath.toPath(), outputPath.toPath());
		} catch (IOException e) {
		}

		try {
			File iterationPath = new File(outputDirectory.getIterationFilename(event.getIteration(), LEGS_FILE_NAME));
			File outputPath = new File(outputDirectory.getOutputFilename(LEGS_FILE_NAME));
			Files.copy(iterationPath.toPath(), outputPath.toPath());
		} catch (IOException e) {
		}

		try {
			File iterationPath = new File(outputDirectory.getIterationFilename(event.getIteration(), PT_FILE_NAME));
			File outputPath = new File(outputDirectory.getOutputFilename(PT_FILE_NAME));
			Files.copy(iterationPath.toPath(), outputPath.toPath());
		} catch (IOException e) {
		}
	}
}
