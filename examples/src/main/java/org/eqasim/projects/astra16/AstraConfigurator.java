package org.eqasim.projects.astra16;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.eqasim.automated_vehicles.components.AvConfigurator;
import org.eqasim.automated_vehicles.components.EqasimAvConfigGroup;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.calibration.CalibrationConfigGroup;
import org.eqasim.projects.astra16.mode_choice.AstraModeAvailability;
import org.eqasim.projects.astra16.mode_choice.AvServiceConstraint;
import org.eqasim.projects.astra16.mode_choice.InfiniteHeadwayConstraint;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraAvUtilityEstimator;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraBikeUtilityEstimator;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraCarUtilityEstimator;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraPtUtilityEstimator;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraWalkUtilityEstimator;
import org.eqasim.projects.astra16.pricing.PricingModule;
import org.eqasim.projects.astra16.pricing.model.AstraAvCostModel;
import org.eqasim.projects.astra16.service_area.ServiceArea;
import org.eqasim.projects.astra16.service_area.ServiceAreaModule;
import org.eqasim.projects.astra16.waiting_time.WaitingTimeModule;
import org.matsim.amodeus.components.dispatcher.single_heuristic.SingleHeuristicDispatcher;
import org.matsim.amodeus.config.AmodeusConfigGroup;
import org.matsim.amodeus.config.AmodeusModeConfig;
import org.matsim.amodeus.config.modal.DispatcherConfig;
import org.matsim.amodeus.config.modal.TimingConfig;
import org.matsim.amodeus.config.modal.WaitingTimeConfig;
import org.matsim.amodeus.framework.AmodeusQSimModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.households.Household;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;

public class AstraConfigurator extends EqasimConfigurator {
	private AstraConfigurator() {
	}

	static public ConfigGroup[] getConfigGroups() {
		return new ConfigGroup[] { //
				new SwissRailRaptorConfigGroup(), //
				new EqasimConfigGroup(), //
				new DiscreteModeChoiceConfigGroup(), //
				new CalibrationConfigGroup(), //
				new AstraConfigGroup(), //
				new EqasimAvConfigGroup(), //
		};
	}

	static public void configure(Config config) {
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		// General MATSim
		config.controler().setLastIteration(200);
		config.controler().setWriteEventsInterval(200);
		config.controler().setWritePlansInterval(200);

		config.qsim().setNumberOfThreads(Math.min(12, Runtime.getRuntime().availableProcessors()));
		config.global().setNumberOfThreads(Runtime.getRuntime().availableProcessors());

		for (StrategySettings strategy : config.strategy().getStrategySettings()) {
			if (strategy.getStrategyName().equals(DiscreteModeChoiceModule.STRATEGY_NAME)) {
				strategy.setWeight(0.05);
			} else {
				strategy.setWeight(0.95);
			}
		}

		// General eqasim
		eqasimConfig.setTripAnalysisInterval(config.controler().getWriteEventsInterval());

		// Estimators
		eqasimConfig.setEstimator(TransportMode.car, AstraCarUtilityEstimator.NAME);
		eqasimConfig.setEstimator(TransportMode.pt, AstraPtUtilityEstimator.NAME);
		eqasimConfig.setEstimator(TransportMode.bike, AstraBikeUtilityEstimator.NAME);
		eqasimConfig.setEstimator(TransportMode.walk, AstraWalkUtilityEstimator.NAME);

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		Set<String> tripConstraints = new HashSet<>(dmcConfig.getTripConstraints());
		tripConstraints.add(InfiniteHeadwayConstraint.NAME);
		tripConstraints.add(AvServiceConstraint.NAME);
		dmcConfig.setTripConstraints(tripConstraints);

		dmcConfig.setModeAvailability(AstraModeAvailability.NAME);

		// Add default AV configuration
		AvConfigurator.configure(config);
		eqasimConfig.setEstimator("av", AstraAvUtilityEstimator.NAME);
		eqasimConfig.setCostModel("av", AstraAvCostModel.NAME);

		// Set up AV
		AmodeusConfigGroup avConfig = AmodeusConfigGroup.get(config);
		AmodeusModeConfig modeConfig = avConfig.getMode("av");

		modeConfig.setUseAccessAgress(true);

		avConfig.setVehicleAnalysisInterval(config.controler().getWriteEventsInterval());
		avConfig.setPassengerAnalysisInterval(config.controler().getWriteEventsInterval());
	}

