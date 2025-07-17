package org.eqasim.switzerland.ch;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.eqasim.switzerland.ch.utils.pricing.inputs.zonal.ZonalReader;
import org.eqasim.switzerland.ch.utils.pricing.inputs.zonal.ZonalRegistry;
import org.eqasim.switzerland.ch.utils.pricing.inputs.zonal.Zone;
import org.eqasim.switzerland.ch.utils.pricing.stopvisiteventhandler.StopVisitModule;
import org.eqasim.switzerland.ch.utils.pricing.inputs.zonal.Authority;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import com.opencsv.exceptions.CsvValidationException;

import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException, IOException {
		// set preventwaitingtoentertraffic to y if you want to to prevent that waiting traffic has to wait for space in the link buffer
		// this is especially important to avoid high waiting times when we cutout scenarios from a larger scenario.
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-parameter", "cost-parameter", "preventwaitingtoentertraffic", "samplingRateForPT") //
				.build();

		File path = new File("/cluster/project/cmdp/asallard/analysis/PT pricing/data/gtfs_zones.csv");

		ZonalReader zonalReader = new ZonalReader();
		ZonalRegistry zonalRegistry = null;
		try {
			Collection<Authority> authorities = zonalReader.readTarifNetworks(path);
			Collection<Zone> zones = zonalReader.readZones(path, authorities);
			zonalRegistry = new ZonalRegistry(authorities, zones);

			System.out.println("Test of zonal registry");
			System.out.println(zonalRegistry.getZones("8590554"));
		} catch (CsvValidationException e) {
		}
		
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
