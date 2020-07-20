package org.eqasim.jakarta.mode_choice;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.jakarta.mode_choice.constraints.VehicleTourConstraintWithCarPassenger;
import org.eqasim.jakarta.mode_choice.constraints.WalkDurationConstraint;
import org.eqasim.jakarta.mode_choice.costs.JakartaCarCostModel;
import org.eqasim.jakarta.mode_choice.costs.JakartaCarodtCostModel;
import org.eqasim.jakarta.mode_choice.costs.JakartaMcodtCostModel;
import org.eqasim.jakarta.mode_choice.costs.JakartaMotorcycleCostModel;
import org.eqasim.jakarta.mode_choice.costs.JakartaPtCostModel;
import org.eqasim.jakarta.mode_choice.parameters.JakartaCostParameters;
import org.eqasim.jakarta.mode_choice.parameters.JakartaModeParameters;
import org.eqasim.jakarta.mode_choice.utilities.estimators.JakartaCarUtilityEstimator;
import org.eqasim.jakarta.mode_choice.utilities.estimators.JakartaCarodtUtilityEstimator;
import org.eqasim.jakarta.mode_choice.utilities.estimators.JakartaMcodtUtilityEstimator;
import org.eqasim.jakarta.mode_choice.utilities.estimators.JakartaMotorcycleUtilityEstimator;
import org.eqasim.jakarta.mode_choice.utilities.estimators.JakartaPTUtilityEstimator;
import org.eqasim.jakarta.mode_choice.utilities.estimators.JakartaWalkUtilityEstimator;
import org.eqasim.jakarta.mode_choice.utilities.predictors.JakartaCarodtPredictor;
import org.eqasim.jakarta.mode_choice.utilities.predictors.JakartaMcodtPredictor;
import org.eqasim.jakarta.mode_choice.utilities.predictors.JakartaPersonPredictor;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import ch.ethz.matsim.discrete_mode_choice.components.utils.home_finder.HomeFinder;
import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import ch.ethz.matsim.discrete_mode_choice.modules.config.VehicleTourConstraintConfigGroup;

public class JakartaModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	static public final String MODE_AVAILABILITY_NAME = "JakartaModeAvailability";

	static public final String CAR_COST_MODEL_NAME = "JakartaCarCostModel";
	static public final String PT_COST_MODEL_NAME = "JakartaPtCostModel";
	static public final String CARODT_COST_MODEL_NAME = "JakartaCarodtCostModel";
	static public final String MCODT_COST_MODEL_NAME = "JakartaMcodtCostModel";
	static public final String MOTORCYCLE_COST_MODEL_NAME = "JakartaMotorcycleCostModel";

	public JakartaModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(JakartaModeAvailability.class);

		bind(JakartaPersonPredictor.class);
		bind(JakartaCarodtPredictor.class);
		bind(JakartaMcodtPredictor.class);
		bindTourConstraintFactory("VehicleTourConstraintWithCarPassenger")
		.to(VehicleTourConstraintWithCarPassenger.Factory.class);
		bindTripConstraintFactory("WalkDurationConstraint")
		.to(WalkDurationConstraint.Factory.class);

		bindCostModel(CAR_COST_MODEL_NAME).to(JakartaCarCostModel.class);
		bindCostModel(PT_COST_MODEL_NAME).to(JakartaPtCostModel.class);
		bindCostModel(MCODT_COST_MODEL_NAME).to(JakartaMcodtCostModel.class);
		bindCostModel(CARODT_COST_MODEL_NAME).to(JakartaCarodtCostModel.class);
		bindCostModel(MOTORCYCLE_COST_MODEL_NAME).to(JakartaMotorcycleCostModel.class);
		bindUtilityEstimator("jPTEstimator").to(JakartaPTUtilityEstimator.class);
		bindUtilityEstimator("jWalkEstimator").to(JakartaWalkUtilityEstimator.class);
		bindUtilityEstimator("jCarEstimator").to(JakartaCarUtilityEstimator.class);
		bindUtilityEstimator("jCarodtEstimator").to(JakartaCarodtUtilityEstimator.class);
		bindUtilityEstimator("jMcodtEstimator").to(JakartaMcodtUtilityEstimator.class);
		bindUtilityEstimator("jMotorcycleEstimator").to(JakartaMotorcycleUtilityEstimator.class);
		bind(ModeParameters.class).to(JakartaModeParameters.class);
	}

	@Provides
	@Singleton
	public JakartaModeParameters provideModeChoiceParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		JakartaModeParameters parameters = JakartaModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public JakartaCostParameters provideCostParameters(EqasimConfigGroup config) {
		JakartaCostParameters parameters = JakartaCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}	
	
	
	@Provides
	@Singleton
	public WalkDurationConstraint.Factory provideWalkDurationConstraintFactory(DiscreteModeChoiceConfigGroup dmcConfig,
			@Named("tour") HomeFinder homeFinder, Config config) {
		return new WalkDurationConstraint.Factory(config);
	}
	
	@Provides
	@Singleton
	public VehicleTourConstraintWithCarPassenger.Factory provideVehicleTourConstraintWithCarPassengerFactory(
			DiscreteModeChoiceConfigGroup dmcConfig, @Named("tour") HomeFinder homeFinder) {
		VehicleTourConstraintConfigGroup config = dmcConfig.getVehicleTourConstraintConfig();
		return new VehicleTourConstraintWithCarPassenger.Factory(config.getRestrictedModes(), homeFinder);
	}
}
