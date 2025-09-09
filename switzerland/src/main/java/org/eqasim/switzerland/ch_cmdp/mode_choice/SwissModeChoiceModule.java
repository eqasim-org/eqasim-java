package org.eqasim.switzerland.ch_cmdp.mode_choice;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.fast_calibration.AlphaCalibratorConfig;
import org.eqasim.core.components.fast_calibration.FastCalibration;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.switzerland.ch.calibration.AlphaCantonCalibrator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.SwissCarCostModel;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.SwissParkingCostModel;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.SwissPtDetailedCostModel;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCmdpModeParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators.*;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.mode_availability.SwissDetailedModeAvailability;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.CarPassengerPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class SwissModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	static public final String CAR_COST_MODEL_NAME = "SwissCarCostModel";
	static public final String PT_COST_MODEL_NAME = "SwissDetailedPtCostModel";

	static public final String MODE_AVAILABILITY_NAME = "SwissDetailedModeAvailability";
	static public final String CAR_ESTIMATOR_NAME = "SwissDetailedCarEstimator";
	static public final String BIKE_ESTIMATOR_NAME = "SwissDetailedBikeEstimator";
	static public final String PT_ESTIMATOR_NAME   = "SwissDetailedPtEstimator";
	static public final String WALK_ESTIMATOR_NAME = "SwissDetailedWalkEstimator";
	static public final String CP_ESTIMATOR_NAME = "SwissDetailedCpEstimator";

	public SwissModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {

		bindCostModel(CAR_COST_MODEL_NAME).to(SwissCarCostModel.class);
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(SwissDetailedModeAvailability.class);
		bindUtilityEstimator(CAR_ESTIMATOR_NAME).to(SwissCarDetailedUtilityEstimator.class);
		bindUtilityEstimator(BIKE_ESTIMATOR_NAME).to(SwissBikeDetailedUtilityEstimator.class);
		bindUtilityEstimator(PT_ESTIMATOR_NAME).to(SwissPtDetailedUtilityEstimator.class);
		bindUtilityEstimator(WALK_ESTIMATOR_NAME).to(SwissWalkDetailedUtilityEstimator.class);
		bindUtilityEstimator(CP_ESTIMATOR_NAME).to(SwissCarPassengerDetailedUtilityEstimator.class);

		bindCostModel(PT_COST_MODEL_NAME).to(SwissPtDetailedCostModel.class);

		bind(SwissPersonPredictor.class);
		bind(CarPassengerPredictor.class);
		bind(ModeParameters.class).to(SwissCmdpModeParameters.class).asEagerSingleton();

		// Calibration
		AlphaCalibratorConfig calConfig = AlphaCalibratorConfig.getOrCreate(getConfig());
		if (calConfig.isActivate() && calConfig.getLevel().equalsIgnoreCase("canton")) {
			bind(FastCalibration.class).to(AlphaCantonCalibrator.class).asEagerSingleton();
		}
	}

	@Provides
	@Singleton
	public SwissCmdpModeParameters provideSwissCmdpModeParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		SwissCmdpModeParameters parameters = SwissCmdpModeParameters.buildDefault();

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
	public SwissParkingCostModel provideSwissParkingCostModel(SwissCostParameters parameters) {
		return new SwissParkingCostModel(parameters);
	}

	@Provides
	@Singleton
	public AlphaCantonCalibrator provideAlphaCantonCalibrator(Scenario scenario,
															  OutputDirectoryHierarchy outputHierarchy,
															  SwissCmdpModeParameters modeParameters,
															  TripListConverter tripListConverter) {
		AlphaCalibratorConfig calConfig = AlphaCalibratorConfig.getOrCreate(getConfig());
		double beta = calConfig.getBeta();
		Map<String, Double> targetModeShares = Map.of(
				"car", calConfig.getCarModeShare(),
				"pt", calConfig.getPtModeShare(),
				"walk", calConfig.getWalkModeShare(),
				"bike", calConfig.getBikeModeShare(),
				"car_passenger", calConfig.getCarPassengerModeShare()
		);
		String filePath = calConfig.getFilePath();
		if (filePath.isEmpty()) {
			throw new IllegalArgumentException("You must provide the file path to the cantons mode share csv file when using canton level calibration.");
		}
		return new AlphaCantonCalibrator(scenario,outputHierarchy,modeParameters,
				tripListConverter,targetModeShares,beta, filePath, calConfig.isActivate());
	}
}
