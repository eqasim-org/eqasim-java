package org.eqasim.switzerland.mode_choice;

import java.io.IOException;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimModule;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.switzerland.mode_choice.costs.SwissCarCostModel;
import org.eqasim.switzerland.mode_choice.costs.SwissPtCostModel;
import org.eqasim.switzerland.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.mode_choice.utilities.estimators.SwissBikeEstimator;
import org.eqasim.switzerland.mode_choice.utilities.estimators.SwissCarEstimator;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class SwissModeChoiceModule extends AbstractEqasimModule {
	private final CommandLine commandLine;

	public static final String MODE_AVAILABILITY_NAME = "SwissModeAvailability";

	public SwissModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(SwissModeAvailability.class);

		bind(SwissPersonPredictor.class);

		bind(SwissCarEstimator.class);
		bind(SwissBikeEstimator.class);

		bindUtilityEstimator("swiss_car").to(SwissCarEstimator.class);
		bindUtilityEstimator("swiss_bike").to(SwissBikeEstimator.class);

		bind(SwissCarCostModel.class);
		bind(SwissPtCostModel.class);

		bindCostModel("swiss_car").to(SwissCarCostModel.class);
		bindCostModel("swiss_pt").to(SwissPtCostModel.class);

		bind(ModeParameters.class).to(SwissModeParameters.class);
	}

	@Provides
	@Singleton
	public SwissModeParameters provideModeChoiceParameters() throws IOException, ConfigurationException {
		SwissModeParameters parameters = SwissModeParameters.buildDefault();
		ParameterDefinition.applyCommandLine("mode-choice-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public SwissCostParameters provideCostParameters() {
		SwissCostParameters parameters = SwissCostParameters.buildDefault();
		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}
}
