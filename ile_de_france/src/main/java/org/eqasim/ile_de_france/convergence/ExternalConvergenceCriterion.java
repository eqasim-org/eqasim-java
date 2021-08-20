package org.eqasim.ile_de_france.convergence;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.eqasim.ile_de_france.convergence.ConvergenceOutputFrame.ConvergenceSignal;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ExternalConvergenceCriterion implements TerminationCriterion, StartupListener {
	private final static Logger logger = Logger.getLogger(ExternalConvergenceCriterion.class);
	private final ObjectMapper mapper = new ObjectMapper();

	private final double interval = 0.25; // TODO: Make configurable
	private final File inputFile;
	private final File outputFile;

	private long sequence = 0;

	@Inject
	public ExternalConvergenceCriterion(OutputDirectoryHierarchy outputHierarchy) {
		inputFile = new File(outputHierarchy.getTempPath() + "/convergence_input.json");
		outputFile = new File(outputHierarchy.getTempPath() + "/convergence_output.json");
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		if (inputFile.exists()) {
			inputFile.delete();
		}

		if (outputFile.exists()) {
			outputFile.delete();
		}
	}

	@Override
	public boolean mayTerminateAfterIteration(int iteration) {
		return getNextResponse(ConvergenceSignal.mayTerminate);
	}

	@Override
	public boolean doTerminate(int iteration) {
		return getNextResponse(ConvergenceSignal.doTerminate);
	}

	private boolean getNextResponse(ConvergenceSignal signal) {
		ConvergenceOutputFrame outputFrame = new ConvergenceOutputFrame();
		outputFrame.sequence = sequence++;
		outputFrame.signal = signal;

		logger.info("Waiting for response on " + signal.toString() + " (seq " + outputFrame.sequence + ")");

		try {
			mapper.writeValue(outputFile, outputFrame);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		try {
			while (true) {
				Thread.sleep((long) (interval * 1000.0));

				if (inputFile.exists()) {
					try {
						ConvergenceInputFrame inputFrame = mapper.readValue(inputFile, ConvergenceInputFrame.class);

						if (inputFrame.sequence == outputFrame.sequence) {
							logger.info("Received response on " + signal.toString() + " (seq " + outputFrame.sequence
									+ ")");
							return inputFrame.response;
						}
					} catch (IOException e) {
					}
				}
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
