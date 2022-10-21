package org.eqasim.examples.zurich_parking.mode_choice;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.examples.zurich_parking.mode_choice.parameters.ZurichParkingModeParameters;
import org.eqasim.examples.zurich_parking.mode_choice.utilities.estimators.ZurichParkingCarUtilityEstimator;
import org.eqasim.examples.zurich_parking.mode_choice.utilities.predictors.ZurichParkingCarPredictor;
import org.matsim.core.config.CommandLine;

import java.io.File;

public class ZurichParkingModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public static final String CAR_ESTIMATOR_NAME = "CarParkingUtilityEstimator";

	public ZurichParkingModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		// Override car alternative with parking
		bind(ZurichParkingCarPredictor.class);
		bindUtilityEstimator(CAR_ESTIMATOR_NAME).to(ZurichParkingCarUtilityEstimator.class);
		bind(ModeParameters.class).to(ZurichParkingModeParameters.class);
	}

	@Provides
	@Singleton
	public ZurichParkingModeParameters provideModeParameters(EqasimConfigGroup config) {
		ZurichParkingModeParameters parameters = ZurichParkingModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;
	}
}
