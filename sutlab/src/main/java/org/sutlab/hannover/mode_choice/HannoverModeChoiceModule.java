package org.sutlab.hannover.mode_choice;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.ActivityTourFinder;
import org.matsim.contribs.discrete_mode_choice.modules.config.ActivityTourFinderConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.eqasim.core.components.calibration.CalibrationConfigGroup;
import org.eqasim.core.components.calibration.Optimizer;
import org.eqasim.core.components.calibration.OptimizerHandler;
import org.eqasim.core.components.calibration.VariablesWriter;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.fast_calibration.AlphaCalibrator;
import org.eqasim.core.components.fast_calibration.AlphaCalibratorConfig;
import org.eqasim.core.components.fast_calibration.FastCalibration;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.tour_finder.ActivityTourFinderWithExcludedActivities;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.BikeUtilityEstimator;
import org.sutlab.hannover.calibration.AlphaClusterCalibrator;
import org.sutlab.hannover.calibration.CmdpOptimizer;
import org.sutlab.hannover.calibration.CmdpOptimizerHandler;
import org.sutlab.hannover.calibration.CmdpVariablesWriter;
import org.sutlab.hannover.mode_choice.costs.HannoverCarCostModel;
import org.sutlab.hannover.mode_choice.costs.HannoverPtCostModel;
import org.sutlab.hannover.mode_choice.parameters.HannoverCostParameters;
import org.sutlab.hannover.mode_choice.parameters.HannoverModeParameters;
import org.sutlab.hannover.mode_choice.utilities.estimators.HannoverBicycleUtilityEstimator;
import org.sutlab.hannover.mode_choice.utilities.estimators.HannoverCarPassengerUtilityEstimator;
import org.sutlab.hannover.mode_choice.utilities.estimators.HannoverCarUtilityEstimator;
import org.sutlab.hannover.mode_choice.utilities.estimators.HannoverPtUtilityEstimator;
import org.sutlab.hannover.mode_choice.utilities.predictors.HannoverCarPassengerPredictor;
import org.sutlab.hannover.mode_choice.utilities.predictors.HannoverPersonPredictor;
import org.sutlab.hannover.mode_choice.utilities.predictors.HannoverPtPredictor;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class HannoverModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public static final String MODE_AVAILABILITY_NAME = "HannoverModeAvailability";

	public static final String CAR_COST_MODEL_NAME = "HannoverCarCostModel";
	public static final String PT_COST_MODEL_NAME = "HannoverPtCostModel";

	public static final String CAR_ESTIMATOR_NAME = "HannoverCarUtilityEstimator";
	public static final String CAR_PASSENGER_ESTIMATOR_NAME = "HannoverCarPassengerUtilityEstimator";
	public static final String BICYCLE_ESTIMATOR_NAME = "HannoverBicycleUtilityEstimator";
	public static final String PT_ESTIMATOR_NAME = "HannoverPtUtilityEstimator";

	static public final String CAR_PASSENGER = "car_passenger";
	static public final String BICYCLE = "bike";

	public static final String ISOLATED_OUTSIDE_TOUR_FINDER_NAME = "IsolatedOutsideTrips";

	public HannoverModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(HannoverModeAvailability.class);

		bind(HannoverPersonPredictor.class);
		bind(HannoverCarPassengerPredictor.class);
		bind(HannoverPtPredictor.class);

		bindCostModel(CAR_COST_MODEL_NAME).to(HannoverCarCostModel.class);
		bindCostModel(PT_COST_MODEL_NAME).to(HannoverPtCostModel.class);

		bindUtilityEstimator(CAR_ESTIMATOR_NAME).to(HannoverCarUtilityEstimator.class);
		bindUtilityEstimator(BICYCLE_ESTIMATOR_NAME).to(HannoverBicycleUtilityEstimator.class);
		bindUtilityEstimator(CAR_PASSENGER_ESTIMATOR_NAME).to(HannoverCarPassengerUtilityEstimator.class);
		bindUtilityEstimator(PT_ESTIMATOR_NAME).to(HannoverPtUtilityEstimator.class);

		bind(ModeParameters.class).to(HannoverModeParameters.class);

		bindTourFinder(ISOLATED_OUTSIDE_TOUR_FINDER_NAME).to(ActivityTourFinderWithExcludedActivities.class);
		
		// Calibration
		bind(Optimizer.class).to(CmdpOptimizer.class).asEagerSingleton();
		bind(VariablesWriter.class).to(CmdpVariablesWriter.class).asEagerSingleton();
		bind(OptimizerHandler.class).to(CmdpOptimizerHandler.class).asEagerSingleton();
		
		AlphaCalibratorConfig calConfig = AlphaCalibratorConfig.getOrCreate(getConfig());
		if (calConfig.isActivate()) {
			String level = calConfig.getLevel().toLowerCase();
			switch (level) {
				case "global":
					bind(FastCalibration.class).to(AlphaCalibrator.class).asEagerSingleton();
					break;
				case "cluster":
					bind(FastCalibration.class).to(AlphaClusterCalibrator.class).asEagerSingleton();
					break;
				default:
					throw new IllegalArgumentException("Unknown calibration level: " + level);
			}
		}
	}

	@Provides
	@Singleton
	public CmdpVariablesWriter provideCmdpVariablesWriter(){
		return new CmdpVariablesWriter();
	}

	@Provides
	@Singleton
	public HannoverModeParameters provideModeChoiceParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		HannoverModeParameters parameters = HannoverModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-choice-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public HannoverCostParameters provideCostParameters(EqasimConfigGroup config) {
		HannoverCostParameters parameters = HannoverCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public ActivityTourFinderWithExcludedActivities provideActivityTourFinderWithExcludedActivities(
			DiscreteModeChoiceConfigGroup dmcConfig) {
		ActivityTourFinderConfigGroup config = dmcConfig.getActivityTourFinderConfigGroup();
		return new ActivityTourFinderWithExcludedActivities(List.of("outside"),
				new ActivityTourFinder(config.getActivityTypes()));
	}

	@Provides
	@Singleton
	public AlphaClusterCalibrator provideAlphaClusterCalibrator(Scenario scenario,
															    OutputDirectoryHierarchy outputHierarchy,
															    HannoverModeParameters modeParameters,
															    TripListConverter tripListConverter) {
		AlphaCalibratorConfig calConfig = AlphaCalibratorConfig.getOrCreate(getConfig());

		String filePath = calConfig.getFilePath();
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

		return new AlphaClusterCalibrator(scenario,outputHierarchy, targetModeShares, modeParameters,
				tripListConverter, calConfig.getCalibratedModes() ,calConfig.getBeta(), filePath, calConfig.isActivate());
	}

	@Provides
	@Singleton
	public CmdpOptimizer provideCmdpOptimizer() {
		CalibrationConfigGroup calibrationConfig = CalibrationConfigGroup.getOrCreate(getConfig());
		return new CmdpOptimizer(calibrationConfig);
	}

	@Provides
	@Singleton
	public CmdpOptimizerHandler provideCmdpOptimizerHandler(OutputDirectoryHierarchy outputDirectoryHierarchy,
															EqasimConfigGroup eqasimConfigGroup, HannoverModeParameters parameters,
															Optimizer optimizer) {
		CalibrationConfigGroup calibrationConfig = CalibrationConfigGroup.getOrCreate(getConfig());
		return new CmdpOptimizerHandler(calibrationConfig, outputDirectoryHierarchy, eqasimConfigGroup, parameters, optimizer);
	}
}