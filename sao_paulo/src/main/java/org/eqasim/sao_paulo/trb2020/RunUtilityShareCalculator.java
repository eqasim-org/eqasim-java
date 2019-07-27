package org.eqasim.sao_paulo.trb2020;

import com.google.inject.Injector;
import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class RunUtilityShareCalculator {
	static public void main(String[] args) throws ConfigurationException, InterruptedException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "output-path") //
				.allowOptions("threads", "batch-size") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				EqasimConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);
		config.strategy().clearStrategySettings();

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

		Injector injector = new InjectorBuilder(scenario) //
				.addOverridingModules(EqasimConfigurator.getModules()) //
				.addOverridingModule(new UtilityShareCalculatorModule(numberOfThreads, batchSize)) //
				.build();

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(cmd.getOptionStrict("output-path"))));

		UtilityShareCalculator utilityShareCalculator = injector.getInstance(UtilityShareCalculator.class);
		utilityShareCalculator.run(scenario.getPopulation(), writer);

	}
}
