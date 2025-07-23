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

public class RunAdjustNetwork {
	static public void main(String[] args) throws ConfigurationException, InterruptedException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path") //
				.allowOptions("capacity-factor", "speed-factor", "extent-path", "capacity-value") //
				.build();

		String inputPath = cmd.getOptionStrict("input-path");
		String outputPath = cmd.getOptionStrict("output-path");

		double capacityFactor = cmd.getOption("capacity-factor").map(Double::parseDouble).orElse(1.0);
		double speedFactor = cmd.getOption("speed-factor").map(Double::parseDouble).orElse(1.0);

		Optional<Double> capacityValue = cmd.getOption("capacity-value").map(Double::parseDouble);

		ScenarioExtent extent = null;
		if (cmd.hasOption("extent-path")) {
			String extentPath = cmd.getOptionStrict("extent-path");
			extent = new ShapeScenarioExtent.Builder(new File(extentPath), Optional.empty(),
					Optional.empty()).build();
		}

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(inputPath);

		for (Link link : network.getLinks().values()) {
			if (extent == null || extent.isInside(link.getCoord())) {
				if (capacityValue.isPresent()) {
					link.setCapacity(capacityValue.get());
				}

				link.setCapacity(link.getCapacity() * capacityFactor);
				link.setFreespeed(link.getFreespeed() * speedFactor);
			}
		}

		new NetworkWriter(network).write(outputPath);
	}
}
