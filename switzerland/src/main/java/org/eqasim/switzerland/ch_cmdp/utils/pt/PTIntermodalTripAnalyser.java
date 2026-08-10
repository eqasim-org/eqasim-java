package org.eqasim.switzerland.ch_cmdp.utils.pt;

import java.io.IOException;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import com.google.inject.Inject;

public class PTIntermodalTripAnalyser implements IterationStartsListener, IterationEndsListener {
	private static final String PT_INTERMODAL_TRIPS_FILE_NAME = "pt_intermodal_trips.csv.gz";

	private final PTIntermodalTripHandler handler;
	private boolean analysisActive = false;

	@Inject
	public PTIntermodalTripAnalyser(PTIntermodalTripHandler handler) {
		this.handler = handler;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (event.getIteration() == event.getServices().getConfig().controller().getLastIteration()) {
			@SuppressWarnings("deprecation")
			String outputPath = event.getServices().getControlerIO()
					.getOutputFilename(PT_INTERMODAL_TRIPS_FILE_NAME);

			try {
				handler.open(outputPath);
			} catch (IOException e) {
				throw new RuntimeException("Failed to open PT intermodal trip CSV: " + outputPath, e);
			}

			event.getServices().getEvents().addHandler(handler);
			analysisActive = true;
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (analysisActive) {
			analysisActive = false;
			event.getServices().getEvents().removeHandler(handler);

			try {
				handler.close();
				System.out.println("PT intermodal trips written to: "
						+ event.getServices().getControlerIO().getOutputFilename(PT_INTERMODAL_TRIPS_FILE_NAME));
			} catch (IOException e) {
				throw new RuntimeException("Failed to write PT intermodal trip CSV", e);
			}
		}
	}
}
