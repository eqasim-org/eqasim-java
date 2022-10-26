package org.eqasim.core.analysis.pt;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;

/**
 * Provides functionality to write transit stops usages into a CSV file
 */
public class PublicTransportStationUsageWriter {
	final private Collection<PublicTransportStationUsageItem> usages;
	final private String delimiter;

	public PublicTransportStationUsageWriter(Collection<PublicTransportStationUsageItem> usages) {
		this(usages, ";");
	}

	public PublicTransportStationUsageWriter(Collection<PublicTransportStationUsageItem> usages, String delimiter) {
		this.usages = usages;
		this.delimiter = delimiter;
	}

	public void write(String outputPath) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

		writer.write(formatHeader() + "\n");
		writer.flush();

		for (PublicTransportStationUsageItem usage : usages) {
			writer.write(formatUsage(usage) + "\n");
			writer.flush();
		}

		writer.flush();
		writer.close();
	}

	private String formatHeader() {
		return String.join(delimiter, new String[] { //
				"stop_id", //
				"stop_name", //
				"x", //
				"y", //
				"nb_accesses", //
				"nb_egresses",
		});
	}

	private String formatUsage(PublicTransportStationUsageItem usage) {
		return String.join(delimiter, new String[] { //
				usage.getTransitStopFacility().getId().toString(), //
				usage.getTransitStopFacility().getName(), //
				String.valueOf(usage.getTransitStopFacility().getCoord().getX()), //
				String.valueOf(usage.getTransitStopFacility().getCoord().getY()),
				String.valueOf(usage.getNbAccesses()), //
				String.valueOf(usage.getNbEgresses()), //
		});
	}
}
