package org.eqasim.san_francisco.analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;

public class GenerateLinkPairsForAnalysis {

	public static void main(String[] args) throws ConfigurationException, IOException {

		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("network-path", "number-of-pairs", "output-path") //
				.build();

		Network fullNetwork = NetworkUtils.createNetwork();
		Network network = NetworkUtils.createNetwork();

		new MatsimNetworkReader(fullNetwork).readFile(cmd.getOptionStrict("network-path"));
		new TransportModeNetworkFilter(fullNetwork).filter(network, Collections.singleton("car"));

		int numberOfPairs = Integer.parseInt(cmd.getOptionStrict("number-of-pairs"));
		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(cmd.getOptionStrict("output-path"))));
		writer.write(String.join(",", new String[] { //
				"trip_id", "start_coord_x", "start_coord_y", "end_coord_x", "end_coord_y", "departure_time" }) + "\n");
		writer.flush();
		List<Link> linksO = new ArrayList<>();
		List<Link> linksD = new ArrayList<>();

		for (Link link : network.getLinks().values()) {

			if (link.getAttributes().getAttribute("osm:way:highway").equals("residential")) {

				linksO.add(link);
				linksD.add(link);
			}
		}
		Collections.shuffle(linksO);
		Collections.shuffle(linksD);
		int tripId = 0;
		
		int[] hours = {6,8,12,18,20};
		
		for (int i = 0; i < numberOfPairs; i++) {
			Link originLink = linksO.get(i);
			Link destinationLink = linksD.get(i);
			for (int h = 0; h < hours.length; h ++) {
				int hour = hours[h];
				writer.write(String.join(",", new String[] { //
						String.valueOf(tripId++), String.valueOf(originLink.getCoord().getX()),
						String.valueOf(originLink.getCoord().getY()), //
						String.valueOf(destinationLink.getCoord().getX()),
						String.valueOf(destinationLink.getCoord().getY()), String.valueOf(hour * 3600.0) }) + "\n");
			}
			writer.flush();
		}

		writer.close();
	}

}
