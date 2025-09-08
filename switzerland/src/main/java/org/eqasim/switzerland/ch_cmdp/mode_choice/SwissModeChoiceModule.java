package org.eqasim.switzerland.ch_cmdp.mode_choice;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.SwissCarCostModel;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.SwissPtCostModel;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.SwissPtDetailedCostModel;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissModeDetailedParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.detailed_estimators.*;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators.*;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.mode_availability.SwissDetailedModeAvailability;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.mode_availability.SwissModeAvailability;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.CarPassengerPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import java.io.File;
import java.io.IOException;

public class SwissModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;
	// default DMC model
	static public final String MODE_AVAILABILITY_NAME = "SwissModeAvailability";
	static public final String CAR_ESTIMATOR_NAME = "SwissCarEstimator";
	static public final String BIKE_ESTIMATOR_NAME = "SwissBikeEstimator";
	static public final String PT_ESTIMATOR_NAME   = "SwissPtEstimator";
	static public final String WALK_ESTIMATOR_NAME = "SwissWalkEstimator";
	static public final String ZERO_ESTIMATOR_NAME = "SwissZeroUtilityEstimator";
	static public final String CAR_COST_MODEL_NAME = "SwissCarCostModel";
	static public final String PT_COST_MODEL_NAME = "SwissPtCostModel";
	// detailed DMC model
	static public final String DETAILED_MODE_AVAILABILITY_NAME = "SwissDetailedModeAvailability";
	static public final String DETAILED_CAR_ESTIMATOR_NAME = "SwissDetailedCarEstimator";
	static public final String DETAILED_BIKE_ESTIMATOR_NAME = "SwissDetailedBikeEstimator";
	static public final String DETAILED_PT_ESTIMATOR_NAME   = "SwissDetailedPtEstimator";
	static public final String DETAILED_WALK_ESTIMATOR_NAME = "SwissDetailedWalkEstimator";
	static public final String DETAILED_CP_ESTIMATOR_NAME = "SwissDetailedCpEstimator";
	static public final String DETAILED_PT_COST_MODEL_NAME = "SwissDetailedPtCostModel";

	public SwissModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		// default DMC model
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(SwissModeAvailability.class);

		bindUtilityEstimator(CAR_ESTIMATOR_NAME).to(SwissCarUtilityEstimator.class);
		bindUtilityEstimator(BIKE_ESTIMATOR_NAME).to(SwissBikeUtilityEstimator.class);
		bindUtilityEstimator(PT_ESTIMATOR_NAME).to(SwissPtUtilityEstimator.class);
		bindUtilityEstimator(WALK_ESTIMATOR_NAME).to(SwissWalkUtilityEstimator.class);
		bindUtilityEstimator(ZERO_ESTIMATOR_NAME).to(SwissZeroUtilityEstimator.class);

		bindCostModel(CAR_COST_MODEL_NAME).to(SwissCarCostModel.class);
		bindCostModel(PT_COST_MODEL_NAME).to(SwissPtCostModel.class);

		// detailed DMC model
		bindModeAvailability(DETAILED_MODE_AVAILABILITY_NAME).to(SwissDetailedModeAvailability.class);

		bindUtilityEstimator(DETAILED_CAR_ESTIMATOR_NAME).to(SwissCarDetailedUtilityEstimator.class);
		bindUtilityEstimator(DETAILED_BIKE_ESTIMATOR_NAME).to(SwissBikeDetailedUtilityEstimator.class);
		bindUtilityEstimator(DETAILED_PT_ESTIMATOR_NAME).to(SwissPtDetailedUtilityEstimator.class);
		bindUtilityEstimator(DETAILED_WALK_ESTIMATOR_NAME).to(SwissWalkDetailedUtilityEstimator.class);
		bindUtilityEstimator(DETAILED_CP_ESTIMATOR_NAME).to(SwissCarPassengerDetailedUtilityEstimator.class);

		bindCostModel(DETAILED_PT_COST_MODEL_NAME).to(SwissPtDetailedCostModel.class);

		bind(SwissPersonPredictor.class);

		// bind(ModeParameters.class).to(SwissModeParameters.class).asEagerSingleton();
		bind(CarPassengerPredictor.class);
		bind(ModeParameters.class).to(SwissModeDetailedParameters.class).asEagerSingleton();
		// AlphaCalibratorConfig calConfig = AlphaCalibratorConfig.getOrCreate(getConfig());
		// if (calConfig.isActivate()) {
		// 	bind(FastCalibration.class).to(AlphaCantonCalibrator.class).asEagerSingleton();
		// }
	}

	@Provides
	@Singleton
	public SwissModeParameters provideSwissModeParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		SwissModeParameters parameters = SwissModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public SwissModeDetailedParameters provideSwissModeDetailedParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		SwissModeDetailedParameters parameters = SwissModeDetailedParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public SwissCostParameters provideCostParameters(EqasimConfigGroup config) {
		SwissCostParameters parameters = SwissCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}

//	@Provides
//	@Singleton
//	public AlphaCantonCalibrator provideAlphaCantonCalibrator(Scenario scenario,
//															  OutputDirectoryHierarchy outputHierarchy,
//															  SwissModeParameters modeParameters,
//															  TripListConverter tripListConverter,
//															  AlphaCalibratorConfig calConfig) {
//		double beta = calConfig.getBeta();
//		Map<String, Double> targetModeShares = Map.of(
//				"car", calConfig.getCarModeShare(),
//				"pt", calConfig.getPtModeShare(),
//				"walk", calConfig.getWalkModeShare(),
//				"bike", calConfig.getBikeModeShare(),
//				"car_passenger", calConfig.getCarPassengerModeShare()
//		);
//		return new AlphaCantonCalibrator(scenario,outputHierarchy,modeParameters,tripListConverter,targetModeShares,beta);
//	}
}
