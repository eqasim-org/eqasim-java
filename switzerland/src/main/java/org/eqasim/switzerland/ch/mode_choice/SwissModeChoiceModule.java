package org.eqasim.switzerland.ch.mode_choice;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.fast_calibration.AlphaCalibratorConfig;
import org.eqasim.core.components.fast_calibration.FastCalibration;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.switzerland.ch.calibration.AlphaCantonCalibrator;
import org.eqasim.switzerland.ch.config.SwissPTZonesConfigGroup;
import org.eqasim.switzerland.ch.mode_choice.constraints.LoopModesConstraint;
import org.eqasim.switzerland.ch.mode_choice.costs.SwissCarCostModel;
import org.eqasim.switzerland.ch.mode_choice.costs.SwissPtCostModel;
import org.eqasim.switzerland.ch.mode_choice.costs.pt.SwissPtStageCostCalculator;
import org.eqasim.switzerland.ch.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.ch.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.ch.mode_choice.utilities.estimators.SwissBikeUtilityEstimator;
import org.eqasim.switzerland.ch.mode_choice.utilities.estimators.SwissCarUtilityEstimator;
import org.eqasim.switzerland.ch.mode_choice.utilities.estimators.SwissPtUtilityEstimator;
import org.eqasim.switzerland.ch.mode_choice.utilities.estimators.SwissWalkUtilityEstimator;
import org.eqasim.switzerland.ch.mode_choice.utilities.estimators.SwissZeroUtilityEstimator;
import org.eqasim.switzerland.ch.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch.mode_choice.utilities.predictors.SwissPtRoutePredictor;
import org.eqasim.switzerland.ch.utils.pricing.inputs.Authority;
import org.eqasim.switzerland.ch.utils.pricing.inputs.NetworkOfDistances;
import org.eqasim.switzerland.ch.utils.pricing.inputs.SBBDistanceReader;
import org.eqasim.switzerland.ch.utils.pricing.inputs.ZonalReader;
import org.eqasim.switzerland.ch.utils.pricing.inputs.ZonalRegistry;
import org.eqasim.switzerland.ch.utils.pricing.inputs.Zone;
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

	public SwissModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindTripConstraintFactory(LOOP_CONSTRAINT_NAME).to(LoopModesConstraint.Factory.class);

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

		return new AlphaCantonCalibrator(scenario,outputHierarchy,targetModeShares,modeParameters,
				tripListConverter, calConfig.getCalibratedModes() ,calConfig.getBeta(), filePath, calConfig.isActivate());
	}

	@Provides
	//@Singleton
	public ZonalRegistry provideZonalRegistry(SwissPTZonesConfigGroup ptZonesConfig, Config mainConfig) throws IOException, CsvValidationException{
		ZonalReader zonalReader = new ZonalReader();
		ZonalRegistry zonalRegistry = null;
		ZonalRegistry sbbZonalRegistry = null;

		if (ptZonesConfig.getZonePath() != null){
			URL url = ConfigGroup.getInputFileURL(mainConfig.getContext(), ptZonesConfig.getZonePath());
			File path = new File(url.getPath());
			Collection<Authority> authorities = zonalReader.readTarifNetworks(path);
			Collection<Zone> zones = zonalReader.readZones(path, authorities);
			zonalRegistry = new ZonalRegistry(authorities, zones);
		}
		else{
			throw new IOException("No input file detected to create the zonal registry.");
		}

		if (ptZonesConfig.getSBBDistancesPath() != null) {
			URL url = ConfigGroup.getInputFileURL(mainConfig.getContext(), ptZonesConfig.getSBBDistancesPath());
			File path = new File(url.getPath());
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
	public NetworkOfDistances provideNetworkOfDistances(SwissPTZonesConfigGroup ptZonesConfig) throws IOException, CsvValidationException{
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