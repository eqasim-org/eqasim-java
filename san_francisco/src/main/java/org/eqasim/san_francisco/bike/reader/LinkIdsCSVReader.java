package org.eqasim.san_francisco.bike.reader;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LinkIdsCSVReader {

    public Set<Id<Link>> read(String path) throws IOException {
        Set<Id<Link>> ids = new HashSet<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

        List<String> header = null;
        String line = null;

        while ((line = reader.readLine()) != null) {
            List<String> row = Arrays.asList(line.split(";"));

            if (header == null) {
                header = row;
            } else {
                Id<Link> linkId = Id.createLinkId(row.get(header.indexOf("ID")));
                ids.add(linkId);
            }
        }

        reader.close();
        return ids;
    }

}
