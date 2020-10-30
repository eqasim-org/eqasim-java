package org.eqasim.projects.astra16;

import java.io.File;
import java.io.IOException;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.projects.astra16.analysis.ConvergenceListener;
import org.eqasim.projects.astra16.convergence.AstraConvergenceCriterion;
import org.eqasim.projects.astra16.mode_choice.AstraModeAvailability;
import org.eqasim.projects.astra16.mode_choice.AstraModeParameters;
import org.eqasim.projects.astra16.mode_choice.InfiniteHeadwayConstraint;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraBikeUtilityEstimator;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraCarUtilityEstimator;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraPtUtilityEstimator;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraWalkUtilityEstimator;
import org.eqasim.projects.astra16.mode_choice.predictors.AstraBikePredictor;
import org.eqasim.projects.astra16.mode_choice.predictors.AstraPersonPredictor;
import org.eqasim.projects.astra16.mode_choice.predictors.AstraPtPredictor;
import org.eqasim.projects.astra16.mode_choice.predictors.AstraTripPredictor;
import org.eqasim.projects.astra16.mode_choice.predictors.AstraWalkPredictor;
import org.eqasim.projects.astra16.service_area.ServiceArea;
import org.eqasim.switzerland.mode_choice.SwissModeAvailability;
import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.ovgk.OVGKCalculator;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.TerminationCriterion;
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

		bind(SwissModeParameters.class).to(AstraModeParameters.class);

		bind(SwissModeAvailability.class);
		bindModeAvailability(AstraModeAvailability.NAME).to(AstraModeAvailability.class);

		addEventHandlerBinding().to(ConvergenceListener.class);
		addControlerListenerBinding().to(ConvergenceListener.class);

		bind(TerminationCriterion.class).to(AstraConvergenceCriterion.class);
		addControlerListenerBinding().to(AstraConvergenceCriterion.class);
	}

	@Provides
	@Singleton
	public ConvergenceListener provideConvergenceListener(OutputDirectoryHierarchy outputHierarchy,
			AstraConvergenceCriterion criterion) {
		return new ConvergenceListener(outputHierarchy, criterion);
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

	@Provides
	public AstraModeAvailability provideAstraModeAvailability(AstraConfigGroup astraConfig,
			SwissModeAvailability delegate, ServiceArea serviceArea) {
		boolean useAv = astraConfig.getFleetSize() > 0;
		return new AstraModeAvailability(useAv, delegate, serviceArea);
	}

	@Singleton
	@Provides
	public AstraConvergenceCriterion provideAstraConvergenceCriterion() {
		return new AstraConvergenceCriterion();
	}
}
