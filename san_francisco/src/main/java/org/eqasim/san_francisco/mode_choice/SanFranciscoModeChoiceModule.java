package org.eqasim.san_francisco.mode_choice;

import java.io.IOException;

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

public class SanFranciscoModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	static public final String MODE_AVAILABILITY_NAME = "SaoPauloModeAvailability";

	static public final String CAR_COST_MODEL_NAME = "SaoPauloCarCostModel";
	static public final String PT_COST_MODEL_NAME = "SaoPauloPtCostModel";

	public SanFranciscoModeChoiceModule(CommandLine commandLine) {
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
	public SaoPauloModeParameters provideModeChoiceParameters() throws IOException, ConfigurationException {
		SaoPauloModeParameters parameters = SaoPauloModeParameters.buildDefault();
		ParameterDefinition.applyCommandLine("mode-choice-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public SaoPauloCostParameters provideCostParameters() {
		SaoPauloCostParameters parameters = SaoPauloCostParameters.buildDefault();
		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}
}
