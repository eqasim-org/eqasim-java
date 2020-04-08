package org.eqasim.wayne_county;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.wayne_county.mode_choice.WayneCountyModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigGroup.OperationalScheme;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.NetworkCleaner;

import ch.ethz.matsim.discrete_mode_choice.modules.DiscreteModeChoiceModule;
import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

public class RunSimulation_drt_test {

	public static void main(String[] args) throws ConfigurationException {

		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-parameter", "cost-parameter") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				EqasimConfigurator.getConfigGroups());

		

		EqasimConfigGroup.get(config).setTripAnalysisInterval(5);
		cmd.applyConfiguration(config);

		// new input of drt
		// Set up DRT

		boolean useDrt = true;
		String drtMode = "drt";

		DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);
		if (true) {
			DvrpConfigGroup dvrpConfig = new DvrpConfigGroup();
			config.addModule(dvrpConfig);
			MultiModeDrtConfigGroup multiModeDrtConfig = new MultiModeDrtConfigGroup();
			config.addModule(multiModeDrtConfig);
			DrtConfigGroup drtConfig = new DrtConfigGroup();
			multiModeDrtConfig.addParameterSet(drtConfig);
			drtConfig.setMaxTravelTimeBeta(900.0);
			drtConfig.setMaxTravelTimeAlpha(1.4);
			drtConfig.setMaxWaitTime(900.0);
			drtConfig.setStopDuration(105);
			drtConfig.setOperationalScheme("stopbased");

			ModeParams modeParams = new ModeParams(drtMode);
			config.planCalcScore().addModeParams(modeParams);
			drtConfig.setTransitStopFile("drt_stops.xml");
			drtConfig.setMaxWalkDistance(800.0);
			drtConfig.setVehiclesFile("drt_vehicle_20.xml");
			DrtConfigs.adjustDrtConfig(drtConfig, config.planCalcScore());
			List<String> cachedModes = new ArrayList<>(dmcConfig.getCachedModes());
			cachedModes.add(drtMode);
			dmcConfig.setCachedModes(cachedModes);
			List<String> availableModes = new ArrayList<>(dmcConfig.getCarModeAvailabilityConfig().getAvailableModes());
			availableModes.add(drtMode);
			dmcConfig.getCarModeAvailabilityConfig().setAvailableModes(availableModes);
		}

//end of new input for drt

		// add truck estimator
		for (String mode : Arrays.asList("truck")) {
			EqasimConfigGroup.get(config).setEstimator(mode, EqasimModeChoiceModule.ZERO_ESTIMATOR_NAME);
		}

		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class,
				new DrtRouteFactory());
		
		
		EqasimConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.car)) {
				Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
				allowedModes.add("drt");
				link.setAllowedModes(allowedModes);
			}
		}		

		addCoordinatesToActivities(scenario);
		EqasimConfigurator.adjustScenario(scenario);

		EqasimConfigGroup eqasimConfig = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);

		eqasimConfig.setEstimator("walk", "wcWalkEstimator");
		eqasimConfig.setEstimator("pt", "wcPTEstimator");
		eqasimConfig.setEstimator("car", "wcCarEstimator");
		eqasimConfig.setEstimator("bike", "wcBikeEstimator");

		Controler controller = new Controler(scenario);
		//EqasimConfigurator.configureController(controller);
		controller.addOverridingModule(new SwissRailRaptorModule());
		controller.addOverridingModule(new DiscreteModeChoiceModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new WayneCountyModeChoiceModule(cmd));
		//controller.addOverridingModule(new EqasimAnalysisModule());
		// controller.addOverridingModule(new CalibrationModule());

		// second part of drt
		if (useDrt) {
			eqasimConfig.setEstimator("drt", "wcDRTEstimator");
			controller.addOverridingModule(new MultiModeDrtModule());
			controller.addOverridingModule(new DvrpModule());
			controller.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(config)));
		}
		// end of second part of drt

		controller.run();

	}

	private static void addCoordinatesToActivities(Scenario scenario) {
		for (Person person : scenario.getPopulation().getPersons().values()) {

			Plan plan = person.getPlans().get(0);

			for (PlanElement pe : plan.getPlanElements()) {

				if (pe instanceof Activity) {
					Link link = scenario.getNetwork().getLinks().get(((Activity) pe).getLinkId());
					
					((Activity) pe).setCoord(link.getCoord());
				}
			}
		}
	}

}
