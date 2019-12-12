package org.eqasim.switzerland.congestion;

import org.eqasim.switzerland.congestion.WorstDelayListener.WorstDelays;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;


public class WorstDelayWriter {
	private final List<WorstDelays> results;
	private final String delimiter;

	public WorstDelayWriter(List<WorstDelays> results) {
		this(results, ";");
	}

	public WorstDelayWriter(List<WorstDelays> results, String delimiter) {
		this.results = results;
		this.delimiter = delimiter;
	}

	public void write(String outputPath) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

		writer.write(formatHeader() + "\n");
		writer.flush();

		for (WorstDelays result : results) {
			writer.write(formatResult(result) + "\n");
			writer.flush();
		}

		writer.flush();
		writer.close();
	}

	private String formatHeader() {
		return String.join(delimiter, new String[] {
				"iteration",
				"linkId",
				"worstDelay",
		});
	}

	private String formatResult(WorstDelays result) {

		String[] out = new String[result.perLink.size()];

		int i = 0;

		for (Id<Link> linkId : result.perLink.keySet()) {
			out[i] = String.join(delimiter, new String[] {
					String.valueOf(result.iteration),
					String.valueOf(linkId),
					String.valueOf(result.perLink.get(linkId)),
			});
			i++;
		}
		return String.join("\n", out);
	}
}
