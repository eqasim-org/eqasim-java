package org.eqasim.sao_paulo;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.sao_paulo.mode_choice.SaoPauloModeChoiceModule;
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

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				EqasimConfigurator.getConfigGroups());
		EqasimConfigGroup.get(config).setTripAnalysisInterval(1);
		cmd.applyConfiguration(config);
		
		config.controler().setLastIteration(0);
		config.transit().setUseTransit(true);
		config.transit().setUsingTransitInMobsim(true);

		Scenario scenario = ScenarioUtils.createScenario(config);
		EqasimConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		EqasimConfigurator.adjustScenario(scenario);
		
		EqasimConfigGroup eqasimConfig = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);
		eqasimConfig.setEstimator("walk", "spWalkEstimator");
		eqasimConfig.setEstimator("pt", "spPTEstimator");
		eqasimConfig.setEstimator("car", "spCarEstimator");
		eqasimConfig.setEstimator("taxi", "spTaxiEstimator");		

		Controler controller = new Controler(scenario);
		EqasimConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new SaoPauloModeChoiceModule(cmd));
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.run();
	}
}