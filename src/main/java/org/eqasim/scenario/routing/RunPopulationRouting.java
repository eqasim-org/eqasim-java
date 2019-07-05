package org.eqasim.scenario.routing;

import java.util.HashSet;
import java.util.Set;

import org.eqasim.misc.InjectorBuilder;
import org.eqasim.simulation.ScenarioConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;

import com.google.inject.Injector;

public class RunPopulationRouting {
	static public void main(String[] args) throws ConfigurationException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "output-path") //
				.allowOptions("threads", "batch-size") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));

		int batchSize = cmd.getOption("batch-size").map(Integer::parseInt).orElse(10);
		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());

		Scenario scenario = ScenarioUtils.loadScenario(config);

		if (scenario.getActivityFacilities() != null) {
			for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
				if (facility.getLinkId() == null) {
					throw new IllegalStateException("Expecting facilities to have link IDs!");
				}
			}
		}

		// TODO: Brign this to the pipeline
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains("car")) {
				Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
				allowedModes.add("car_passenger");
				allowedModes.add("truck");
				link.setAllowedModes(allowedModes);
			}
		}

		Injector injector = new InjectorBuilder(scenario) //
				.addOverridingModules(ScenarioConfigurator.getModules()) //
				.addOverridingModule(new PopulationRouterModule(numberOfThreads, batchSize)) //
				.build();

		PopulationRouter populationRouter = injector.getInstance(PopulationRouter.class);
		populationRouter.run(scenario.getPopulation());

		new PopulationWriter(scenario.getPopulation()).write(cmd.getOptionStrict("output-path"));
	}
}
