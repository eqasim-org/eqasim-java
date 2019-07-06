package org.eqasim.simulation;

import org.eqasim.simulation.ile_de_france.preparation.IleDeFranceConfigurator;
import org.eqasim.simulation.ile_de_france.preparation.IleDeFranceModeAvailability;
import org.eqasim.simulation.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.matsim.discrete_mode_choice.modules.AbstractDiscreteModeChoiceExtension;
import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;

public class RunIleDeFranceSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				ScenarioConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);
		dmcConfig.setModeAvailability("IDF");

		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		ScenarioConfigurator.adjustScenario(scenario);
		IleDeFranceConfigurator.adjustScenario(scenario);

		Controler controller = new Controler(scenario);
		ScenarioConfigurator.configureController(controller);
		controller.addOverridingModule(new SwissModeChoiceModule(cmd));
		controller.addOverridingModule(new AbstractDiscreteModeChoiceExtension() {
			@Override
			protected void installExtension() {
				bindModeAvailability("IDF").to(IleDeFranceModeAvailability.class);
			}
		});
		controller.run();
	}
}
