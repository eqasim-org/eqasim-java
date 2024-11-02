package org.eqasim.switzerland.scenario;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.switzerland.SwitzerlandConfigurator;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;

public class RunAdaptConfig {

	private static double storageCapExponent = 0.75;

	static public void main(String[] args) throws ConfigurationException {
		SwitzerlandConfigurator configurator = new SwitzerlandConfigurator();
		SwissConfigAdapter.run(args, configurator, RunAdaptConfig::adaptConfiguration);
	}

	static public void adaptConfiguration(Config config) {

		ReplanningConfigGroup replanningConfigGroup = config.replanning();
		if (SwissConfigAdapter.hasFreight) {
			StrategySettings freightStrategy = new StrategySettings();
			freightStrategy.setStrategyName(DefaultStrategy.ReRoute);
			freightStrategy.setWeight(SwissConfigAdapter.replanningRate);
			freightStrategy.setSubpopulation("freight");
			replanningConfigGroup.addStrategySettings(freightStrategy);

			StrategySettings selectorStrategy = new StrategySettings();
			selectorStrategy.setStrategyName(DefaultSelector.KeepLastSelected);
			selectorStrategy.setWeight(1.0 - SwissConfigAdapter.replanningRate);
			selectorStrategy.setSubpopulation("freight");
			replanningConfigGroup.addStrategySettings(selectorStrategy);
		}

		if (SwissConfigAdapter.downsamplingRate < 1.0) {
			// adjust the flow and storage capacities based on
			// the work from T.W. Nicolai Using MATSim as a travel model plug-in to UrbanSim
			// VSP Working Paper (2012), pp. 12-29 TU Berlin, Transport Systems Planning

			QSimConfigGroup qsimConfigGroup = config.qsim();
			qsimConfigGroup.setFlowCapFactor(SwissConfigAdapter.downsamplingRate);
			qsimConfigGroup.setStorageCapFactor(Math.pow(SwissConfigAdapter.downsamplingRate, RunAdaptConfig.storageCapExponent));

		}
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		eqasimConfig.setEstimator(TransportMode.car, SwissModeChoiceModule.CAR_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(TransportMode.bike, SwissModeChoiceModule.BIKE_ESTIMATOR_NAME);

		eqasimConfig.setCostModel(TransportMode.car, SwissModeChoiceModule.CAR_COST_MODEL_NAME);
		eqasimConfig.setCostModel(TransportMode.pt, SwissModeChoiceModule.PT_COST_MODEL_NAME);

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		dmcConfig.setModeAvailability(SwissModeChoiceModule.MODE_AVAILABILITY_NAME);

		// adapting Scoring config with custom activities
		if (SwissConfigAdapter.hasCustomActivities) {
			ScoringConfigGroup scoringConfig = config.scoring();

			for (String activityType : SwissConfigAdapter.activityTypes) {
				ScoringConfigGroup.ActivityParams activityParams = scoringConfig.getActivityParams(activityType);

				if (activityParams == null) {
					activityParams = new ScoringConfigGroup.ActivityParams(activityType);
					config.scoring().addActivityParams(activityParams);
				}

				activityParams.setScoringThisActivityAtAll(false);
			}
		}

	}

}
