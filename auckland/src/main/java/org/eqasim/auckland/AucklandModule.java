package org.eqasim.auckland;

import java.io.File;
import java.io.IOException;

import org.eqasim.auckland.costs.AucklandCarCostModel;
import org.eqasim.auckland.costs.AucklandCostParameters;
import org.eqasim.auckland.costs.AucklandPtCostModel;
import org.eqasim.auckland.mode_choice.AucklandModeAvailability;
import org.eqasim.auckland.mode_choice.AucklandModeParameters;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class AucklandModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public AucklandModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	public void installEqasimExtension() {
		bind(AucklandCarCostModel.class);
		bind(AucklandPtCostModel.class);

		bindCostModel(AucklandCarCostModel.NAME).to(AucklandCarCostModel.class);
		bindCostModel(AucklandPtCostModel.NAME).to(AucklandPtCostModel.class);

		bindModeAvailability(AucklandModeAvailability.NAME).to(AucklandModeAvailability.class);
	}

	@Provides
	@Singleton
	public ModeParameters provideModeParameters(EqasimConfigGroup config) throws IOException, ConfigurationException {
		ModeParameters parameters = AucklandModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public AucklandCostParameters provideCostParameters(EqasimConfigGroup config) {
		AucklandCostParameters parameters = AucklandCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}
}
