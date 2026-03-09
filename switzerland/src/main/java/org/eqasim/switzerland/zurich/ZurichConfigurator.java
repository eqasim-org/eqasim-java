package org.eqasim.switzerland.zurich;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.fast_calibration.AlphaCalibratorConfig;
import org.eqasim.core.components.raptor.EqasimRaptorConfigGroup;
import org.eqasim.core.components.traffic_light.DelaysConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.termination.EqasimTerminationConfigGroup;
import org.eqasim.switzerland.ch.mode_choice.SwissModeChoiceModule;
import org.eqasim.switzerland.zurich.mode_choice.ZurichModeAvailability;
import org.eqasim.switzerland.zurich.mode_choice.ZurichModeChoiceModule;
import org.eqasim.switzerland.zurich.mode_choice.constraints.InfiniteHeadwayConstraint;
import org.eqasim.switzerland.zurich.mode_choice.utilities.estimators.ZurichBikeUtilityEstimator;
import org.eqasim.switzerland.zurich.mode_choice.utilities.estimators.ZurichCarUtilityEstimator;
import org.eqasim.switzerland.zurich.mode_choice.utilities.estimators.ZurichPtUtilityEstimator;
import org.eqasim.switzerland.zurich.mode_choice.utilities.estimators.ZurichWalkUtilityEstimator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.households.Household;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;

public class ZurichConfigurator extends EqasimConfigurator {
	public static double bikeAvailability = 0.55;
	public static double travelTimeEstimationAlpha = 0.1;
	public ZurichConfigurator(CommandLine cmd) {
		super(cmd);
		registerModule(new SwissModeChoiceModule(cmd));
		registerModule(new ZurichModeChoiceModule(cmd));
		//registerModule(new SmoothingTravelTimeModule());
	}

	public ConfigGroup[] getConfigGroups() {
		return new ConfigGroup[] { //
				new SwissRailRaptorConfigGroup(), //
				new EqasimConfigGroup(), //
				new DiscreteModeChoiceConfigGroup(), //
				new EqasimTerminationConfigGroup(),
				new EqasimRaptorConfigGroup(),
				new AlphaCalibratorConfig(),
				new DelaysConfigGroup()};
	}

	public void configure(Config config) {
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		config.qsim().setNumberOfThreads(Math.min(12, Runtime.getRuntime().availableProcessors()));
		config.global().setNumberOfThreads(Runtime.getRuntime().availableProcessors());
		
		config.transitRouter().setDirectWalkFactor(5.0);

		for (StrategySettings strategy : config.replanning().getStrategySettings()) {
			if (strategy.getStrategyName().equals(DiscreteModeChoiceModule.STRATEGY_NAME)) {
				strategy.setWeight(0.05);
			} else {
				strategy.setWeight(0.95);
			}
		}

		// General eqasim
		eqasimConfig.setAnalysisInterval(config.controller().getWriteEventsInterval());

		// Estimators
		eqasimConfig.setEstimator(TransportMode.car, ZurichCarUtilityEstimator.NAME);
		eqasimConfig.setEstimator(TransportMode.pt, ZurichPtUtilityEstimator.NAME);
		eqasimConfig.setEstimator(TransportMode.bike, ZurichBikeUtilityEstimator.NAME);
		eqasimConfig.setEstimator(TransportMode.walk, ZurichWalkUtilityEstimator.NAME);

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		Set<String> tripConstraints = new HashSet<>(dmcConfig.getTripConstraints());
		tripConstraints.add(InfiniteHeadwayConstraint.NAME);
		dmcConfig.setTripConstraints(tripConstraints);

		dmcConfig.setModeAvailability(ZurichModeAvailability.NAME);
	}

	public void adjustScenario(Scenario scenario) {
		for (Household household : scenario.getHouseholds().getHouseholds().values()) {
			for (Id<Person> memberId : household.getMemberIds()) {
				Person person = scenario.getPopulation().getPersons().get(memberId);

				if (person != null) {
					person.getAttributes().putAttribute("householdIncome", household.getIncome().getIncome());
					copyAttribute(household, person, "spRegion");
					copyAttribute(household, person, "bikeAvailability");
				}
			}
		}
		adjustBikeAvailability(scenario);
	}

	@SuppressWarnings("null")
	static private void adjustBikeAvailability(Scenario scenario) {
		Random random = new Random(scenario.getConfig().global().getRandomSeed());

		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (!person.getId().toString().contains("freight")) {
				if (!person.getAttributes().getAttribute("bikeAvailability").equals("FOR_NONE")) {
					if (random.nextDouble() > ZurichConfigurator.bikeAvailability) {
						person.getAttributes().putAttribute("bikeAvailability", "FOR_NONE");
					}
				}
			}
		}
	}
}