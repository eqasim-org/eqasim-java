package org.eqasim.projects.astra16;

import java.io.File;
import java.io.IOException;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class AstraZurichModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public AstraZurichModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Provides
	@Singleton
	public SwissModeParameters provideSwissModeParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		SwissModeParameters parameters = SwissModeParameters.buildASTRA2016();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;
	}

	@Override
	protected void installEqasimExtension() {
	}
}
