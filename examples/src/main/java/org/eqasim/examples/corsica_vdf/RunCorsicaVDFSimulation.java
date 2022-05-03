package org.eqasim.examples.corsica_vdf;

import java.net.URL;

import org.eqasim.core.components.traffic.EqasimTrafficQSimModule;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.eqasim.vdf.VDFConfigGroup;
import org.eqasim.vdf.VDFModule;
import org.eqasim.vdf.VDFQSimModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.common.io.Resources;

/**
 * This is an example run script that runs the Corsica test scenario with a
 * volume-delay function to simulate travel times.
 */
public class RunCorsicaVDFSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.allowPrefixes("mode-parameter", "cost-parameter") //
				.build();

		IDFConfigurator configurator = new IDFConfigurator();
		configurator.getQSimModules().removeIf(m -> m instanceof EqasimTrafficQSimModule);

		URL configUrl = Resources.getResource("corsica/corsica_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, configurator.getConfigGroups());

		config.controler().setLastIteration(2);
		config.addModule(new VDFConfigGroup());

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);

		Controler controller = new Controler(scenario);
		configurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));

		controller.addOverridingModule(new VDFModule());
		controller.addOverridingQSimModule(new VDFQSimModule());

		controller.run();
	}
}