	static public void adjustOperator(Config config) {
		// Here we assume that all other config stuff has been handled before
		AstraConfigGroup astraConfig = AstraConfigGroup.get(config);

		AmodeusModeConfig operatorConfig = AmodeusConfigGroup.get(config).getMode("av");
		operatorConfig.getGeneratorConfig().setNumberOfVehicles(astraConfig.getFleetSize());

		WaitingTimeConfig waitingTimeConfig = operatorConfig.getWaitingTimeEstimationConfig();
		waitingTimeConfig.setEstimationLinkAttribute("avWaitingTimeGroup");
		waitingTimeConfig.setEstimationStartTime(5.0 * 3600.0);
		waitingTimeConfig.setEstimationEndTime(24.0 * 3600.0);
		waitingTimeConfig.setEstimationInterval(15.0 * 60.0);
		waitingTimeConfig.setEstimationAlpha(0.1);
		waitingTimeConfig.setDefaultWaitingTime(10.0 * 60.0);

		TimingConfig timingConfig = operatorConfig.getTimingConfig();
		timingConfig.setMinimumPickupDurationPerStop(120.0);
		timingConfig.setMinimumDropoffDurationPerStop(60.0);
		timingConfig.setPickupDurationPerPassenger(0.0);
		timingConfig.setDropoffDurationPerPassenger(0.0);

		DispatcherConfig dispatcherConfig = operatorConfig.getDispatcherConfig();
		dispatcherConfig.setType(SingleHeuristicDispatcher.TYPE);
		dispatcherConfig.addParam("replanningInterval", String.valueOf(astraConfig.getDispatchInterval()));
	}

	static public void adjustNetwork(Scenario scenario) {
		Network network = scenario.getNetwork();
		ServiceArea serviceArea = new ServiceAreaModule().provideServiceArea(scenario.getConfig(),
				AstraConfigGroup.get(scenario.getConfig()), network);

		for (Link link : network.getLinks().values()) {
			if (serviceArea.covers(link)) {
				Set<String> modes = new HashSet<>(link.getAllowedModes());

				if (modes.contains("car")) {
					modes.add("av");
					link.setAllowedModes(modes);
				}
			}
		}
	}

	static public void adjustScenario(Scenario scenario) {
		for (Household household : scenario.getHouseholds().getHouseholds().values()) {
			for (Id<Person> memberId : household.getMemberIds()) {
				Person person = scenario.getPopulation().getPersons().get(memberId);

				if (person != null) {
					person.getAttributes().putAttribute("householdIncome", household.getIncome().getIncome());
				}
			}
		}

		AvConfigurator.configureUniformWaitingTimeGroup(scenario);
		adjustBikeAvailability(scenario);
	}

	static private void adjustBikeAvailability(Scenario scenario) {
		Random random = new Random(scenario.getConfig().global().getRandomSeed());
		AstraConfigGroup astraConfig = AstraConfigGroup.get(scenario.getConfig());

		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (!person.getId().toString().contains("freight")) {
				if (!person.getAttributes().getAttribute("bikeAvailability").equals("FOR_NONE")) {
					if (random.nextDouble() > astraConfig.getBikeAvailability()) {
						person.getAttributes().putAttribute("bikeAvailability", "FOR_NONE");
					}
				}
			}
		}
	}

	static public void configureController(Controler controller, CommandLine commandLine) {
		AvConfigurator.configureController(controller, commandLine);
		controller.addOverridingModule(new AstraAvModule(commandLine));
		controller.addOverridingModule(new ServiceAreaModule());
		controller.addOverridingModule(new WaitingTimeModule());
		controller.addOverridingModule(new PricingModule());

		controller.configureQSimComponents(configurator -> {
			EqasimTransitQSimModule.configure(configurator, controller.getConfig());
			AmodeusQSimModule.activateModes(controller.getConfig()).configure(configurator);
		});
	}
}
