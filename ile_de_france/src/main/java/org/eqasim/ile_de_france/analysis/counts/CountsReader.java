package org.eqasim.ile_de_france.analysis.counts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;

import com.google.common.base.Verify;

public class CountsReader {
	public DailyCounts read(File path) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
		IdMap<Link, Double> counts = new IdMap<>(Link.class);

		String line = null;
		List<String> header = null;

		while ((line = reader.readLine()) != null) {
			List<String> row = Arrays.asList(line.trim().split(";"));

			if (header == null) {
				header = row;

				Verify.verify(header.contains("link_id"), "Missing link_id in counts file");
				Verify.verify(header.contains("count"), "Missing count in in counts file");
			} else {
				Id<Link> linkId = Id.createLinkId(row.get(header.indexOf("link_id")));
				double count = Double.parseDouble(row.get(header.indexOf("count")));

				counts.compute(linkId, (id, v) -> {
					return v == null ? count : v + count;
				});
			}
		}

		reader.close();

		return new DailyCounts(counts);
	}
}
