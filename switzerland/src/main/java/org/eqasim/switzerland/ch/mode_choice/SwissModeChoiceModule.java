package org.eqasim.switzerland.ch.mode_choice;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.fast_calibration.AlphaCalibratorConfig;
import org.eqasim.core.components.fast_calibration.FastCalibration;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.switzerland.ch_cmdp.calibration.AlphaCantonCalibrator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.SwissPtPricingConfigGroup;
import org.eqasim.switzerland.ch_cmdp.mode_choice.constraints.BorderConstraint;
import org.eqasim.switzerland.ch_cmdp.mode_choice.constraints.LoopModesConstraint;
import org.eqasim.switzerland.ch.mode_choice.costs.SwissCarCostModel;
import org.eqasim.switzerland.ch.mode_choice.costs.SwissPtCostModel;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.SwissPtStageCostCalculator;
import org.eqasim.switzerland.ch.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.ch.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.ch.mode_choice.utilities.estimators.SwissBikeUtilityEstimator;
import org.eqasim.switzerland.ch.mode_choice.utilities.estimators.SwissCarUtilityEstimator;
import org.eqasim.switzerland.ch.mode_choice.utilities.estimators.SwissPtUtilityEstimator;
import org.eqasim.switzerland.ch.mode_choice.utilities.estimators.SwissWalkUtilityEstimator;
import org.eqasim.switzerland.ch.mode_choice.utilities.estimators.SwissZeroUtilityEstimator;
import org.eqasim.switzerland.ch.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPtRoutePredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.NetworkOfDistances;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.SwissPtPricingInputs;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.ZonalRegistry;
import org.eqasim.switzerland.ch_cmdp.mode_choice.constraints.RemoteWalkConstraint;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCmdpModeParameters;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.opencsv.exceptions.CsvValidationException;

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

	static public final String LOOP_CONSTRAINT_NAME = "LoopModesConstraint";
	static public final String REMOTE_WALK_CONSTRAINT_NAME = "RemoteWalkConstraint";
	static public final String BORDER_CONSTRAINT = "BorderConstraint";

	public SwissModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindTripConstraintFactory(LOOP_CONSTRAINT_NAME).to(LoopModesConstraint.Factory.class);
		bindTripConstraintFactory(REMOTE_WALK_CONSTRAINT_NAME).to(RemoteWalkConstraint.Factory.class);
		bindTripConstraintFactory(BORDER_CONSTRAINT).to(BorderConstraint.Factory.class);

		bindModeAvailability(MODE_AVAILABILITY_NAME).to(SwissModeAvailability.class);

		bindUtilityEstimator(CAR_ESTIMATOR_NAME).to(SwissCarUtilityEstimator.class);
		bindUtilityEstimator(BIKE_ESTIMATOR_NAME).to(SwissBikeUtilityEstimator.class);
		bindUtilityEstimator(PT_ESTIMATOR_NAME).to(SwissPtUtilityEstimator.class);
		bindUtilityEstimator(WALK_ESTIMATOR_NAME).to(SwissWalkUtilityEstimator.class);
		bindUtilityEstimator(ZERO_ESTIMATOR_NAME).to(SwissZeroUtilityEstimator.class);

		bindCostModel(CAR_COST_MODEL_NAME).to(SwissCarCostModel.class);
		bindCostModel(PT_COST_MODEL_NAME).to(SwissPtCostModel.class);

		bind(SwissPersonPredictor.class);
		bind(SwissPtRoutePredictor.class);

		bind(ModeParameters.class).to(SwissModeParameters.class).asEagerSingleton();

		// Calibration
		AlphaCalibratorConfig calConfig = AlphaCalibratorConfig.getOrCreate(getConfig());
		if (calConfig.isActivate() && calConfig.getLevel().equals("canton")) {
			bind(FastCalibration.class).to(AlphaCantonCalibrator.class).asEagerSingleton();
		}

	}

	@Provides
	@Singleton
	public SwissModeParameters provideSwissModeParameters(EqasimConfigGroup config, Config mainConfig)
			throws IOException, ConfigurationException {
		SwissModeParameters parameters = SwissModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			URL url = ConfigGroup.getInputFileURL(mainConfig.getContext(), config.getModeParametersPath());
			ParameterDefinition.applyFile(new File(url.getPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public SwissCostParameters provideCostParameters(EqasimConfigGroup config, Config mainConfig) {
		SwissCostParameters parameters = SwissCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			URL url = ConfigGroup.getInputFileURL(mainConfig.getContext(), config.getCostParametersPath());
			ParameterDefinition.applyFile(new File(url.getPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public AlphaCantonCalibrator provideAlphaCantonCalibrator(Scenario scenario,
															  OutputDirectoryHierarchy outputHierarchy,
															  SwissCmdpModeParameters modeParameters,
															  TripListConverter tripListConverter,
															  EqasimConfigGroup eqasimConfigGroup) {
		AlphaCalibratorConfig calConfig = AlphaCalibratorConfig.getOrCreate(getConfig());

		String filePath = calConfig.getResolvedFilePath(getConfig());
		if (filePath.isEmpty()) {
			throw new IllegalArgumentException("You must provide the file path to the cantons mode share csv file when using canton level calibration.");
		}
		Map<String, Double> targetModeShares = Map.of(
				"car", calConfig.getCarModeShare(),
				"pt", calConfig.getPtModeShare(),
				"walk", calConfig.getWalkModeShare(),
				"bike", calConfig.getBikeModeShare(),
				"car_passenger", calConfig.getCarPassengerModeShare()
		);

		return new AlphaCantonCalibrator(scenario,outputHierarchy,targetModeShares,modeParameters,
				tripListConverter, calConfig.getCalibratedModes() ,calConfig.getBeta(), filePath, calConfig.isActivate(),
				eqasimConfigGroup);
	}

	@Provides
	@Singleton
	public ZonalRegistry provideZonalRegistry(Config mainConfig) throws IOException, CsvValidationException {
		return SwissPtPricingInputs.readZonalRegistry(mainConfig, SwissPtPricingConfigGroup.getOrCreate(mainConfig));
	}

	@Provides
	@Singleton
	public NetworkOfDistances provideNetworkOfDistances(Config mainConfig) throws IOException, CsvValidationException {
		return SwissPtPricingInputs.readNetworkOfDistances(mainConfig, SwissPtPricingConfigGroup.getOrCreate(mainConfig));
	}

	@Provides
	@Singleton
	public SwissPtStageCostCalculator provideSwissPtStageCostCalculator(Config mainConfig) throws Exception {
		// the bundled pricing description without pricingDescriptionPath, and the Léman Pass if the config has it
		return SwissPtPricingInputs.readCalculator(mainConfig, SwissPtPricingConfigGroup.getOrCreate(mainConfig));
	}

	/** The fares of the pricing description bundled with eqasim (as used by eqasim-switzerland), for a config without one. */
	public static SwissPtStageCostCalculator createDefaultPtStageCostCalculator() {
		return SwissPtPricingInputs.readDefaultCalculator();
	}
}
