package org.eqasim.projects.dynamic_av;

import org.eqasim.automated_vehicles.components.AvConfigurator;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.calibration.CalibrationConfigGroup;
import org.eqasim.core.simulation.calibration.CalibrationModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.switzerland.SwitzerlandConfigurator;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.matsim.av.framework.AVQSimModule;

/**
 * This is an example run script that runs the Switzerland/Zurich scenario with
 * automated vehicles. This script basically resembles the basic RunSimulation
 * script from the Switzerland scenario, but with a couple of added components
 * to use the AV module. They are marked further below.
 */
public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-parameter", "cost-parameter", "av-mode-parameter", "av-cost-parameter") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				SwitzerlandConfigurator.getConfigGroups());
		CalibrationConfigGroup.get(config);
		AvConfigurator.configure(config); // Add some configuration for AV
		cmd.applyConfiguration(config);

		// Here we customize our configuration by setting the fleet size from the
		// command line
		/*
		 * OperatorConfig operatorConfig = AVConfigGroup.getOrCreate(config)
		 * .getOperatorConfig(OperatorConfig.DEFAULT_OPERATOR_ID);
		 * operatorConfig.getGeneratorConfig().setNumberOfVehicles(Integer.parseInt(cmd.
		 * getOptionStrict("fleet-size")));
		 */

		DAConfigurator.configure(config);
		
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
		eqasimConfig.setTripAnalysisInterval(1);
		
		CalibrationConfigGroup calibrationConfig = CalibrationConfigGroup.get(config);
		calibrationConfig.setEnable(true);

		Scenario scenario = ScenarioUtils.createScenario(config);

		SwitzerlandConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		SwitzerlandConfigurator.adjustScenario(scenario);
		DAConfigurator.adjustScenario(scenario);

		// The AvConfigurator provides some convenience functions to adjust the
		// scenario. Here, we add the mode 'av' to all links that have the 'car' mode
		// and define that all links belong to one waiting time estimation group (i.e.
		// we estimate an overall waiting time average over all links).
		AvConfigurator.configureCarLinks(scenario);
		AvConfigurator.configureUniformWaitingTimeGroup(scenario);

		Controler controller = new Controler(scenario);
		SwitzerlandConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new SwissModeChoiceModule(cmd));
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new CalibrationModule());
		AvConfigurator.configureController(controller, cmd); // Add some modules for AV

		// Here we add our custom Zurich/AV module to add our specific ModeAvailability
		controller.addOverridingModule(new DAModule(cmd));

		// This is not totally obvious, but we need to adjust the QSim components if we
		// have AVs
		controller.configureQSimComponents(configurator -> {
			EqasimTransitQSimModule.configure(configurator);
			AVQSimModule.configureComponents(configurator);
		});

		controller.run();
	}
}
