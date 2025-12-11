package org.eqasim.switzerland.ch_cmdp.scenario;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.switzerland.ch_cmdp.SwitzerlandConfigurator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.RoutingConfigGroup.TeleportedModeParams;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.config.groups.VehiclesConfigGroup;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.pt.config.TransitRouterConfigGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class RunAdaptConfig {

	private static double storageCapExponent = 0.75;

	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args).allowAnyOption(true).build();
		SwissConfigAdapter.run(args, new SwitzerlandConfigurator(cmd), RunAdaptConfig::adaptConfiguration);
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

		// Cross border agents should not replan their mode
		StrategySettings crossborderStrategy = new StrategySettings();
		crossborderStrategy.setStrategyName(DefaultStrategy.ReRoute);
		crossborderStrategy.setWeight(SwissConfigAdapter.replanningRate);
		crossborderStrategy.setSubpopulation("crossborder");
		replanningConfigGroup.addStrategySettings(crossborderStrategy);

		StrategySettings crossBorderStrategy = new StrategySettings();
		crossBorderStrategy.setStrategyName(DefaultSelector.KeepLastSelected);
		crossBorderStrategy.setWeight(1.0 - SwissConfigAdapter.replanningRate);
		crossBorderStrategy.setSubpopulation("crossborder");
		replanningConfigGroup.addStrategySettings(crossBorderStrategy);

		// set the main mode in qsim to car and truck
		QSimConfigGroup qsimConfigGroup = config.qsim();
		qsimConfigGroup.setMainModes(Arrays.asList(TransportMode.car, "truck"));

		// downsampling adjustments
		if (SwissConfigAdapter.downsamplingRate < 1.0) {
			// adjust the flow and storage capacities based on
			// the work from T.W. Nicolai Using MATSim as a travel model plug-in to UrbanSim
			// VSP Working Paper (2012), pp. 12-29 TU Berlin, Transport Systems Planning
			qsimConfigGroup.setFlowCapFactor(SwissConfigAdapter.downsamplingRate);
			qsimConfigGroup.setStorageCapFactor(Math.pow(SwissConfigAdapter.downsamplingRate, RunAdaptConfig.storageCapExponent));
		}

		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		List<String> LOOP_MODES = new ArrayList<>(Arrays.asList("walk_loop", "pt_loop", "bike_loop", "car_loop", "car_passenger_loop"));

		// set mode choice model estimators and cost models
		eqasimConfig.setEstimator(TransportMode.car, SwissModeChoiceModule.CAR_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(TransportMode.bike, SwissModeChoiceModule.BIKE_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(TransportMode.pt, SwissModeChoiceModule.PT_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(TransportMode.walk, SwissModeChoiceModule.WALK_ESTIMATOR_NAME);
		eqasimConfig.setEstimator("car_passenger", SwissModeChoiceModule.CP_ESTIMATOR_NAME);

		eqasimConfig.setCostModel(TransportMode.car, SwissModeChoiceModule.CAR_COST_MODEL_NAME);
		eqasimConfig.setCostModel(TransportMode.pt, SwissModeChoiceModule.PT_COST_MODEL_NAME);
		// also adding loop modes that should not be considered for mode choice
		for (String mode : LOOP_MODES) {
			eqasimConfig.setEstimator(mode, EqasimModeChoiceModule.ZERO_ESTIMATOR_NAME);
		}
		for (String mode : LOOP_MODES) {
			eqasimConfig.setCostModel(mode, EqasimModeChoiceModule.ZERO_COST_MODEL_NAME);
		}

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		dmcConfig.setModeAvailability(SwissModeChoiceModule.MODE_AVAILABILITY_NAME);
		Collection<String> cachedModes = dmcConfig.getCachedModes();
		for (String mode : LOOP_MODES) {
			cachedModes.add(mode);
		}
		dmcConfig.setCachedModes(cachedModes);

		ScoringConfigGroup scoringConfig1 = config.scoring();
		RoutingConfigGroup routingConfig  = config.routing();
		// adjust routing parameters
		RoutingConfigGroup.TeleportedModeParams bikeParams = routingConfig.getOrCreateModeRoutingParams(TransportMode.bike);
		bikeParams.setBeelineDistanceFactor(1.4);
		bikeParams.setTeleportedModeSpeed(4.0);

		RoutingConfigGroup.TeleportedModeParams walkParams = routingConfig.getOrCreateModeRoutingParams(TransportMode.walk);
		walkParams.setBeelineDistanceFactor(1.3);
		walkParams.setTeleportedModeSpeed(1.3);

		//loop modes
		for (String mode : LOOP_MODES) {
			ModeParams modeParams = scoringConfig1.getOrCreateModeParams(mode);

			modeParams.setConstant(0.0);
			modeParams.setMarginalUtilityOfDistance(0.0);
			modeParams.setMarginalUtilityOfTraveling(-1.0);
			modeParams.setMonetaryDistanceRate(0.0);

			TeleportedModeParams modeParams2 = routingConfig.getOrCreateModeRoutingParams(mode);
			modeParams2.setBeelineDistanceFactor(1.0);

			if (mode.equals("walk_loop")) {
				modeParams2.setTeleportedModeSpeed(1.2);
			} else if (mode.equals("bike_loop")){
				modeParams2.setTeleportedModeSpeed(3.1);
			} else if (mode.equals("car_loop")){
				modeParams2.setTeleportedModeSpeed(1000.0);
			} else {
				modeParams2.setTeleportedModeSpeed(1000.0);
			}
		}

		// set trip constraints (to remove car passenger constraint)
		dmcConfig.setTripConstraints(Arrays.asList("OutsideConstraint", "TransitWalk", "LoopModesConstraint"));

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

		// Vehicles
		QSimConfigGroup qsimConfig = config.qsim();
		qsimConfig.setVehiclesSource(VehiclesSource.fromVehiclesData);
		VehiclesConfigGroup vehiclesConfig = config.vehicles();
		vehiclesConfig.setVehiclesFile(SwissConfigAdapter.prefix + "vehicles.xml.gz");

		// transit router
		TransitRouterConfigGroup transitRouterParams = config.transitRouter();
		transitRouterParams.setDirectWalkFactor(3.0);

	}

}
