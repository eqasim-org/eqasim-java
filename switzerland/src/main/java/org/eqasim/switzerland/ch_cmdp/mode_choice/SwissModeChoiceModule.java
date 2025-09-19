package org.eqasim.switzerland.ch_cmdp.mode_choice;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.opencsv.exceptions.CsvValidationException;
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
import org.eqasim.switzerland.ch.calibration.AlphaCantonCalibrator;
import org.eqasim.switzerland.ch.config.SwissPTZonesConfigGroup;
import org.eqasim.switzerland.ch.mode_choice.constraints.LoopModesConstraint;
import org.eqasim.switzerland.ch.mode_choice.costs.pt.SwissPtStageCostCalculator;
import org.eqasim.switzerland.ch.utils.pricing.inputs.*;
import org.eqasim.switzerland.ch_cmdp.calibration.AlphaClusterCalibrator;
import org.eqasim.switzerland.ch_cmdp.calibration.CmdpOptimizer;
import org.eqasim.switzerland.ch_cmdp.calibration.CmdpOptimizerHandler;
import org.eqasim.switzerland.ch_cmdp.calibration.CmdpVariablesWriter;
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
import java.util.Collection;
import java.util.List;
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

	static public final String LOOP_CONSTRAINT_NAME = "LoopModesConstraint";

	public SwissModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {

		bind(VariablesWriter.class).to(CmdpVariablesWriter.class).asEagerSingleton();
		bind(Optimizer.class).to(CmdpOptimizer.class).asEagerSingleton();
		bind(OptimizerHandler.class).to(CmdpOptimizerHandler.class).asEagerSingleton();

		bindTripConstraintFactory(LOOP_CONSTRAINT_NAME).to(LoopModesConstraint.Factory.class);

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
		if (calConfig.isActivate()) {
			String level = calConfig.getLevel().toLowerCase();
			switch (level) {
				case "global":
					bind(FastCalibration.class).to(AlphaCalibrator.class).asEagerSingleton();
					break;
				case "canton":
					bind(FastCalibration.class).to(AlphaCantonCalibrator.class).asEagerSingleton();
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

		return new AlphaCantonCalibrator(scenario,outputHierarchy,targetModeShares, modeParameters,
				tripListConverter, calConfig.getCalibratedModes() ,calConfig.getBeta(), filePath, calConfig.isActivate());
	}

	@Provides
	@Singleton
	public AlphaClusterCalibrator provideAlphaClusterCalibrator(Scenario scenario,
															    OutputDirectoryHierarchy outputHierarchy,
															    SwissCmdpModeParameters modeParameters,
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
	public CmdpOptimizerHandler provideCmdpOptimizerHandler(CalibrationConfigGroup calibrationConfig, OutputDirectoryHierarchy outputDirectoryHierarchy,
															  EqasimConfigGroup eqasimConfigGroup, SwissCmdpModeParameters parameters, Optimizer optimizer) {
		return new CmdpOptimizerHandler(calibrationConfig, outputDirectoryHierarchy, eqasimConfigGroup, parameters, optimizer);
	}

	@Provides
	//@Singleton
	public ZonalRegistry provideZonalRegistry() throws IOException, CsvValidationException {
		SwissPTZonesConfigGroup ptZonesConfig = SwissPTZonesConfigGroup.getOrCreate(getConfig());

		String file_path = "";
		ZonalReader zonalReader = new ZonalReader();
		ZonalRegistry zonalRegistry = null;
		ZonalRegistry sbbZonalRegistry = null;

		if (ptZonesConfig.getZonePath() != null){
			file_path = ptZonesConfig.getZonePath();
			File path = new File(file_path);
			Collection<Authority> authorities = zonalReader.readTarifNetworks(path);
			Collection<Zone> zones = zonalReader.readZones(path, authorities);
			zonalRegistry = new ZonalRegistry(authorities, zones);
		}
		else{
			throw new IOException("No input file detected to create the zonal registry.");
		}

		if (ptZonesConfig.getSBBDistancesPath() != null) {
			file_path = ptZonesConfig.getSBBDistancesPath();
			File path = new File(file_path);
			Zone sbbZone = SBBDistanceReader.createZone(path);
			sbbZonalRegistry = SBBDistanceReader.createZonalRegistry(sbbZone);
			zonalRegistry.merge(sbbZonalRegistry);
		}
		else{
			throw new IOException("No input file detected to create the SBB network.");
		}

		return zonalRegistry;
	}

	@Provides
	public NetworkOfDistances provideNetworkOfDistances() throws IOException, CsvValidationException{
		SwissPTZonesConfigGroup ptZonesConfig = SwissPTZonesConfigGroup.getOrCreate(getConfig());

		String file_path = "";
		NetworkOfDistances sbbNetwork = new NetworkOfDistances();

		if (ptZonesConfig.getSBBDistancesPath() != null){
			file_path = ptZonesConfig.getSBBDistancesPath();
			File path = new File(file_path);
			sbbNetwork = SBBDistanceReader.createNetworkOfDistances(path);
		}
		else{
			throw new IOException("No input file detected to create the SBB network.");
		}

		return sbbNetwork;

	}

	@Provides
	public SwissPtStageCostCalculator provideSwissPtStageCostCalculator(){
		return new SwissPtStageCostCalculator();
	}

}