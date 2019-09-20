package org.eqasim.sao_paulo.mode_choice;

import java.io.File;
import java.io.IOException;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.sao_paulo.mode_choice.costs.SaoPauloCarCostModel;
import org.eqasim.sao_paulo.mode_choice.costs.SaoPauloPtCostModel;
import org.eqasim.sao_paulo.mode_choice.parameters.SaoPauloCostParameters;
import org.eqasim.sao_paulo.mode_choice.parameters.SaoPauloModeParameters;
import org.eqasim.sao_paulo.mode_choice.utilities.predictors.SaoPauloPersonPredictor;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class SaoPauloModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	static public final String MODE_AVAILABILITY_NAME = "SaoPauloModeAvailability";

	static public final String CAR_COST_MODEL_NAME = "SaoPauloCarCostModel";
	static public final String PT_COST_MODEL_NAME = "SaoPauloPtCostModel";

	public SaoPauloModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(SaoPauloModeAvailability.class);

		bind(SaoPauloPersonPredictor.class);

		bindCostModel(CAR_COST_MODEL_NAME).to(SaoPauloCarCostModel.class);
		bindCostModel(PT_COST_MODEL_NAME).to(SaoPauloPtCostModel.class);

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

		ParameterDefinition.applyCommandLine("mode-choice-parameter", commandLine, parameters);
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
}
