package org.eqasim.ile_de_france.analysis.counts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;

import com.google.common.base.Verify;

public class CountsReader {
	public IdSet<Link> readLinks(File path) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
		IdSet<Link> linkIds = new IdSet<>(Link.class);

		String line = null;
		List<String> header = null;

		while ((line = reader.readLine()) != null) {
			List<String> row = Arrays.asList(line.trim().split(";"));

			if (header == null) {
				header = row;

				Verify.verify(header.contains("link_id"), "Missing link_id in counts file");
			} else {
				Id<Link> linkId = Id.createLinkId(row.get(header.indexOf("link_id")));
				linkIds.add(linkId);
			}
		}

		reader.close();

		return linkIds;
	}
}
