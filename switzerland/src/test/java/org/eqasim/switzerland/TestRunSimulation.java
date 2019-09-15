package org.eqasim.switzerland;

import java.net.URL;

import org.eqasim.core.misc.SiouxFallsConfigurator;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

public class TestRunSimulation {
	@Test
	public void testRun() throws ConfigurationException {
		URL configUrl = IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("siouxfalls-2014"), "config_default.xml");
		Config config = SiouxFallsConfigurator.configure(configUrl, SwitzerlandConfigurator.getConfigGroups());
		CommandLine cmd = new CommandLine.Builder(new String[] {}).build();

		Scenario scenario = ScenarioUtils.createScenario(config);

		SwitzerlandConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		SwitzerlandConfigurator.adjustScenario(scenario);

		Controler controller = new Controler(scenario);
		SwitzerlandConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new SwissModeChoiceModule(cmd));

		controller.run();
	}
}
