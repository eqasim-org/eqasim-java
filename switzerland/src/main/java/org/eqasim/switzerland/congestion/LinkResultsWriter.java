package org.eqasim.switzerland.congestion;

import org.eqasim.switzerland.congestion.LinkResultsListener.IterationResult;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;


public class LinkResultsWriter {
	private final List<Map<Id<Link>, IterationResult>> results;
	private final String delimiter;

	public LinkResultsWriter(List<Map<Id<Link>, IterationResult>> results) {
		this(results, ";");
	}

	public LinkResultsWriter(List<Map<Id<Link>, IterationResult>> results, String delimiter) {
		this.results = results;
		this.delimiter = delimiter;
	}

	public void write(String outputPath) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

		writer.write(formatHeader() + "\n");
		writer.flush();

		for (Map<Id<Link>, IterationResult> item : results) {
			for (IterationResult result : item.values()) {
				writer.write(formatTrip(result) + "\n");
				writer.flush();
			}
		}

		writer.flush();
		writer.close();
	}

	private String formatHeader() {
		return String.join(delimiter, new String[] {
				"iteration",
				"linkId",
				"binSize",
				"bin",
				"perLink",
				"meanTravelTime",
		});
	}

	private String formatTrip(IterationResult result) {

		String[] out = new String[result.numBins];

		for (int i=0; i<result.numBins; i++) {
			out[i] = String.join(delimiter, new String[] {
					String.valueOf(result.iteration),
					String.valueOf(result.linkId),
					String.valueOf(result.binSize),
					String.valueOf(i),
					String.valueOf(result.numberOfAgents[i]),
					String.valueOf(result.meanTravelTime[i]),
			});

		}
		return String.join("\n", out);
	}
}
