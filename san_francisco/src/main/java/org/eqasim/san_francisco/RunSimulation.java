package org.eqasim.san_francisco;

import org.eqasim.core.analysis.DistanceUnit;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.san_francisco.bike.SanFranciscoBikeModule;
import org.eqasim.san_francisco.mode_choice.SanFranciscoModeChoiceModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.LinkedList;
import java.util.List;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-parameter", "cost-parameter") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				EqasimConfigurator.getConfigGroups());
		EqasimConfigGroup.get(config).setTripAnalysisInterval(5);
		EqasimConfigGroup.get(config).setDistanceUnit(DistanceUnit.foot);
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		EqasimConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		EqasimConfigurator.adjustScenario(scenario);

		EqasimConfigGroup eqasimConfig = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);
		eqasimConfig.setEstimator("walk", "sfWalkEstimator");
		eqasimConfig.setEstimator("bike", "sfBikeEstimator");
		eqasimConfig.setEstimator("pt", "sfPTEstimator");
		eqasimConfig.setEstimator("car", "sfCarEstimator");

		// add bike
		VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.bike, VehicleType.class));
		bike.setMaximumVelocity(4.16666666); // 15km/h
		bike.setPcuEquivalents(0.25);
		scenario.getVehicles().addVehicleType(bike);

		// add bike as network mode
		List<String> networkModes = new LinkedList<>(config.plansCalcRoute().getNetworkModes());
		networkModes.add(TransportMode.bike);
		config.plansCalcRoute().setNetworkModes(networkModes);

		Controler controller = new Controler(scenario);

		EqasimConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new SanFranciscoModeChoiceModule(cmd));
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new SanFranciscoBikeModule());
		// controller.addOverridingModule(new CalibrationModule());
		controller.run();
	}
}