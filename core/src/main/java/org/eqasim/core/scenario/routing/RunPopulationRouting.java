package org.eqasim.core.scenario.routing;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.scenario.validation.VehiclesValidator;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.termination.EqasimTerminationConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretationModule;
import org.matsim.facilities.ActivityFacility;

import com.google.inject.Injector;

public class RunPopulationRouting {
	static public void main(String[] args) throws ConfigurationException, InterruptedException, ExecutionException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowOptions("output-path", "threads", "batch-size", "modes", "routes-path", "export-trips-path",
						"export-only", EqasimConfigurator.CONFIGURATOR) //
				.build();
		boolean exportOnly = cmd.getOption("export-only").map(v -> {
			if (!v.equalsIgnoreCase("true") && !v.equalsIgnoreCase("false"))
				throw new IllegalArgumentException("export-only must be true or false");
			return Boolean.parseBoolean(v);
		}).orElse(false);
		if (exportOnly && !cmd.hasOption("export-trips-path"))
			throw new IllegalArgumentException("export-only requires export-trips-path");
		if (!exportOnly) cmd.getOptionStrict("output-path");

		EqasimConfigurator configurator = EqasimConfigurator.getInstance(cmd);
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
		configurator.updateConfig(config);
		config.getModules().remove(EqasimTerminationConfigGroup.GROUP_NAME);
		cmd.applyConfiguration(config);
		config.replanning().clearStrategySettings();
		VehiclesValidator.validate(config);

		int batchSize = cmd.getOption("batch-size").map(Integer::parseInt).orElse(100);
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

		Set<String> modes = new HashSet<>();

		if (cmd.hasOption("modes")) {
			for (String mode : cmd.getOptionStrict("modes").split(",")) {
				modes.add(mode.trim());
			}
		}
		PopulationRoutingIO.enrichTrips(scenario);
		try {
			if (cmd.hasOption("export-trips-path")) PopulationRoutingIO.exportTrips(scenario,
					java.nio.file.Path.of(cmd.getOptionStrict("export-trips-path")), modes);
			if (exportOnly) return;
			var reused = cmd.hasOption("routes-path") ? PopulationRoutingIO.importRoutes(scenario,
					java.nio.file.Path.of(cmd.getOptionStrict("routes-path")), modes)
					: java.util.Map.<org.matsim.api.core.v01.population.Plan, Set<org.matsim.api.core.v01.population.Activity>>of();

			if (PopulationRoutingIO.needsRouting(scenario, modes, reused)) {
				Injector injector = new InjectorBuilder(scenario, configurator) //
						.addOverridingModule(new PopulationRouterModule(numberOfThreads, batchSize, true, modes, reused)) //
						.addOverridingModule(new TimeInterpretationModule()).build();

				PopulationRouter populationRouter = injector.getInstance(PopulationRouter.class);
				populationRouter.run(scenario.getPopulation());
			}

			new PopulationWriter(scenario.getPopulation()).write(cmd.getOptionStrict("output-path"));
		} catch (java.io.IOException e) {
			throw new java.io.UncheckedIOException("Could not read/write population routing CSV", e);
		}
	}
}
