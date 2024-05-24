package org.eqasim.ile_de_france.analysis.counts;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

public class RunCountsAnalysis {
	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("events-path", "reference-path", "network-path", "output-path", "sample-size") //
				.build();

		File eventsPath = new File(cmd.getOptionStrict("events-path"));
		File outputPath = new File(cmd.getOptionStrict("output-path"));
		File referencePath = new File(cmd.getOptionStrict("reference-path"));
		File networkPath = new File(cmd.getOptionStrict("network-path"));
		double scalingFactor = 1.0 / Double.parseDouble(cmd.getOptionStrict("sample-size"));

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkPath.toString());

		IdSet<Link> linkIds = new CountsReader().readLinks(referencePath);
		CountsHandler handler = new CountsHandler(linkIds);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(handler);

		new MatsimEventsReader(eventsManager).readFile(eventsPath.toString());

		new CountsWriter(handler.getCounts(), network, scalingFactor).write(outputPath);
	}
}
