package org.sutlab.seville_extended;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.scenario.validation.VehiclesValidator;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.sutlab.seville_extended.mode_choice.SevilleModeChoiceModule;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-choice-parameter", "cost-parameter") //
				.build();

		SevilleConfigurator configurator = new SevilleConfigurator(cmd);
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
		configurator.updateConfig(config);

		// Existing scenario configs use the core walk estimator, switch to the Seville one
		EqasimConfigGroup.get(config).setEstimator(TransportMode.walk, SevilleModeChoiceModule.WALK_ESTIMATOR_NAME);

		cmd.applyConfiguration(config);
		VehiclesValidator.validate(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		configurator.adjustScenario(scenario);

		Controler controller = new Controler(scenario);
		configurator.configureController(controller);

		controller.run();
	}
}