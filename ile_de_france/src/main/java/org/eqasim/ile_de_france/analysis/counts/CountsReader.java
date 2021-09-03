package org.eqasim.ile_de_france.analysis.counts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class CountsReader {
	Set<Id<Link>> readLinks(File path) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

		Set<Id<Link>> linkIds = new HashSet<>();

		String line = null;
		while ((line = reader.readLine()) != null) {
			linkIds.add(Id.createLinkId(line.trim()));
		}

		reader.close();

		return linkIds;
	}
}
