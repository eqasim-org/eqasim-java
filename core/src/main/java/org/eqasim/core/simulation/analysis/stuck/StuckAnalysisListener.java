package org.eqasim.core.simulation.analysis.stuck;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StuckAnalysisListener implements StartupListener, IterationStartsListener, IterationEndsListener {
	static public final String OUTPUT_NAME = "stuck_analysis.csv";

	private final EventsManager eventsManager;
	private final OutputDirectoryHierarchy outputHierarchy;

	private final StuckAnalysisHandler handler = new StuckAnalysisHandler();

	@Inject
	public StuckAnalysisListener(EventsManager eventsManager, OutputDirectoryHierarchy outputHierarchy) {
		this.outputHierarchy = outputHierarchy;
		this.eventsManager = eventsManager;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		eventsManager.addHandler(handler);
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(
					IOUtils.getFileUrl(outputHierarchy.getOutputFilename(OUTPUT_NAME)), IOUtils.CHARSET_UTF8, false);
			writer.write(String.join(";", new String[] { "iteration", "count" }) + "\n");
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		eventsManager.removeHandler(handler);

		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(
					IOUtils.getFileUrl(outputHierarchy.getOutputFilename(OUTPUT_NAME)), IOUtils.CHARSET_UTF8, true);
			writer.write(String.join(";",
					new String[] { String.valueOf(event.getIteration()), String.valueOf(handler.getCount()) }) + "\n");
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}