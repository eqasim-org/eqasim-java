package org.eqasim.ile_de_france.scenario;

import java.io.IOException;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;

public class RunConfigureNetwork {
	static public void main(String[] args) throws ConfigurationException, InterruptedException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path") //
				.allowOptions("node-capacity") //
				.build();

		String inputPath = cmd.getOptionStrict("input-path");
		String outputPath = cmd.getOptionStrict("output-path");

		double nodeCapacity = cmd.getOption("node-capacity").map(Double::parseDouble).orElse(1800.0);

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(inputPath);

		for (Node node : network.getNodes().values()) {
			// calculate incoming lanes and distribute node capacity over them
			double incomingLaneCount = node.getInLinks().values().stream().mapToDouble(Link::getNumberOfLanes).sum();
			double laneCapacity = nodeCapacity / incomingLaneCount;

			for (Link link : node.getInLinks().values()) {
				// set incoming link capacity by distributing outgoing capacity
				link.setCapacity(link.getNumberOfLanes() * laneCapacity);
			}
		}

		new NetworkWriter(network).write(outputPath);
	}
}
