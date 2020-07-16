package org.eqasim.san_francisco.bike.reader;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BikeInfoCSVReader {

    public Map<Id<Link>, BikeInfo> read(String path) throws IOException {
        Map<Id<Link>, BikeInfo> map = new HashMap<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

        List<String> header = null;
        String line = null;

        while ((line = reader.readLine()) != null) {
            List<String> row = Arrays.asList(line.split(";"));

            if (header == null) {
                header = row;
            } else {
                Id<Link> linkId = Id.createLinkId(row.get(header.indexOf("id")));
                String facility = row.get(header.indexOf("facility"));
                Boolean opposite = Boolean.valueOf(row.get(header.indexOf("opposite")));
                Boolean permitted = Boolean.valueOf(row.get(header.indexOf("permitted")));
                map.putIfAbsent(linkId, new BikeInfo(linkId, facility, opposite, permitted));
            }
        }

        reader.close();
        return map;
    }

}
