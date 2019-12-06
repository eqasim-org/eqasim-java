package org.eqasim.san_francisco.scenario;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;

public class RunNetworkFixer {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path") //
				.build();

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(cmd.getOptionStrict("input-path"));

		NetworkFixer networkFixer = new NetworkFixer();
		networkFixer.run(network);

		new NetworkWriter(network).write(cmd.getOptionStrict("output-path"));
	}
}
