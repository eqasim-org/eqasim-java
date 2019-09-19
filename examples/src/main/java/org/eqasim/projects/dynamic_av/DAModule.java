package org.eqasim.projects.dynamic_av;

import java.io.File;
import java.io.IOException;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.projects.dynamic_av.mode_choice.DAModeParameters;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.estimators.DABikeUtilityEstimator;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.estimators.DACarUtilityEstimator;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.estimators.DAPtUtilityEstimator;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.estimators.DAWalkUtilityEstimator;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.DABikePredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.DAPersonPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.DAPtPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.DATripPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.DAWalkPredictor;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class DAModule extends AbstractEqasimExtension {
	static public final String DA_AV_MODE_AVAILABILITY_NAME = "DAAvModeAvailability";

	static public final String DA_CAR_ESTIMATOR = "DACarEstimator";
	static public final String DA_PT_ESTIMATOR = "DAPtEstimator";
	static public final String DA_BIKE_ESTIMATOR = "DABikeEstimator";
	static public final String DA_WALK_ESTIMATOR = "DAWalkEstimator";

	private final CommandLine commandLine;

	public DAModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(DA_AV_MODE_AVAILABILITY_NAME).to(DAModeAvailability.class);

		bindUtilityEstimator(DA_CAR_ESTIMATOR).to(DACarUtilityEstimator.class);
		bindUtilityEstimator(DA_PT_ESTIMATOR).to(DAPtUtilityEstimator.class);
		bindUtilityEstimator(DA_BIKE_ESTIMATOR).to(DABikeUtilityEstimator.class);
		bindUtilityEstimator(DA_WALK_ESTIMATOR).to(DAWalkUtilityEstimator.class);

		bind(DABikePredictor.class);
		bind(DAWalkPredictor.class);
		bind(DAPtPredictor.class);
		bind(DAPersonPredictor.class);
		bind(DATripPredictor.class);
	}

	@Provides
	@Singleton
	public DAModeParameters provideDAModeParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		DAModeParameters parameters = DAModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;
	}
}
