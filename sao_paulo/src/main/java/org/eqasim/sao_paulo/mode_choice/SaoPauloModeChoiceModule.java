package org.eqasim.sao_paulo.mode_choice;

import java.io.IOException;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimModule;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.sao_paulo.mode_choice.costs.SaoPauloCarCostModel;
import org.eqasim.sao_paulo.mode_choice.costs.SaoPauloPtCostModel;
import org.eqasim.sao_paulo.mode_choice.parameters.SaoPauloCostParameters;
import org.eqasim.sao_paulo.mode_choice.parameters.SaoPauloModeParameters;
import org.eqasim.sao_paulo.mode_choice.utilities.predictors.SaoPauloPersonPredictor;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class SaoPauloModeChoiceModule extends AbstractEqasimModule {
	private final CommandLine commandLine;

	public final static String MODE_AVAILABILITY_NAME = "SaoPauloModeAvailability";

	public SaoPauloModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(SaoPauloModeAvailability.class);

		bind(SaoPauloPersonPredictor.class);

		bindCostModel("sao_paulo_car").to(SaoPauloCarCostModel.class);
		bindCostModel("sao_paulo_pt").to(SaoPauloPtCostModel.class);
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
