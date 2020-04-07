package org.eqasim.projects.astra16;

import java.util.HashSet;
import java.util.Set;

import org.eqasim.automated_vehicles.components.AvConfigurator;
import org.eqasim.automated_vehicles.components.EqasimAvConfigGroup;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.calibration.CalibrationConfigGroup;
import org.eqasim.projects.astra16.mode_choice.AstraModeAvailability;
import org.eqasim.projects.astra16.mode_choice.InfiniteHeadwayConstraint;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraAvUtilityEstimator;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraBikeUtilityEstimator;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraCarUtilityEstimator;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraPtUtilityEstimator;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraWalkUtilityEstimator;
import org.eqasim.projects.astra16.pricing.PricingModule;
import org.eqasim.projects.astra16.pricing.model.AstraAvCostModel;
import org.eqasim.projects.astra16.service_area.ServiceAreaModule;
import org.eqasim.projects.astra16.waiting_time.WaitingTimeModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.households.Household;

import ch.ethz.matsim.av.config.AVConfigGroup;
import ch.ethz.matsim.av.config.operator.OperatorConfig;
import ch.ethz.matsim.av.config.operator.WaitingTimeConfig;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.framework.AVQSimModule;
import ch.ethz.matsim.discrete_mode_choice.modules.DiscreteModeChoiceModule;
import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
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
		config.controler().setLastIteration(100);
		config.controler().setWriteEventsInterval(100);

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
		dmcConfig.setTripConstraints(tripConstraints);

		dmcConfig.setModeAvailability(AstraModeAvailability.NAME);

		// Add default AV configuration
		AvConfigurator.configure(config);
		eqasimConfig.setEstimator(AVModule.AV_MODE, AstraAvUtilityEstimator.NAME);
		eqasimConfig.setCostModel(AVModule.AV_MODE, AstraAvCostModel.NAME);

		// Set up AV
		AVConfigGroup avConfig = AVConfigGroup.getOrCreate(config);
		avConfig.setUseAccessAgress(true);
		avConfig.setAllowedLinkMode("car"); // And later we also filter for operating area

		avConfig.setVehicleAnalysisInterval(config.controler().getWriteEventsInterval());
		avConfig.setPassengerAnalysisInterval(config.controler().getWriteEventsInterval());
	}

	static public void adjustOperator(Config config) {
		// Here we assume that all other config stuff has been handled before
		AstraConfigGroup astraConfig = AstraConfigGroup.get(config);

		OperatorConfig operatorConfig = AVConfigGroup.getOrCreate(config)
				.getOperatorConfig(OperatorConfig.DEFAULT_OPERATOR_ID);
		operatorConfig.getGeneratorConfig().setNumberOfVehicles(astraConfig.getFleetSize());

		operatorConfig.setCleanNetwork(true);

		WaitingTimeConfig waitingTimeConfig = operatorConfig.getWaitingTimeConfig();
		waitingTimeConfig.setEstimationStartTime(5.0 * 3600.0);
		waitingTimeConfig.setEstimationEndTime(24.0 * 3600.0);
		waitingTimeConfig.setEstimationInterval(15.0 * 60.0);
		waitingTimeConfig.setEstimationAlpha(0.1);
		waitingTimeConfig.setDefaultWaitingTime(10.0 * 60.0);
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
	}

	static public void configureController(Controler controller, CommandLine commandLine) {
		AvConfigurator.configureController(controller, commandLine);
		controller.addOverridingModule(new AstraAvModule(commandLine));
		controller.addOverridingModule(new ServiceAreaModule());
		controller.addOverridingModule(new WaitingTimeModule());
		controller.addOverridingModule(new PricingModule());

		controller.configureQSimComponents(configurator -> {
			EqasimTransitQSimModule.configure(configurator);
			AVQSimModule.configureComponents(configurator);
		});
	}
}
