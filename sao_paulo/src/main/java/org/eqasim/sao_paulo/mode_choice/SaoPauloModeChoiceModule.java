package org.eqasim.sao_paulo.mode_choice;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.sao_paulo.mode_choice.constraints.VehicleTourConstraintWithCarPassenger;
import org.eqasim.sao_paulo.mode_choice.constraints.WalkDurationConstraint;
import org.eqasim.sao_paulo.mode_choice.costs.SaoPauloCarCostModel;
import org.eqasim.sao_paulo.mode_choice.costs.SaoPauloPtCostModel;
import org.eqasim.sao_paulo.mode_choice.costs.SaoPauloTaxiCostModel;
import org.eqasim.sao_paulo.mode_choice.parameters.SaoPauloCostParameters;
import org.eqasim.sao_paulo.mode_choice.parameters.SaoPauloModeParameters;
import org.eqasim.sao_paulo.mode_choice.utilities.estimators.SaoPauloCarUtilityEstimator;
import org.eqasim.sao_paulo.mode_choice.utilities.estimators.SaoPauloPTUtilityEstimator;
import org.eqasim.sao_paulo.mode_choice.utilities.estimators.SaoPauloTaxiUtilityEstimator;
import org.eqasim.sao_paulo.mode_choice.utilities.estimators.SaoPauloWalkUtilityEstimator;
import org.eqasim.sao_paulo.mode_choice.utilities.predictors.SaoPauloPersonPredictor;
import org.eqasim.sao_paulo.mode_choice.utilities.predictors.SaoPauloTaxiPredictor;
import org.matsim.contribs.discrete_mode_choice.components.utils.home_finder.HomeFinder;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.VehicleTourConstraintConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class SaoPauloModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	static public final String MODE_AVAILABILITY_NAME = "SaoPauloModeAvailability";

	static public final String CAR_COST_MODEL_NAME = "SaoPauloCarCostModel";
	static public final String PT_COST_MODEL_NAME = "SaoPauloPtCostModel";
	static public final String TAXI_COST_MODEL_NAME = "SaoPauloTaxiCostModel";

	public SaoPauloModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(SaoPauloModeAvailability.class);

		bind(SaoPauloPersonPredictor.class);
		bind(SaoPauloTaxiPredictor.class);
		bindTourConstraintFactory("VehicleTourConstraintWithCarPassenger")
		.to(VehicleTourConstraintWithCarPassenger.Factory.class);
		bindTripConstraintFactory("WalkDurationConstraint")
		.to(WalkDurationConstraint.Factory.class);

		bindCostModel(CAR_COST_MODEL_NAME).to(SaoPauloCarCostModel.class);
		bindCostModel(PT_COST_MODEL_NAME).to(SaoPauloPtCostModel.class);
		bindCostModel(TAXI_COST_MODEL_NAME).to(SaoPauloTaxiCostModel.class);
		bindUtilityEstimator("spPTEstimator").to(SaoPauloPTUtilityEstimator.class);
		bindUtilityEstimator("spWalkEstimator").to(SaoPauloWalkUtilityEstimator.class);
		bindUtilityEstimator("spCarEstimator").to(SaoPauloCarUtilityEstimator.class);
		bindUtilityEstimator("spTaxiEstimator").to(SaoPauloTaxiUtilityEstimator.class);
		bind(ModeParameters.class).to(SaoPauloModeParameters.class);
	}

	@Provides
	@Singleton
	public SaoPauloModeParameters provideModeChoiceParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		SaoPauloModeParameters parameters = SaoPauloModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public SaoPauloCostParameters provideCostParameters(EqasimConfigGroup config) {
		SaoPauloCostParameters parameters = SaoPauloCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}
	
	@Provides
	@Named("taxi")
	public CostModel provideTaxiCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
		return getCostModel(factory, config, "taxi");
	}
	
	@Provides
	@Singleton
	public WalkDurationConstraint.Factory provideWalkDurationConstraintFactory(DiscreteModeChoiceConfigGroup dmcConfig,
			HomeFinder homeFinder, Config config) {
		return new WalkDurationConstraint.Factory(config);
	}
	
	@Provides
	@Singleton
	public VehicleTourConstraintWithCarPassenger.Factory provideVehicleTourConstraintWithCarPassengerFactory(
			DiscreteModeChoiceConfigGroup dmcConfig, HomeFinder homeFinder) {
		VehicleTourConstraintConfigGroup config = dmcConfig.getVehicleTourConstraintConfig();
		return new VehicleTourConstraintWithCarPassenger.Factory(config.getRestrictedModes(), homeFinder);
	}
}
