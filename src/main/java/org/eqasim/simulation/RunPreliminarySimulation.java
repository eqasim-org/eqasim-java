package org.eqasim.simulation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunPreliminarySimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);

		Controler controller = new Controler(scenario);
		ScenarioConfigurator.configureController(controller);
		controller.run();
	}
}
