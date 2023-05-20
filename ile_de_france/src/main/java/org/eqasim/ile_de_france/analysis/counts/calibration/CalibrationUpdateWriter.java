package org.eqasim.ile_de_france.analysis.counts.calibration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.eqasim.ile_de_france.analysis.counts.calibration.CalibrationUpdate.LinkItem;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;

public class CalibrationUpdateWriter {
	private final OutputDirectoryHierarchy hierarchy;

	public CalibrationUpdateWriter(OutputDirectoryHierarchy hierarchy) {
		this.hierarchy = hierarchy;
	}

	public void write(CalibrationUpdate update) {
		String outputPath = hierarchy.getOutputFilename("eqasim_calibration.csv");

		try {
			boolean writeHeader = new File(outputPath).exists();
			BufferedWriter outputWriter = IOUtils.getAppendingBufferedWriter(outputPath);

			if (writeHeader) {
				outputWriter.write(String.join(";", Arrays.asList( //
						"iteration", //
						"correction", //
						"rmse", //
						"mae" //
				)) + "\n");
			}

			outputWriter.write(String.join(";", Arrays.asList( //
					String.valueOf(update.iteration), //
					String.valueOf(update.correctionFactor), //
					String.valueOf(update.rmse), //
					String.valueOf(update.mae) //
			)) + "\n");

			outputWriter.close();

			String iterationPath = hierarchy.getIterationFilename(update.iteration, "eqasim_calibration.csv");
			BufferedWriter iterationWriter = IOUtils.getAppendingBufferedWriter(iterationPath);

			iterationWriter.write(String.join(";", Arrays.asList( //
					"link_id", //
					"reference_count", //
					"scaled_count", //
					"corrected_count", //
					"initial_capacity", //
					"current_capacity", //
					"updated_capacity", //
					"updated_links" //
			)) + "\n");

			for (LinkItem link : update.links) {
				iterationWriter.write(String.join(";", Arrays.asList( //
						link.linkId.toString(), //
						String.valueOf(link.referenceCount), //
						String.valueOf(link.scaledCount), //
						String.valueOf(link.correctedCount), //
						String.valueOf(link.initialCapacity), //
						String.valueOf(link.currentCapacity), //
						String.valueOf(link.updatedCapacity), //
						String.valueOf(link.updatedLinks) //
				)) + "\n");
			}

			iterationWriter.close();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
