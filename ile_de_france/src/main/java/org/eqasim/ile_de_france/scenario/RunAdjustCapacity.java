package org.eqasim.ile_de_france.scenario;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.extent.ShapeScenarioExtent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;

public class RunAdjustCapacity {
	static public void main(String[] args) throws ConfigurationException, InterruptedException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path", "extent-path", "factor") //
				.build();

		String inputPath = cmd.getOptionStrict("input-path");
		String extentPath = cmd.getOptionStrict("extent-path");
		String outputPath = cmd.getOptionStrict("output-path");
		double factor = Double.parseDouble(cmd.getOptionStrict("factor"));

		ScenarioExtent extent = new ShapeScenarioExtent.Builder(new File(extentPath), Optional.empty(),
				Optional.empty()).build();

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(cmd.getOptionStrict("input-path"));

		for (Link link : network.getLinks().values()) {
			if (extent.isInside(link.getCoord())) {
				link.setCapacity(link.getCapacity() * factor);
			}
		}

		new NetworkWriter(network).write(cmd.getOptionStrict("output-path"));
	}
}
