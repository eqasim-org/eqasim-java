package org.eqasim.switzerland.ch.mode_choice;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.switzerland.ch.config.SwissPTZonesConfigGroup;
import org.eqasim.switzerland.ch.mode_choice.constraints.LoopModesConstraint;
import org.eqasim.switzerland.ch.mode_choice.costs.SwissCarCostModel;
import org.eqasim.switzerland.ch.mode_choice.costs.SwissPtCostModel;
import org.eqasim.switzerland.ch.mode_choice.costs.pt.SwissPtStageCostCalculator;
import org.eqasim.switzerland.ch.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.ch.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.ch.mode_choice.utilities.estimators.SwissBikeUtilityEstimator;
import org.eqasim.switzerland.ch.mode_choice.utilities.estimators.SwissCarUtilityEstimator;
import org.eqasim.switzerland.ch.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch.mode_choice.utilities.predictors.SwissPtRoutePredictor;
import org.eqasim.switzerland.ch.utils.pricing.inputs.Authority;
import org.eqasim.switzerland.ch.utils.pricing.inputs.ZonalReader;
import org.eqasim.switzerland.ch.utils.pricing.inputs.SBBDistanceReader;
import org.eqasim.switzerland.ch.utils.pricing.inputs.ZonalRegistry;
import org.eqasim.switzerland.ch.utils.pricing.inputs.NetworkOfDistances;
import org.eqasim.switzerland.ch.utils.pricing.inputs.Zone;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.opencsv.exceptions.CsvValidationException;

public class SwissModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	static public final String MODE_AVAILABILITY_NAME = "SwissModeAvailability";
	static public final String CAR_ESTIMATOR_NAME = "SwissCarEstimator";
	static public final String BIKE_ESTIMATOR_NAME = "SwissBikeEstimator";
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

		bindCostModel(CAR_COST_MODEL_NAME).to(SwissCarCostModel.class);
		bindCostModel(PT_COST_MODEL_NAME).to(SwissPtCostModel.class);

		bind(SwissPersonPredictor.class);
		bind(SwissPtRoutePredictor.class);

		bind(ModeParameters.class).to(SwissModeParameters.class).asEagerSingleton();
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
	//@Singleton
	public ZonalRegistry provideZonalRegistry(SwissPTZonesConfigGroup ptZonesConfig) throws IOException, CsvValidationException{
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