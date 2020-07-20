package org.eqasim.jakarta;



import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.jakarta.mode_choice.JakartaModeChoiceModule;
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

		Scenario scenario = ScenarioUtils.createScenario(config);
		EqasimConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		EqasimConfigurator.adjustScenario(scenario);
		
		EqasimConfigGroup eqasimConfig = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);
		eqasimConfig.setEstimator("walk", "jWalkEstimator");
		eqasimConfig.setEstimator("pt", "jPTEstimator");
		eqasimConfig.setEstimator("motorcycle", "jMotorcycleEstimator");
		eqasimConfig.setEstimator("car", "jCarEstimator");
		eqasimConfig.setEstimator("carodt", "jCarodtEstimator");
		eqasimConfig.setEstimator("mcodt", "jMcodtEstimator");
		
		//DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);
		
		//List<String> availableModes = new ArrayList<>(dmcConfig.getCarModeAvailabilityConfig().getAvailableModes());
		//availableModes.add("car_odt");
		//dmcConfig.getCarModeAvailabilityConfig().setAvailableModes(availableModes);
		

		Controler controller = new Controler(scenario);
		EqasimConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new JakartaModeChoiceModule(cmd));
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.run();
	}
}