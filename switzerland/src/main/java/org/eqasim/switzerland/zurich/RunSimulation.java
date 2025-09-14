package org.eqasim.switzerland.zurich;

import java.io.IOException;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunSimulation {

	/**
	 * 
	 * you need one argument to run this class
	 * --config-path "path-to-your-config-file/config.xml"
	 * 
	 */
	static public void main(String[] args) throws ConfigurationException, MalformedURLException, IOException {

		// set preventwaitingtoentertraffic to y if you want to to prevent that waiting
		// traffic has to wait for space in the link buffer
		// this is especially important to avoid high waiting times when we cutout
		// scenarios from a larger scenario.

		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-parameter", "cost-parameter", "preventwaitingtoentertraffic") //
				.build();

		ZurichConfigurator zurichConfigurator = new ZurichConfigurator(cmd);
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
		zurichConfigurator.updateConfig(config);
		zurichConfigurator.configure(config);
		cmd.applyConfiguration(config);

		if (cmd.hasOption("preventwaitingtoentertraffic")) {
			if (cmd.getOption("preventwaitingtoentertraffic").get().equals("y")) {
				((QSimConfigGroup) config.getModules().get(QSimConfigGroup.GROUP_NAME))
						.setPcuThresholdForFlowCapacityEasing(1.0);
			}
		}
		Scenario scenario = ScenarioUtils.createScenario(config);

		zurichConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		zurichConfigurator.adjustScenario(scenario);

		Controler controller = new Controler(scenario);
		zurichConfigurator.configureController(controller);
		controller.run();
	}
}