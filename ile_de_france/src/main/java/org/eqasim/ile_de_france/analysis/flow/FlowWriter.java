package org.eqasim.ile_de_france.analysis.flow;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;

public class FlowWriter {
	private final IdMap<Link, List<Integer>> counts;

	public FlowWriter(IdMap<Link, List<Integer>> counts) {
		this.counts = counts;
	}

	public void write(String path) throws IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter(path);

		writer.write(String.join(";", new String[] { //
				"link_id", "hour", "count" //
		}) + "\n");

		for (Map.Entry<Id<Link>, List<Integer>> entry : counts.entrySet()) {
			for (int hour = 0; hour < 24; hour++) {
				writer.write(String.join(";", new String[] { //
						entry.getKey().toString(), //
						String.valueOf(hour), //
						String.valueOf(entry.getValue().get(hour)) //
				}) + "\n");
			}
		}

		writer.close();
	}
}
