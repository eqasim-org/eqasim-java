package org.eqasim.switzerland.scenario;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.switzerland.SwitzerlandConfigurator;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;

public class RunAdaptConfig {

	static public void main(String[] args) throws ConfigurationException {
		SwitzerlandConfigurator configurator = new SwitzerlandConfigurator();
		SwissConfigAdapter.run(args, configurator.getConfigGroups(), RunAdaptConfig::adaptConfiguration);
	}

	static public void adaptConfiguration(Config config) {
		// add replanning strategies for freight agents
		StrategyConfigGroup.StrategySettings freightReRouteStrategy = new StrategyConfigGroup.StrategySettings();
		freightReRouteStrategy.setDisableAfter(-1);
		freightReRouteStrategy.setStrategyName("ReRoute");
		freightReRouteStrategy.setSubpopulation("freight");
		freightReRouteStrategy.setWeight(0.05);
		config.strategy().addStrategySettings(freightReRouteStrategy);

		StrategyConfigGroup.StrategySettings freightKeepLastSelectedStrategy = new StrategyConfigGroup.StrategySettings();
		freightKeepLastSelectedStrategy.setDisableAfter(-1);
		freightKeepLastSelectedStrategy.setStrategyName("KeepLastSelected");
		freightKeepLastSelectedStrategy.setSubpopulation("freight");
		freightKeepLastSelectedStrategy.setWeight(0.95);
		config.strategy().addStrategySettings(freightKeepLastSelectedStrategy);

		// adapt eqasim config
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		eqasimConfig.setEstimator(TransportMode.car, SwissModeChoiceModule.CAR_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(TransportMode.bike, SwissModeChoiceModule.BIKE_ESTIMATOR_NAME);

		eqasimConfig.setCostModel(TransportMode.car, SwissModeChoiceModule.CAR_COST_MODEL_NAME);
		eqasimConfig.setCostModel(TransportMode.pt, SwissModeChoiceModule.PT_COST_MODEL_NAME);

		// adapt discrete mode choice config
		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		dmcConfig.setModeAvailability(SwissModeChoiceModule.MODE_AVAILABILITY_NAME);

		// adapting Scoring config with custom activities
		if (SwissConfigAdapter.hasCustomActivities) {
			PlanCalcScoreConfigGroup scoringConfig = config.planCalcScore();

			for (String activityType : SwissConfigAdapter.activityTypes) {
				PlanCalcScoreConfigGroup.ActivityParams activityParams = scoringConfig.getActivityParams(activityType);

				if (activityParams == null) {
					activityParams = new PlanCalcScoreConfigGroup.ActivityParams(activityType);
					config.planCalcScore().addActivityParams(activityParams);
				}

				activityParams.setScoringThisActivityAtAll(false);
			}
		}

	}

}
