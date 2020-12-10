package org.eqasim.los_angeles.mode_choice;

import java.io.File;
import java.io.IOException;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.los_angeles.mode_choice.constraints.VehicleTourConstraintWithCarPassenger;
import org.eqasim.los_angeles.mode_choice.constraints.WalkDurationConstraint;
import org.eqasim.los_angeles.mode_choice.costs.LosAngelesCarCostModel;
import org.eqasim.los_angeles.mode_choice.costs.LosAngelesPtCostModel;
import org.eqasim.los_angeles.mode_choice.parameters.LosAngelesCostParameters;
import org.eqasim.los_angeles.mode_choice.parameters.LosAngelesModeParameters;
import org.eqasim.los_angeles.mode_choice.utilities.estimators.LosAngelesCarUtilityEstimator;
import org.eqasim.los_angeles.mode_choice.utilities.estimators.LosAngelesPTUtilityEstimator;
import org.eqasim.los_angeles.mode_choice.utilities.estimators.LosAngelesWalkUtilityEstimator;
import org.eqasim.los_angeles.mode_choice.utilities.predictors.LosAngelesPersonPredictor;
import org.matsim.contribs.discrete_mode_choice.components.utils.home_finder.HomeFinder;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.VehicleTourConstraintConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class LosAngelesModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	static public final String MODE_AVAILABILITY_NAME = "LosAngelesModeAvailability";

	static public final String CAR_COST_MODEL_NAME = "LosAngelesCarCostModel";
	static public final String PT_COST_MODEL_NAME = "LosAngelesPtCostModel";

	public LosAngelesModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(LosAngelesModeAvailability.class);
		bindTourConstraintFactory("VehicleTourConstraintWithCarPassenger")
				.to(VehicleTourConstraintWithCarPassenger.Factory.class);
		bindTripConstraintFactory("WalkDurationConstraint")
				.to(WalkDurationConstraint.Factory.class);
		bind(LosAngelesPersonPredictor.class);

		bindCostModel(CAR_COST_MODEL_NAME).to(LosAngelesCarCostModel.class);
		bindCostModel(PT_COST_MODEL_NAME).to(LosAngelesPtCostModel.class);
		bindUtilityEstimator("laPTEstimator").to(LosAngelesPTUtilityEstimator.class);
		bindUtilityEstimator("laWalkEstimator").to(LosAngelesWalkUtilityEstimator.class);
		bindUtilityEstimator("laCarEstimator").to(LosAngelesCarUtilityEstimator.class);

		bind(ModeParameters.class).to(LosAngelesModeParameters.class);
	}

	@Provides
	@Singleton
	public LosAngelesModeParameters provideModeChoiceParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		LosAngelesModeParameters parameters = LosAngelesModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);

		return parameters;
	}

	@Provides
	@Singleton
	public LosAngelesCostParameters provideCostParameters(EqasimConfigGroup config) {
		LosAngelesCostParameters parameters = LosAngelesCostParameters.buildDefault();

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
