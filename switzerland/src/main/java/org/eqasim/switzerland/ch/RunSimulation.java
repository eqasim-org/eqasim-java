package org.eqasim.switzerland.ch;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		// set preventwaitingtoentertraffic to y if you want to to prevent that waiting traffic has to wait for space in the link buffer
		// this is especially important to avoid high waiting times when we cutout scenarios from a larger scenario.
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-parameter", "cost-parameter", "preventwaitingtoentertraffic", "samplingRateForPT") //
				.build();

		SwitzerlandConfigurator configurator = new SwitzerlandConfigurator(cmd);
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
		configurator.updateConfig(config);
		cmd.applyConfiguration(config);

		if (cmd.hasOption("preventwaitingtoentertraffic")) {
			if (cmd.getOption("preventwaitingtoentertraffic").get().equals("y")) {
				((QSimConfigGroup) config.getModules().get(QSimConfigGroup.GROUP_NAME))
						.setPcuThresholdForFlowCapacityEasing(1.0);
			}
		}

		Scenario scenario = ScenarioUtils.createScenario(config);
		

		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		configurator.adjustScenario(scenario);
		configurator.adjustPTpcu(scenario);
		Controler controller = new Controler(scenario);
		configurator.configureController(controller);
        controller.addOverridingModule(new PTPassengerCountsModule());
        controller.addOverridingModule(new PTLinkVolumesModule());

		 // To use the deterministic pt simulation (Part 1 of 2):
        controller.addOverridingModule(new SBBTransitModule());
        // To use the deterministic pt simulation (Part 2 of 2):
        controller.configureQSimComponents(components -> {
            new SBBTransitEngineQSimModule().configure(components);

        });
		controller.run();
	}
}
