package org.eqasim.core.simulation.calibration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;

import org.apache.log4j.Logger;
import org.eqasim.core.analysis.TripListener;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CalibrationOutputListener implements IterationEndsListener, ShutdownListener {
	private final Logger logger = Logger.getLogger(CalibrationOutputListener.class);

	private static final String OUTPUT_FILE_NAME_JSON = "calibration.json";
	private static final String OUTPUT_FILE_NAME_HTML = "calibration.html";

	private final CalibrationConfigGroup config;

	private final OutputDirectoryHierarchy outputDirectory;
	private final int lastIteration;

	private final TripListener tripAnalysisListener;
	private final int tripAnalysisInterval;

	private CalibrationData referenceData;

	@Inject
	public CalibrationOutputListener(EqasimConfigGroup eqasimConfig, CalibrationConfigGroup config,
			ControlerConfigGroup controllerConfig, OutputDirectoryHierarchy outputDirectory,
			TripListener tripListener) {
		this.outputDirectory = outputDirectory;
		this.lastIteration = controllerConfig.getLastIteration();

		this.tripAnalysisInterval = eqasimConfig.getTripAnalysisInterval();
		this.tripAnalysisListener = tripListener;

		this.config = config;

		if (eqasimConfig.getTripAnalysisInterval() < 1) {
			logger.warn("To use calibration output, the tripAnalysisInterval must be > 0");
		}
	}

	private CalibrationData getReference() {
		try {
			if (referenceData == null) {
				File inputPath = new File(config.getReferencePath());
				referenceData = new ObjectMapper().readValue(inputPath, CalibrationData.class);
			}

			return referenceData;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String readTemplate() {
		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(CalibrationOutputListener.class.getResourceAsStream("calibration.html")));

			StringBuilder template = new StringBuilder();
			String line = null;

			while ((line = reader.readLine()) != null) {
				template.append(line);
				template.append("\n");
			}

			return template.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (config.getEnable()) {
			try {
				if (tripAnalysisInterval > 0 && event.getIteration() % tripAnalysisInterval == 0) {
					CalibrationData data = new CalibrationDataProvider(tripAnalysisListener, getReference()).getData();

					CalibrationOutput output = new CalibrationOutput();
					output.reference = getReference();
					output.simulation = data;
					output.hint = config.getHint();

					// Write JSON
					File outputFile = new File(
							outputDirectory.getIterationFilename(event.getIteration(), OUTPUT_FILE_NAME_JSON));
					new ObjectMapper().writeValue(outputFile, output);

					// Write HTML
					String template = readTemplate();
					String replacement = "var data = " + new ObjectMapper().writeValueAsString(output) + ";";
					template = template.replace("<!-- INSERT DATA -->", replacement);

					outputFile = new File(
							outputDirectory.getIterationFilename(event.getIteration(), OUTPUT_FILE_NAME_HTML));
					BufferedWriter writer = new BufferedWriter(
							new OutputStreamWriter(new FileOutputStream(outputFile)));
					writer.write(template);
					writer.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		if (config.getEnable()) {
			try {
				File iterationPath = new File(
						outputDirectory.getIterationFilename(lastIteration, OUTPUT_FILE_NAME_JSON));
				File outputPath = new File(outputDirectory.getOutputFilename(OUTPUT_FILE_NAME_JSON));
				Files.copy(iterationPath.toPath(), outputPath.toPath());

				iterationPath = new File(outputDirectory.getIterationFilename(lastIteration, OUTPUT_FILE_NAME_HTML));
				outputPath = new File(outputDirectory.getOutputFilename(OUTPUT_FILE_NAME_HTML));
				Files.copy(iterationPath.toPath(), outputPath.toPath());
			} catch (IOException e) {
			}
		}
	}
}
