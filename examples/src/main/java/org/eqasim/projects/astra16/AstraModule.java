package org.eqasim.projects.astra16;

import java.io.File;
import java.io.IOException;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.projects.astra16.mode_choice.InfiniteHeadwayConstraint;
import org.eqasim.projects.astra16.mode_choice.utilities.estimators.AstraBikeUtilityEstimator;
import org.eqasim.projects.astra16.mode_choice.utilities.estimators.AstraCarUtilityEstimator;
import org.eqasim.projects.astra16.mode_choice.utilities.estimators.AstraPtUtilityEstimator;
import org.eqasim.projects.astra16.mode_choice.utilities.estimators.AstraWalkUtilityEstimator;
import org.eqasim.projects.astra16.mode_choice.utilities.predictors.AstraBikePredictor;
import org.eqasim.projects.astra16.mode_choice.utilities.predictors.AstraPersonPredictor;
import org.eqasim.projects.astra16.mode_choice.utilities.predictors.AstraPtPredictor;
import org.eqasim.projects.astra16.mode_choice.utilities.predictors.AstraTripPredictor;
import org.eqasim.projects.astra16.mode_choice.utilities.predictors.AstraWalkPredictor;
import org.eqasim.switzerland.ovgk.OVGKCalculator;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class AstraModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public AstraModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindUtilityEstimator(AstraCarUtilityEstimator.NAME).to(AstraCarUtilityEstimator.class);
		bindUtilityEstimator(AstraPtUtilityEstimator.NAME).to(AstraPtUtilityEstimator.class);
		bindUtilityEstimator(AstraBikeUtilityEstimator.NAME).to(AstraBikeUtilityEstimator.class);
		bindUtilityEstimator(AstraWalkUtilityEstimator.NAME).to(AstraWalkUtilityEstimator.class);

		bind(AstraPtPredictor.class);
		bind(AstraBikePredictor.class);
		bind(AstraWalkPredictor.class);
		bind(AstraPersonPredictor.class);
		bind(AstraTripPredictor.class);

		bindTripConstraintFactory(InfiniteHeadwayConstraint.NAME).to(InfiniteHeadwayConstraint.Factory.class);
	}

	@Provides
	@Singleton
	public AstraModeParameters provideAstraModeParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		AstraModeParameters parameters = AstraModeParameters.buildFrom6Feb2020();

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
}
