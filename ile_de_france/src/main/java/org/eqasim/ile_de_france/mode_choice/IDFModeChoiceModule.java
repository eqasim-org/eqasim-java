package org.eqasim.ile_de_france.mode_choice;

import java.io.IOException;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimModule;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.ile_de_france.mode_choice.costs.IDFCarCostModel;
import org.eqasim.ile_de_france.mode_choice.costs.IDFPtCostModel;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class IDFModeChoiceModule extends AbstractEqasimModule {
	private final CommandLine commandLine;

	public final static String MODE_AVAILABILITY_NAME = "IDFModeAvailability";

	public IDFModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(IDFModeAvailability.class);
		
		bind(IDFPersonPredictor.class);

		bindCostModel("idf_car").to(IDFCarCostModel.class);
		bindCostModel("idf_pt").to(IDFPtCostModel.class);

		bind(ModeParameters.class).to(IDFModeParameters.class);
	}

	@Provides
	@Singleton
	public IDFModeParameters provideModeChoiceParameters() throws IOException, ConfigurationException {
		IDFModeParameters parameters = IDFModeParameters.buildDefault();
		ParameterDefinition.applyCommandLine("mode-choice-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public IDFCostParameters provideCostParameters() {
		IDFCostParameters parameters = IDFCostParameters.buildDefault();
		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}
}
