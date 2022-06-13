package org.eqasim.core.simulation.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eqasim.core.analysis.DistanceUnit;
import org.eqasim.core.analysis.legs.LegListener;
import org.eqasim.core.analysis.legs.LegWriter;
import org.eqasim.core.analysis.pt.PublicTransportLegListener;
import org.eqasim.core.analysis.pt.PublicTransportLegWriter;
import org.eqasim.core.analysis.trips.TripListener;
import org.eqasim.core.analysis.trips.TripWriter;
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
	private final LegListener legAnalysisListener;
	private final PublicTransportLegListener ptAnalysisListener;

	private final int analysisInterval;
	private boolean isAnalysisActive = false;

	private final DistanceUnit scenarioDistanceUnit;
	private final DistanceUnit analysisDistanceUnit;

	@Inject
	public AnalysisOutputListener(EqasimConfigGroup config, OutputDirectoryHierarchy outputDirectory,
			TripListener tripListener, LegListener legListener, PublicTransportLegListener ptListener) {
		this.outputDirectory = outputDirectory;

		this.scenarioDistanceUnit = config.getDistanceUnit();
		this.analysisDistanceUnit = config.getAnalysisDistanceUnit();

		this.analysisInterval = config.getAnalysisInterval();

		this.tripAnalysisListener = tripListener;
		this.legAnalysisListener = legListener;
		this.ptAnalysisListener = ptListener;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		isAnalysisActive = false;

		if (analysisInterval > 0) {
			if (event.getIteration() % analysisInterval == 0 || event.isLastIteration()) {
				isAnalysisActive = true;
				event.getServices().getEvents().addHandler(tripAnalysisListener);
				event.getServices().getEvents().addHandler(legAnalysisListener);
				event.getServices().getEvents().addHandler(ptAnalysisListener);
			}
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			if (isAnalysisActive) {
				event.getServices().getEvents().removeHandler(tripAnalysisListener);
				event.getServices().getEvents().removeHandler(legAnalysisListener);
				event.getServices().getEvents().removeHandler(ptAnalysisListener);

				new TripWriter(tripAnalysisListener.getTripItems(), scenarioDistanceUnit, analysisDistanceUnit)
						.write(outputDirectory.getIterationFilename(event.getIteration(), TRIPS_FILE_NAME));

				new LegWriter(legAnalysisListener.getLegItems(), scenarioDistanceUnit, analysisDistanceUnit)
						.write(outputDirectory.getIterationFilename(event.getIteration(), LEGS_FILE_NAME));

				new PublicTransportLegWriter(ptAnalysisListener.getTripItems())
						.write(outputDirectory.getIterationFilename(event.getIteration(), PT_FILE_NAME));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		try {
			Files.copy(new File(outputDirectory.getIterationFilename(event.getIteration(), TRIPS_FILE_NAME)).toPath(),
					new File(outputDirectory.getOutputFilename(TRIPS_FILE_NAME)).toPath());
			Files.copy(new File(outputDirectory.getIterationFilename(event.getIteration(), LEGS_FILE_NAME)).toPath(),
					new File(outputDirectory.getOutputFilename(LEGS_FILE_NAME)).toPath());
			Files.copy(new File(outputDirectory.getIterationFilename(event.getIteration(), PT_FILE_NAME)).toPath(),
					new File(outputDirectory.getOutputFilename(PT_FILE_NAME)).toPath());
		} catch (IOException e) {
		}
	}
}
