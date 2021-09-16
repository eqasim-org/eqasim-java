package org.eqasim.core.scenario.cutter.network;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.extent.ShapeScenarioExtent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.UncheckedIOException;

public class RunNetworkCutter {
	static public void main(String[] args) throws MalformedURLException, IOException, InterruptedException,
			UncheckedIOException, ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path", "extent-path") //
				.allowOptions("threads", "prefix", "extent-attribute", "extent-value", "modes") //
				.build();

		// Load scenario extent
		File extentPath = new File(cmd.getOptionStrict("extent-path"));
		Optional<String> extentAttribute = cmd.getOption("extent-attribute");
		Optional<String> extentValue = cmd.getOption("extent-value");
		ScenarioExtent extent = new ShapeScenarioExtent.Builder(extentPath, extentAttribute, extentValue).build();

		// Load network
		Network fullNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader(fullNetwork).readFile(cmd.getOptionStrict("input-path"));

		// Filter modes
		Set<String> modes = new HashSet<>();

		Arrays.asList(cmd.getOption("modes").orElse("car").split(",")).forEach(mode -> {
			modes.add(mode.trim());
		});

		Network filteredNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(fullNetwork).filter(filteredNetwork, modes);

		// Cut network
		RoadNetwork roadNetwork = new RoadNetwork(filteredNetwork);
		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());

		MinimumNetworkFinder minimumNetworkFinder = new MinimumNetworkFinder(extent, roadNetwork, numberOfThreads, 100);
		new NetworkCutter(extent, ScenarioUtils.createScenario(ConfigUtils.createConfig()), minimumNetworkFinder)
				.run(roadNetwork);

		new NetworkWriter(roadNetwork).write(cmd.getOptionStrict("output-path"));
	}
}
