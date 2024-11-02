package org.eqasim.core.scenario.routing;

import java.util.HashSet;
import java.util.Set;

import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.scenario.validation.VehiclesValidator;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.termination.EqasimTerminationConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceModule;
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
	static public void main(String[] args) throws ConfigurationException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "output-path") //
				.allowOptions("threads", "batch-size", "modes") //
				.build();

		EqasimConfigurator configurator = new EqasimConfigurator();
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

		Injector injector = new InjectorBuilder(scenario) //
				.addOverridingModules(configurator.getModules(config).stream()
						.filter(module -> !(module instanceof AbstractEqasimExtension)) //
						.filter(module -> !(module instanceof DiscreteModeChoiceModule)).toList()) //
				.addOverridingModule(new PopulationRouterModule(numberOfThreads, batchSize, true, modes)) //
				.addOverridingModule(new TimeInterpretationModule()).build();

		PopulationRouter populationRouter = injector.getInstance(PopulationRouter.class);
		populationRouter.run(scenario.getPopulation());

		new PopulationWriter(scenario.getPopulation()).write(cmd.getOptionStrict("output-path"));
	}
}
