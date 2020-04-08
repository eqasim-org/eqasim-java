package org.eqasim.wayne_county.mode_choice;

import java.io.File;
import java.io.IOException;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.wayne_county.mode_choice.costs.WayneCountyCarCostModel;
import org.eqasim.wayne_county.mode_choice.costs.WayneCountyPtCostModel;
import org.eqasim.wayne_county.mode_choice.parameters.WayneCountyCostParameters;
import org.eqasim.wayne_county.mode_choice.parameters.WayneCountyModeParameters;
import org.eqasim.wayne_county.mode_choice.utilities.estimators.*;
import org.eqasim.wayne_county.mode_choice.utilities.predictors.WayneCountyPersonPredictor;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class WayneCountyModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	static public final String MODE_AVAILABILITY_NAME = "WayneCountyModeAvailability";

	static public final String CAR_COST_MODEL_NAME = "WayneCountyCarCostModel";
	static public final String PT_COST_MODEL_NAME = "WayneCountyPtCostModel";

	public WayneCountyModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(WayneCountyModeAvailability.class);

		bind(WayneCountyPersonPredictor.class);
		bind(ModeParameters.class).to(WayneCountyModeParameters.class);

		bindCostModel(CAR_COST_MODEL_NAME).to(WayneCountyCarCostModel.class);
		bindCostModel(PT_COST_MODEL_NAME).to(WayneCountyPtCostModel.class);
		bindUtilityEstimator("wcPTEstimator").to(WayneCountyPTUtilityEstimator.class);
		bindUtilityEstimator("wcWalkEstimator").to(WayneCountyWalkUtilityEstimator.class);
		bindUtilityEstimator("wcCarEstimator").to(WayneCountyCarUtilityEstimator.class);
		bindUtilityEstimator("wcBikeEstimator").to(WayneCountyBikeUtilityEstimator.class);
		bindUtilityEstimator("wcDRTEstimator").to(WayneCountyDRTUtilityEstimator.class);

	}

	@Provides
	@Singleton
	public WayneCountyModeParameters provideModeChoiceParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		WayneCountyModeParameters parameters = WayneCountyModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);

		return parameters;
	}

	@Provides
	@Singleton
	public WayneCountyCostParameters provideCostParameters(EqasimConfigGroup config) {
		WayneCountyCostParameters parameters = WayneCountyCostParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);

		return parameters;
	}

}
