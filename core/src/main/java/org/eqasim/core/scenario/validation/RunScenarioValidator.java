package org.eqasim.core.scenario.validation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class RunScenarioValidator {
	public static void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
		Scenario scenario = ScenarioUtils.loadScenario(config);

		ScenarioValidator scenarioValidator = new ScenarioValidator();
		scenarioValidator.checkScenario(scenario);
	}
}
