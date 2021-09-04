package org.eqasim.ile_de_france.analysis.counts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class CountsReader {
	Set<Id<Link>> readLinks(File path) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

		Set<Id<Link>> linkIds = new HashSet<>();

		String line = null;
		List<String> header = null;

		while ((line = reader.readLine()) != null) {
			List<String> row = Arrays.asList(line.trim().split(";"));

			if (header == null) {
				header = row;
			} else {
				linkIds.add(Id.createLinkId(row.get(header.indexOf("link_id")).trim()));
			}
		}

		reader.close();

		return linkIds;
	}
}
