package org.eqasim.ile_de_france.analysis.flow;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;

public class FlowReader {
	public IdSet<Link> read(String path) throws IOException {
		IdSet<Link> linkIds = new IdSet<>(Link.class);
		BufferedReader reader = IOUtils.getBufferedReader(path);

		String line = null;
		List<String> header = null;

		while ((line = reader.readLine()) != null) {
			List<String> row = Arrays.asList(line.split(";"));

			if (header == null) {
				header = row;
			} else {
				int index = header.indexOf("link_id");
				linkIds.add(Id.createLinkId(row.get(index)));
			}
		}

		reader.close();
		return linkIds;
	}
}
