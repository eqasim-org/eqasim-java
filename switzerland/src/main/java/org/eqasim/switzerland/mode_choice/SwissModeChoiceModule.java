package org.eqasim.switzerland.mode_choice;

import java.io.IOException;

import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.switzerland.mode_choice.parameters.CostParameters;
import org.eqasim.switzerland.mode_choice.parameters.ModeChoiceParameters;
import org.eqasim.switzerland.mode_choice.utilities.SwissUtilityEstimator;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.ethz.matsim.discrete_mode_choice.modules.AbstractDiscreteModeChoiceExtension;

public class SwissModeChoiceModule extends AbstractDiscreteModeChoiceExtension {
	private final CommandLine commandLine;

	public SwissModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installExtension() {
		bindTripEstimator(EqasimModeChoiceModule.UTILITY_ESTIMATOR_NAME).to(SwissUtilityEstimator.class);
		bindModeAvailability(EqasimModeChoiceModule.MODE_AVAILABILITY_NAME).to(SwissModeAvailability.class);
	}

	@Provides
	@Singleton
	public ModeChoiceParameters provideModeChoiceParameters() throws IOException, ConfigurationException {
		ModeChoiceParameters parameters = ModeChoiceParameters.buildDefault();
		ParameterDefinition.applyCommandLine("mode-choice-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public CostParameters provideCostParameters() {
		CostParameters parameters = CostParameters.buildDefault();
		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}
}
