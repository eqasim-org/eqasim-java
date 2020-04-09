package org.eqasim.examples.zurich_adpt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import org.eqasim.automated_vehicles.components.AvConfigurator;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.examples.zurich_adpt.mode_choice.costs.ZonalVariables;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones.Zone;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones.Zones;
import org.eqasim.examples.zurich_adpt.scenario.AdPTModule;
import org.eqasim.switzerland.SwitzerlandConfigurator;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;

/**
 * This is an example run script that runs the Switzerland/Zurich scenario with
 * automated vehicles. This script basically resembles the basic RunSimulation
 * script from the Switzerland scenario, but with a couple of added components
 * to use the AV module. They are marked further below.
 */
public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException, MalformedURLException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "zones-shapefile", "costs-zones") //
				.allowPrefixes("mode-parameter", "cost-parameter") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				SwitzerlandConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);
		dmcConfig.setModeAvailability(AdPTModule.ADPT_MODE_AVAILABILITY_NAME);

		//read zones
		Map<String, Zone> mapZones = Zone.read(new File(cmd.getOptionStrict("zones-shapefile")));
		Zones zones = new Zones(mapZones);
		ZonalVariables zonalVariables = new ZonalVariables();
		zonalVariables.readFile(cmd.getOptionStrict("costs-zones"));
		
		Scenario scenario = ScenarioUtils.createScenario(config);

		SwitzerlandConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		SwitzerlandConfigurator.adjustScenario(scenario);

		// The AvConfigurator provides some convenience functions to adjust the
		// scenario. Here, we add the mode 'av' to all links that have the 'car' mode
		// and define that all links belong to one waiting time estimation group (i.e.
		// we estimate an overall waiting time average over all links).
		AvConfigurator.configureCarLinks(scenario);
		AvConfigurator.configureUniformWaitingTimeGroup(scenario);

		Controler controller = new Controler(scenario);
		SwitzerlandConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new SwissModeChoiceModule(cmd));
		AvConfigurator.configureController(controller, cmd); // Add some modules for AV

		// Here we add our custom AdPT module to add our specific ModeAvailability
		controller.addOverridingModule(new AdPTModule(zones, zonalVariables));

		controller.run();
	}
}
