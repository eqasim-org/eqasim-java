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
				.requireOptions("network-path","number-of-pairs","input-departure-times", "output-path") //
				.build();

		Network fullNetwork = NetworkUtils.createNetwork();
		Network network = NetworkUtils.createNetwork();

		new MatsimNetworkReader(fullNetwork).readFile(cmd.getOptionStrict("network-path"));
		new TransportModeNetworkFilter(fullNetwork).filter(network, Collections.singleton("car"));

		int numberOfPairs = Integer.parseInt(cmd.getOptionStrict("number-of-pairs"));
		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(cmd.getOptionStrict("output-path"))));

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
		for (int i = 0; i < numberOfPairs; i++) {
			Link originLink = linksO.get(i);
			Link destinationLink = linksD.get(i);
			writer.write(String.join(",", new String[] { //
					String.valueOf(tripId++),
					String.valueOf(originLink.getCoord().getX()),
					String.valueOf(originLink.getCoord().getY()), //
					String.valueOf(destinationLink.getCoord().getX()),
					String.valueOf(destinationLink.getCoord().getY()),
					String.valueOf(2.0 * 3600.0)
			}) + "\n");
			
			writer.write(String.join(",", new String[] { //
					String.valueOf(tripId++),
					String.valueOf(originLink.getCoord().getX()),
					String.valueOf(originLink.getCoord().getY()), //
					String.valueOf(destinationLink.getCoord().getX()),
					String.valueOf(destinationLink.getCoord().getY()),
					String.valueOf(5.0 * 3600.0)
			}) + "\n");
			
			writer.write(String.join(",", new String[] { //
					String.valueOf(tripId++),
					String.valueOf(originLink.getCoord().getX()),
					String.valueOf(originLink.getCoord().getY()), //
					String.valueOf(destinationLink.getCoord().getX()),
					String.valueOf(destinationLink.getCoord().getY()),
					String.valueOf(7.0 * 3600.0)
			}) + "\n");
			
			writer.write(String.join(",", new String[] { //
					String.valueOf(tripId++),
					String.valueOf(originLink.getCoord().getX()),
					String.valueOf(originLink.getCoord().getY()), //
					String.valueOf(destinationLink.getCoord().getX()),
					String.valueOf(destinationLink.getCoord().getY()),
					String.valueOf(11.0 * 3600.0)
			}) + "\n");
			
			writer.write(String.join(",", new String[] { //
					String.valueOf(tripId++),
					String.valueOf(originLink.getCoord().getX()),
					String.valueOf(originLink.getCoord().getY()), //
					String.valueOf(destinationLink.getCoord().getX()),
					String.valueOf(destinationLink.getCoord().getY()),
					String.valueOf(18.0 * 3600.0)
			}) + "\n");
			writer.flush();
		}
		
		writer.close();
	}

}
