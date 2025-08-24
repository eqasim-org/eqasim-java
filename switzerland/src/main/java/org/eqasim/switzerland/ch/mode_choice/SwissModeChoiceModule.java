package org.eqasim.switzerland.ch.mode_choice;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.fast_calibration.AlphaCalibratorConfig;
import org.eqasim.core.components.fast_calibration.FastCalibration;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.switzerland.ch.calibration.AlphaCantonCalibrator;
import org.eqasim.switzerland.ch.mode_choice.costs.SwissCarCostModel;
import org.eqasim.switzerland.ch.mode_choice.costs.SwissPtCostModel;
import org.eqasim.switzerland.ch.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.ch.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.ch.mode_choice.utilities.estimators.SwissBikeUtilityEstimator;
import org.eqasim.switzerland.ch.mode_choice.utilities.estimators.SwissCarUtilityEstimator;
import org.eqasim.switzerland.ch.mode_choice.utilities.estimators.SwissPtUtilityEstimator;
import org.eqasim.switzerland.ch.mode_choice.utilities.estimators.SwissWalkUtilityEstimator;
import org.eqasim.switzerland.ch.mode_choice.utilities.estimators.SwissZeroUtilityEstimator;
import org.eqasim.switzerland.ch.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.core.controler.OutputDirectoryHierarchy;

public class SwissModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	static public final String MODE_AVAILABILITY_NAME = "SwissModeAvailability";
	static public final String CAR_ESTIMATOR_NAME = "SwissCarEstimator";
	static public final String BIKE_ESTIMATOR_NAME = "SwissBikeEstimator";
	static public final String PT_ESTIMATOR_NAME   = "SwissPtEstimator";
	static public final String WALK_ESTIMATOR_NAME = "SwissWalkEstimator";
	static public final String ZERO_ESTIMATOR_NAME = "SwissZeroUtilityEstimator";
	static public final String CAR_COST_MODEL_NAME = "SwissCarCostModel";
	static public final String PT_COST_MODEL_NAME = "SwissPtCostModel";

	public SwissModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(SwissModeAvailability.class);

		bindUtilityEstimator(CAR_ESTIMATOR_NAME).to(SwissCarUtilityEstimator.class);
		bindUtilityEstimator(BIKE_ESTIMATOR_NAME).to(SwissBikeUtilityEstimator.class);
		bindUtilityEstimator(PT_ESTIMATOR_NAME).to(SwissPtUtilityEstimator.class);
		bindUtilityEstimator(WALK_ESTIMATOR_NAME).to(SwissWalkUtilityEstimator.class);
		bindUtilityEstimator(ZERO_ESTIMATOR_NAME).to(SwissZeroUtilityEstimator.class);

		bindCostModel(CAR_COST_MODEL_NAME).to(SwissCarCostModel.class);
		bindCostModel(PT_COST_MODEL_NAME).to(SwissPtCostModel.class);

		bind(SwissPersonPredictor.class);

		bind(ModeParameters.class).to(SwissModeParameters.class).asEagerSingleton();
		bind(FastCalibration.class).to(AlphaCantonCalibrator.class).asEagerSingleton();
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
	public SwissCostParameters provideCostParameters(EqasimConfigGroup config) {
		SwissCostParameters parameters = SwissCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public AlphaCantonCalibrator provideAlphaCantonCalibrator(Scenario scenario,
															  OutputDirectoryHierarchy outputHierarchy,
															  SwissModeParameters modeParameters,
															  TripListConverter tripListConverter,
															  AlphaCalibratorConfig calConfig) {
		double beta = calConfig.getBeta();
		Map<String, Double> targetModeShares = Map.of(
				"car", calConfig.getCarModeShare(),
				"pt", calConfig.getPtModeShare(),
				"walk", calConfig.getWalkModeShare(),
				"bike", calConfig.getBikeModeShare(),
				"car_passenger", calConfig.getCarPassengerModeShare()
		);
		return new AlphaCantonCalibrator(scenario,outputHierarchy,modeParameters,tripListConverter,targetModeShares,beta);
	}
}
