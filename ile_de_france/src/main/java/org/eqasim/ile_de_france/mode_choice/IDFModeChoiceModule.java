package org.eqasim.ile_de_france.mode_choice;

import java.io.IOException;

import org.eqasim.core.simulation.mode_choice.ModeChoiceModule;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.ile_de_france.mode_choice.parameters.CostParameters;
import org.eqasim.ile_de_france.mode_choice.parameters.ModeChoiceParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.IDFUtilityEstimator;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.ethz.matsim.discrete_mode_choice.modules.AbstractDiscreteModeChoiceExtension;

public class IDFModeChoiceModule extends AbstractDiscreteModeChoiceExtension {
	private final CommandLine commandLine;

	public IDFModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installExtension() {
		bindTripEstimator(ModeChoiceModule.UTILITY_ESTIMATOR_NAME).to(IDFUtilityEstimator.class);
		bindModeAvailability(ModeChoiceModule.MODE_AVAILABILITY_NAME).to(IDFModeAvailability.class);
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
