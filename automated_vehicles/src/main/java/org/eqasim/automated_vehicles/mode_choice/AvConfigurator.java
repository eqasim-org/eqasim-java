package org.eqasim.automated_vehicles.mode_choice;

import java.util.HashSet;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;

import ch.ethz.matsim.av.config.AVConfigGroup;
import ch.ethz.matsim.av.config.AVScoringParameterSet;
import ch.ethz.matsim.av.config.operator.DispatcherConfig;
import ch.ethz.matsim.av.config.operator.GeneratorConfig;
import ch.ethz.matsim.av.config.operator.OperatorConfig;
import ch.ethz.matsim.av.config.operator.WaitingTimeConfig;
import ch.ethz.matsim.av.dispatcher.single_heuristic.SingleHeuristicDispatcher;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.generator.PopulationDensityGenerator;
import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;

public final class AvConfigurator {
	private AvConfigurator() {
	}

	static public void configure(Config config) {
		// Set up DVRP
		if (!config.getModules().containsKey(DvrpConfigGroup.GROUP_NAME)) {
			config.addModule(new DvrpConfigGroup());
		}

		// Set up AV extension
		if (!config.getModules().containsKey(AVConfigGroup.GROUP_NAME)) {
			AVConfigGroup avConfig = AVConfigGroup.getOrCreate(config);
			avConfig.setAllowedLinkMode("av");

			// Set up operator
			OperatorConfig operatorConfig = new OperatorConfig();
			avConfig.addOperator(operatorConfig);
			operatorConfig.setPredictRouteTravelTime(true); // Important for prediction!

			DispatcherConfig dispatcherConfig = operatorConfig.getDispatcherConfig();
			dispatcherConfig.setType(SingleHeuristicDispatcher.TYPE);

			GeneratorConfig generatorConfig = operatorConfig.getGeneratorConfig();
			generatorConfig.setType(PopulationDensityGenerator.TYPE);
			generatorConfig.setNumberOfVehicles(10);

			WaitingTimeConfig waitingTimeConfig = operatorConfig.getWaitingTimeConfig();
			waitingTimeConfig.setEstimationAlpha(0.1);

			// TODO: This should be fixed in AV extension (when it is null, we get an
			// exception)
			waitingTimeConfig.setConstantWaitingTimeLinkAttribute("constantWaitingTime");

			// Set up scoring (although we don't really use it - MATSim wants it)
			// TODO: Could also be added by default in AV extension
			AVScoringParameterSet scoringParameters = new AVScoringParameterSet();
			avConfig.addScoringParameters(scoringParameters);
		}

		// Set up DiscreteModeChoice
		DiscreteModeChoiceConfigGroup dmcConfig = getOrCreateDiscreteModeChoiceConfigGroup(config);

		// -- Constraint that makes sure we don't have "fallback" trips for AV that only
		// consist of walk
		Set<String> tripConstraints = new HashSet<>();
		tripConstraints.addAll(dmcConfig.getTripConstraints());
		tripConstraints.add(AvModeChoiceModule.AV_WALK_CONSTRAINT_NAME);
		dmcConfig.setTripConstraints(tripConstraints);

		// -- Add AV to the cached modes
		Set<String> cachedModes = new HashSet<>();
		cachedModes.addAll(dmcConfig.getCachedModes());
		cachedModes.add("av");
		dmcConfig.setCachedModes(cachedModes);

		// Set up MATSim scoring (although we don't really use it - MATSim wants it)
		ModeParams modeParams = new ModeParams(AVModule.AV_MODE);
		config.planCalcScore().addModeParams(modeParams);

		// Set up Eqasim (add AV cost model and estimator)
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
		eqasimConfig.setCostModel("av", AvModeChoiceModule.AV_COST_MODEL_NAME);
		eqasimConfig.setEstimator("av", AvModeChoiceModule.AV_ESTIMATOR_NAME);
	}

	// TODO: This will be included in the next version of DMC
	static public DiscreteModeChoiceConfigGroup getOrCreateDiscreteModeChoiceConfigGroup(Config config) {
		DiscreteModeChoiceConfigGroup configGroup = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		if (configGroup == null) {
			configGroup = new DiscreteModeChoiceConfigGroup();
			config.addModule(configGroup);
		}

		return configGroup;
	}

	static public void configureCarLinks(Scenario scenario) {
		for (Link link : scenario.getNetwork().getLinks().values()) {
			Set<String> allowedModes = new HashSet<>(link.getAllowedModes());

			if (allowedModes.contains("car")) {
				allowedModes.add("av");
			}

			link.setAllowedModes(allowedModes);
		}
	}

	static public void configureUniformWaitingTimeGroup(Scenario scenario) {
		for (Link link : scenario.getNetwork().getLinks().values()) {
			link.getAttributes().putAttribute("avWaitingTimeGroup", 0);
		}
	}

	static public void configureController(Controler controller, CommandLine cmd) {
		controller.addOverridingModule(new AVModule());
		controller.addOverridingModule(new DvrpModule());
		controller.addOverridingModule(new AvModeChoiceModule(cmd));
	}
}
