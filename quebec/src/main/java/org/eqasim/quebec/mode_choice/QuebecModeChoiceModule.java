package org.eqasim.quebec.mode_choice;

import java.io.File;
import java.io.IOException;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.quebec.mode_choice.constraints.VehicleTourConstraintWithCarPassenger;
import org.eqasim.quebec.mode_choice.constraints.WalkDurationConstraint;

import org.eqasim.quebec.mode_choice.costs.QuebecPtCostModel;
import org.eqasim.quebec.mode_choice.parameters.QuebecCostParameters;
import org.eqasim.quebec.mode_choice.parameters.QuebecModeParameters;
import org.eqasim.quebec.mode_choice.utilities.estimators.QuebecCarUtilityEstimator;
import org.eqasim.quebec.mode_choice.utilities.estimators.QuebecCarPassengerUtilityEstimator;

import org.eqasim.quebec.mode_choice.utilities.estimators.QuebecPTUtilityEstimator;
import org.eqasim.quebec.mode_choice.utilities.estimators.QuebecWalkUtilityEstimator;
import org.eqasim.quebec.mode_choice.utilities.predictors.QuebecPersonPredictor;
import org.matsim.contribs.discrete_mode_choice.components.utils.home_finder.HomeFinder;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.VehicleTourConstraintConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class QuebecModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	static public final String MODE_AVAILABILITY_NAME = "QuebecModeAvailability";

	
	static public final String PT_COST_MODEL_NAME = "QuebecPtCostModel";

	public QuebecModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(QuebecModeAvailability.class);
		bindTourConstraintFactory("VehicleTourConstraintWithCarPassenger")
				.to(VehicleTourConstraintWithCarPassenger.Factory.class);
		bindTripConstraintFactory("WalkDurationConstraint")
				.to(WalkDurationConstraint.Factory.class);
		bind(QuebecPersonPredictor.class);

		
		bindCostModel(PT_COST_MODEL_NAME).to(QuebecPtCostModel.class);
		bindUtilityEstimator("qcPTEstimator").to(QuebecPTUtilityEstimator.class);
		bindUtilityEstimator("qcWalkEstimator").to(QuebecWalkUtilityEstimator.class);
		bindUtilityEstimator("qcCarEstimator").to(QuebecCarUtilityEstimator.class);
		bindUtilityEstimator("qcCarPassengerEstimator").to(QuebecCarPassengerUtilityEstimator.class);

		bind(ModeParameters.class).to(QuebecModeParameters.class);
	}

	@Provides
	@Singleton
	public QuebecModeParameters provideModeChoiceParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		QuebecModeParameters parameters = QuebecModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);

		return parameters;
	}

	@Provides
	@Singleton
	public QuebecCostParameters provideCostParameters(EqasimConfigGroup config) {
		QuebecCostParameters parameters = QuebecCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);

		return parameters;
	}

	@Provides
	@Singleton
	public VehicleTourConstraintWithCarPassenger.Factory provideVehicleTourConstraintWithCarPassengerFactory(
			DiscreteModeChoiceConfigGroup dmcConfig, HomeFinder homeFinder) {
		VehicleTourConstraintConfigGroup config = dmcConfig.getVehicleTourConstraintConfig();
		return new VehicleTourConstraintWithCarPassenger.Factory(config.getRestrictedModes(), homeFinder);
	}

	@Provides
	@Singleton
	public WalkDurationConstraint.Factory provideWalkDurationConstraintFactory(DiscreteModeChoiceConfigGroup dmcConfig,
			HomeFinder homeFinder) {
		return new WalkDurationConstraint.Factory();
	}
}
