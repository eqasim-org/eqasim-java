package org.eqasim.examples.zurich_adpt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eqasim.automated_vehicles.mode_choice.AvModeChoiceModule;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.examples.zurich_adpt.mode_choice.AdPTModeChoiceModule;
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
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.matsim.av.framework.AVModule;
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

		Set<String> tripConstraints = new HashSet<>();
		tripConstraints.addAll(dmcConfig.getTripConstraints());
		tripConstraints.add(AdPTModeChoiceModule.ADPT_CONSTRAINT_NAME);
		dmcConfig.setTripConstraints(tripConstraints);
		
		ModeParams modeParams = new ModeParams(AdPTModule.ADPT_MODE);
		config.planCalcScore().addModeParams(modeParams);

		// Set up Eqasim (add AV cost model and estimator)
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
		eqasimConfig.setCostModel("adpt", AdPTModeChoiceModule.ADPT_COST_MODEL_NAME);
		eqasimConfig.setEstimator("adpt", AdPTModeChoiceModule.ADPT_ESTIMATOR_NAME);
		
		//read zones
		Map<String, Zone> mapZones = Zone.read(new File(cmd.getOptionStrict("zones-shapefile")));
		Zones zones = new Zones(mapZones);
		ZonalVariables zonalVariables = new ZonalVariables();
		zonalVariables.readFile(cmd.getOptionStrict("costs-zones"));
		
		Scenario scenario = ScenarioUtils.createScenario(config);

		SwitzerlandConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		SwitzerlandConfigurator.adjustScenario(scenario);
		
		Controler controller = new Controler(scenario);
		SwitzerlandConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new SwissModeChoiceModule(cmd));
		controller.addOverridingModule(new AdPTModeChoiceModule());
		// Here we add our custom AdPT module to add our specific ModeAvailability
		controller.addOverridingModule(new AdPTModule(zones, zonalVariables));

		controller.run();
	}
}
