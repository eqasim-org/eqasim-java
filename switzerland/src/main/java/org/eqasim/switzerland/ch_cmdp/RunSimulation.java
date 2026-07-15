package org.eqasim.switzerland.ch_cmdp;

import java.io.IOException;

import org.eqasim.switzerland.ch_cmdp.StrategyWeightDecay.StrategyWeightDecayModule;
import org.eqasim.switzerland.ch_cmdp.config.SwissBikesharingConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.shared_mobility.run.SharingConfigGroup;
import org.matsim.contrib.shared_mobility.run.SharingModule;
import org.matsim.contrib.shared_mobility.service.SharingUtils;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;

public class RunSimulation {
	@SuppressWarnings("deprecation")
	static public void main(String[] args) throws IOException, org.matsim.core.config.CommandLine.ConfigurationException {
		// set preventwaitingtoentertraffic to y if you want to to prevent that waiting
		// traffic has to wait for space in the link buffer
		// this is especially important to avoid high waiting times when we cutout
		// scenarios from a larger scenario.
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-parameter", "cost-parameter", "preventwaitingtoentertraffic", "samplingRateForPT",
					"useBikesharing"
				) //
				.build();

		SwitzerlandConfigurator configurator = new SwitzerlandConfigurator(cmd);

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
		configurator.updateConfig(config);
		configurator.configure(config);
		cmd.applyConfiguration(config);

		if (cmd.hasOption("preventwaitingtoentertraffic")) {
			if (cmd.getOption("preventwaitingtoentertraffic").get().equals("y")) {
				((QSimConfigGroup) config.getModules().get(QSimConfigGroup.GROUP_NAME))
						.setPcuThresholdForFlowCapacityEasing(1.0);
			}
		}

		Scenario scenario = ScenarioUtils.createScenario(config);

		SwissRailRaptorConfigGroup srrConfigGroup = (SwissRailRaptorConfigGroup) config.getModules()
				.getOrDefault("swissRailRaptor", null);
		System.out.println("Found the following transfer penalties: "
				+ srrConfigGroup.getModeToModeTransferPenaltyParameterSets());

		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		configurator.adjustScenario(scenario);
		configurator.adjustPTpcu(scenario);
		Controler controller = new Controler(scenario);
		configurator.configureController(controller);
		controller.addOverridingModule(new PTPassengerCountsModule());
		controller.addOverridingModule(new PTLinkVolumesModule());
		controller.addOverridingModule(new StrategyWeightDecayModule());
		SwissBikesharingConfigGroup bikesharingConfig = SwissBikesharingConfigGroup.getOrCreate(config);
		boolean useBikesharing = cmd.getOption("useBikesharing")
				.map(Boolean::parseBoolean)
				.orElse(bikesharingConfig.isUseBikesharing());
		if (config.getModules().containsKey(SharingConfigGroup.GROUP_NAME) && useBikesharing) 
			controller.addOverridingModule(new SharingModule());

		// To use the deterministic pt simulation (Part 1 of 2):
		controller.addOverridingModule(new SBBTransitModule());

		controller.configureQSimComponents(components -> {
			// To use the deterministic pt simulation (Part 2 of 2):
			new SBBTransitEngineQSimModule().configure(components);

			// Enable QSim components for shared mobility.
			if (config.getModules().containsKey(SharingConfigGroup.GROUP_NAME) && useBikesharing) {
				SharingConfigGroup scf = (SharingConfigGroup) config.getModules().get(SharingConfigGroup.GROUP_NAME);
				SharingUtils.configureQSim(scf).configure(components);
			}
		});
		controller.run();
	}
}
