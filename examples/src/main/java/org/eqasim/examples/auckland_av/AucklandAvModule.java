package org.eqasim.examples.auckland_av;

import java.io.File;

import org.eqasim.auckland.costs.AucklandCostParameters;
import org.eqasim.automated_vehicles.components.EqasimAvConfigGroup;
import org.eqasim.automated_vehicles.mode_choice.cost.AvCostParameters;
import org.eqasim.automated_vehicles.mode_choice.mode_parameters.AvModeParameters;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.matsim.core.config.CommandLine;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class AucklandAvModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public AucklandAvModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(AucklandAvModeAvailability.NAME).to(AucklandAvModeAvailability.class);
	}
	
	@Provides
	@Singleton
	public AucklandAvCostParameters provideAucklandAvCostParameters(EqasimAvConfigGroup config) {
		AucklandAvCostParameters parameters = AucklandAvCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}
	
	@Provides
	@Singleton
	public AvCostParameters provideAvCostParameters(AucklandAvCostParameters parameters) {
		return parameters.av;
	}

	@Provides
	@Singleton
	public AucklandCostParameters provideCostParameters(AucklandAvCostParameters parameters) {
		return parameters;
	}

	@Provides
	@Singleton
	public AucklandAvModeParameters provideAucklandAvModeParameters(EqasimAvConfigGroup config) {
		AucklandAvModeParameters parameters = AucklandAvModeParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public AvModeParameters provideAvModeParameters(AucklandAvModeParameters parameters) {
		return parameters.av;
	}

	@Provides
	@Singleton
	public ModeParameters provideModeParameters(AucklandAvModeParameters parameters) {
		return parameters;
	}
}
