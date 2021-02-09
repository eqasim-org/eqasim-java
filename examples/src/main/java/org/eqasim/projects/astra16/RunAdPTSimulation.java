package org.eqasim.projects.astra16;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.calibration.CalibrationModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.examples.zurich_adpt.mode_choice.AdPTModeChoiceModule;
import org.eqasim.examples.zurich_adpt.mode_choice.costs.ZonalVariables;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones.Zone;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones.Zones;
import org.eqasim.examples.zurich_adpt.scenario.AdPTModule;
import org.eqasim.projects.astra16.convergence.ConvergenceModule;
import org.eqasim.projects.astra16.travel_time.SmoothingTravelTimeModule;
import org.eqasim.projects.astra16.travel_time.TravelTimeComparisonModule;
import org.eqasim.switzerland.SwitzerlandConfigurator;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;

public class RunAdPTSimulation {
	static public void main(String[] args) throws ConfigurationException, MalformedURLException, IOException {
		// Some paramters added from AdPT
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "zones-shapefile", "costs-zones", "cordon-shapefile",
						"cordon-charges") //
				.allowPrefixes("av-mode-parameter", "mode-parameter", "cost-parameter") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), AstraConfigurator.getConfigGroups());
		AstraConfigurator.configure(config);
		cmd.applyConfiguration(config);
		AstraConfigurator.adjustOperator(config);
		
		// Start copy from AdPT
		
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
		eqasimConfig.setCostModel(TransportMode.car, AdPTModeChoiceModule.CAR_COST_MODEL_NAME);
		
		//read zones
		Map<String, Zone> mapZones = Zone.read(new File(cmd.getOptionStrict("zones-shapefile")));
		Zones zones = new Zones(mapZones);
		ZonalVariables zonalVariables = new ZonalVariables();
		zonalVariables.readFile(cmd.getOptionStrict("costs-zones"));
		
		// End of copy from AdPT

		Scenario scenario = ScenarioUtils.createScenario(config);

		SwitzerlandConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		SwitzerlandConfigurator.adjustScenario(scenario);
		AstraConfigurator.adjustScenario(scenario);

		// EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		for (Link link : scenario.getNetwork().getLinks().values()) {
			double maximumSpeed = link.getFreespeed();
			boolean isMajor = true;

			for (Link other : link.getToNode().getInLinks().values()) {
				if (other.getCapacity() >= link.getCapacity()) {
					isMajor = false;
				}
			}

			if (!isMajor && link.getToNode().getInLinks().size() > 1) {
				double travelTime = link.getLength() / maximumSpeed;
				travelTime += eqasimConfig.getCrossingPenalty();
				link.setFreespeed(link.getLength() / travelTime);
			}
		}

		// EqasimLinkSpeedCalcilator deactivated!

		Controler controller = new Controler(scenario);
		SwitzerlandConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new SwissModeChoiceModule(cmd));
		controller.addOverridingModule(new CalibrationModule());
		controller.addOverridingModule(new AstraModule(cmd));
		controller.addOverridingModule(new TravelTimeComparisonModule());
		controller.addOverridingModule(new ConvergenceModule());

		AstraConfigurator.configureController(controller, cmd);

		controller.addOverridingModule(new SmoothingTravelTimeModule());
		
		// Start added from AdPT
		
		controller.addOverridingModule(new AdPTModeChoiceModule(cmd));
		// Here we add our custom AdPT module to add our specific ModeAvailability
		controller.addOverridingModule(new AdPTModule(zones, zonalVariables));
		
		// End added from AdPT

		controller.run();
	}
}
