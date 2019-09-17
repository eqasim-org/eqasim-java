package org.eqasim.switzerland.mode_choice;

import java.io.File;
import java.io.IOException;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.switzerland.mode_choice.costs.SwissCarCostModel;
import org.eqasim.switzerland.mode_choice.costs.SwissPtCostModel;
import org.eqasim.switzerland.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.mode_choice.utilities.estimators.SwissBikeUtilityEstimator;
import org.eqasim.switzerland.mode_choice.utilities.estimators.SwissCarUtilityEstimator;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class SwissModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	static public final String MODE_AVAILABILITY_NAME = "SwissModeAvailability";
	static public final String CAR_ESTIMATOR_NAME = "SwissCarEstimator";
	static public final String BIKE_ESTIMATOR_NAME = "SwissBikeEstimator";
	static public final String CAR_COST_MODEL_NAME = "SwissCarCostModel";
	static public final String PT_COST_MODEL_NAME = "SwissPtCostModel";

	public SwissModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(SwissModeAvailability.class);

		bindUtilityEstimator(CAR_ESTIMATOR_NAME).to(SwissCarUtilityEstimator.class);
		bindUtilityEstimator(BIKE_ESTIMATOR_NAME).to(SwissBikeUtilityEstimator.class);

		bindCostModel(CAR_COST_MODEL_NAME).to(SwissCarCostModel.class);
		bindCostModel(PT_COST_MODEL_NAME).to(SwissPtCostModel.class);

		bind(SwissPersonPredictor.class);

		bind(ModeParameters.class).to(SwissModeParameters.class).asEagerSingleton();
	}

	@Provides
	@Singleton
	public SwissModeParameters provideSwissModeParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		SwissModeParameters parameters = SwissModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public SwissCostParameters provideCostParameters(EqasimConfigGroup config) {
		SwissCostParameters parameters = SwissCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}
}
