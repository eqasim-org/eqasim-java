package org.eqasim.switzerland.zurich.mode_choice;

import java.io.File;
import java.io.IOException;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.switzerland.ch.mode_choice.SwissModeAvailability;
import org.eqasim.switzerland.ch.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.ch.ovgk.OVGKCalculator;
import org.eqasim.switzerland.zurich.mode_choice.constraints.InfiniteHeadwayConstraint;
import org.eqasim.switzerland.zurich.mode_choice.parameters.ZurichModeParameters;
import org.eqasim.switzerland.zurich.mode_choice.utilities.estimators.ZurichBikeUtilityEstimator;
import org.eqasim.switzerland.zurich.mode_choice.utilities.estimators.ZurichCarUtilityEstimator;
import org.eqasim.switzerland.zurich.mode_choice.utilities.estimators.ZurichPtUtilityEstimator;
import org.eqasim.switzerland.zurich.mode_choice.utilities.estimators.ZurichWalkUtilityEstimator;
import org.eqasim.switzerland.zurich.mode_choice.utilities.predictors.ZurichBikePredictor;
import org.eqasim.switzerland.zurich.mode_choice.utilities.predictors.ZurichPersonPredictor;
import org.eqasim.switzerland.zurich.mode_choice.utilities.predictors.ZurichPtPredictor;
import org.eqasim.switzerland.zurich.mode_choice.utilities.predictors.ZurichTripPredictor;
import org.eqasim.switzerland.zurich.mode_choice.utilities.predictors.ZurichWalkPredictor;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class ZurichModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public ZurichModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindUtilityEstimator(ZurichCarUtilityEstimator.NAME).to(ZurichCarUtilityEstimator.class);
		bindUtilityEstimator(ZurichPtUtilityEstimator.NAME).to(ZurichPtUtilityEstimator.class);
		bindUtilityEstimator(ZurichBikeUtilityEstimator.NAME).to(ZurichBikeUtilityEstimator.class);
		bindUtilityEstimator(ZurichWalkUtilityEstimator.NAME).to(ZurichWalkUtilityEstimator.class);

		bind(ZurichPtPredictor.class);
		bind(ZurichBikePredictor.class);
		bind(ZurichWalkPredictor.class);
		bind(ZurichPersonPredictor.class);
		bind(ZurichTripPredictor.class);

		bindTripConstraintFactory(InfiniteHeadwayConstraint.NAME).to(InfiniteHeadwayConstraint.Factory.class);

		bind(SwissModeParameters.class).to(ZurichModeParameters.class);

		bind(SwissModeAvailability.class);
		bindModeAvailability(ZurichModeAvailability.NAME).to(ZurichModeAvailability.class);

	}

	@Provides
	@Singleton
	public ZurichModeParameters provideZurichModeParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		ZurichModeParameters parameters = ZurichModeParameters.buildFrom6Feb2020();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public OVGKCalculator provideOVGKCalculator(TransitSchedule transitSchedule) {
		return new OVGKCalculator(transitSchedule);
	}

	@Provides
	public ZurichModeAvailability provideZurichModeAvailability(SwissModeAvailability delegate) {
		return new ZurichModeAvailability(delegate);
	}
}