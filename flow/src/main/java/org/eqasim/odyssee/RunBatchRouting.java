package org.eqasim.odyssee;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Injector;

public class RunBatchRouting {
	static public void main(String[] args) throws ConfigurationException, InterruptedException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "input-path", "output-path") //
				.allowOptions("threads", "batch-size", "update-network-path", "minimum-speed") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				EqasimConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);

		config.plans().setInputFile(null);
		config.facilities().setInputFile(null);
		config.transit().setVehiclesFile(null);
		config.households().setInputFile(null);
		config.plans().setInputFile(null);

		int batchSize = cmd.getOption("batch-size").map(Integer::parseInt).orElse(100);
		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());

		Scenario scenario = ScenarioUtils.loadScenario(config);

		if (cmd.hasOption("update-network-path")) {
			Network updateNetwork = NetworkUtils.createNetwork();
			new MatsimNetworkReader(updateNetwork).readFile(cmd.getOptionStrict("update-network-path"));

			for (Link link : updateNetwork.getLinks().values()) {
				scenario.getNetwork().getLinks().get(link.getId()).setFreespeed(link.getFreespeed());
			}

			if (cmd.hasOption("minimum-speed")) {
				double minimumSpeed = Double.parseDouble(cmd.getOptionStrict("minimum-speed"));

				for (Link link : scenario.getNetwork().getLinks().values()) {
					if (link.getFreespeed() < minimumSpeed) {
						link.setFreespeed(minimumSpeed);
					}
				}
			}
		}

		Injector injector = new InjectorBuilder(scenario) //
				.addOverridingModules(EqasimConfigurator.getModules()) //
				.build();

		List<RoutingTask> tasks = new RoutingTaskReader().read(new File(cmd.getOptionStrict("input-path")));

		BatchRouter router = new BatchRouter(numberOfThreads, batchSize, injector);
		List<RoutingResult> results = router.process(tasks);

		new RoutingResultWriter(results).write(new File(cmd.getOptionStrict("output-path")));
	}
}
