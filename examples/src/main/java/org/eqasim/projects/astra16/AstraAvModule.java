package org.eqasim.projects.astra16;

import java.io.File;
import java.io.IOException;

import org.eqasim.automated_vehicles.components.EqasimAvConfigGroup;
import org.eqasim.automated_vehicles.mode_choice.mode_parameters.AvModeParameters;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.projects.astra16.mode_choice.AstraAvModeParameters;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraAvUtilityEstimator;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class AstraAvModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public AstraAvModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindUtilityEstimator(AstraAvUtilityEstimator.NAME).to(AstraAvUtilityEstimator.class);

		bind(AvModeParameters.class).to(AstraAvModeParameters.class);
	}

	@Provides
	@Singleton
	public AstraAvModeParameters provideAstraAvModeParameters(EqasimAvConfigGroup config)
			throws IOException, ConfigurationException {
		AstraAvModeParameters parameters = AstraAvModeParameters.buildFrom6Feb2020();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("av-mode-parameter", commandLine, parameters);
		return parameters;
	}
}
