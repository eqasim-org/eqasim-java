package org.eqasim.examples.corsica_parking.mode_choice;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.examples.corsica_parking.mode_choice.parameters.CorsicaParkingModeParameters;
import org.eqasim.examples.corsica_parking.mode_choice.utilities.estimators.CorsicaParkingCarUtilityEstimator;
import org.eqasim.examples.corsica_parking.mode_choice.utilities.predictors.CorsicaParkingCarPredictor;
import org.matsim.core.config.CommandLine;

import java.io.File;

public class CorsicaParkingModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public static final String CAR_ESTIMATOR_NAME = "CarParkingUtilityEstimator";

	public CorsicaParkingModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		// Override car alternative with parking
		bind(CorsicaParkingCarPredictor.class);
		bindUtilityEstimator(CAR_ESTIMATOR_NAME).to(CorsicaParkingCarUtilityEstimator.class);
		bind(ModeParameters.class).to(CorsicaParkingModeParameters.class);
	}

	@Provides
	@Singleton
	public CorsicaParkingModeParameters provideModeParameters(EqasimConfigGroup config) {
		CorsicaParkingModeParameters parameters = CorsicaParkingModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;
	}
}
