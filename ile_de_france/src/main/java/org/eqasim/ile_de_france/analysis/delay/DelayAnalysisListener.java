package org.eqasim.ile_de_france.analysis.delay;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.io.IOUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class DelayAnalysisListener implements IterationStartsListener, IterationEndsListener, ShutdownListener {
	static public final String OUTPUT_NAME = "delay_analysis.csv";

	private final EventsManager eventsManager;
	private final OutputDirectoryHierarchy outputHierarchy;

	private DelayAnalysisHandler handler;
	private BufferedWriter writer;

	private final int analysisInterval;

	private final Population population;
	private final PersonAnalysisFilter personFilter;

	@Inject
	public DelayAnalysisListener(EventsManager eventsManager, OutputDirectoryHierarchy outputHierarchy,
			EqasimConfigGroup eqasimConfig, Population population, PersonAnalysisFilter personFilter) {
		this.outputHierarchy = outputHierarchy;
		this.eventsManager = eventsManager;
		this.analysisInterval = eqasimConfig.getAnalysisInterval();
		this.population = population;
		this.personFilter = personFilter;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		boolean writeAnalysisAtAll = analysisInterval > 0;

		if (writeAnalysisAtAll) {
			if (event.getIteration() % analysisInterval == 0 || event.isLastIteration()) {
				writer = IOUtils
						.getBufferedWriter(outputHierarchy.getIterationFilename(event.getIteration(), OUTPUT_NAME));
				handler = new DelayAnalysisHandler(population, personFilter, writer);
				eventsManager.addHandler(handler);
			}
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (handler != null) {
			handler.finish();
			eventsManager.removeHandler(handler);

			try {
				writer.flush();
				writer.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			handler = null;
			writer = null;
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		try {
			File iterationPath = new File(outputHierarchy.getIterationFilename(event.getIteration(), OUTPUT_NAME));
			File outputPath = new File(outputHierarchy.getOutputFilename(OUTPUT_NAME));
			Files.copy(iterationPath.toPath(), outputPath.toPath());
		} catch (IOException e) {
		}
	}
}
