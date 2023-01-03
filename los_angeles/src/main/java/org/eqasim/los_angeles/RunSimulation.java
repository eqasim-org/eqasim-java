package org.eqasim.los_angeles;

import org.eqasim.core.analysis.DistanceUnit;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.los_angeles.mode_choice.LosAngelesModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-parameter", "cost-parameter") //
				.build();

		EqasimConfigurator configurator = new EqasimConfigurator();
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configurator.getConfigGroups());
		EqasimConfigGroup.get(config).setAnalysisInterval(5);
		EqasimConfigGroup.get(config).setDistanceUnit(DistanceUnit.foot);
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		configurator.adjustScenario(scenario);

		EqasimConfigGroup eqasimConfig = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);
		eqasimConfig.setEstimator("walk", "laWalkEstimator");
		eqasimConfig.setEstimator("pt", "laPTEstimator");
		eqasimConfig.setEstimator("car", "laCarEstimator");

		Controler controller = new Controler(scenario);
		configurator.configureController(controller);
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new LosAngelesModeChoiceModule(cmd));
		controller.addOverridingModule(new EqasimAnalysisModule());
		// controller.addOverridingModule(new CalibrationModule());
		controller.run();
	}
}