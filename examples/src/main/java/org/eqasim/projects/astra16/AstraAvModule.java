package org.eqasim.projects.astra16;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eqasim.automated_vehicles.components.EqasimAvConfigGroup;
import org.eqasim.automated_vehicles.mode_choice.mode_parameters.AvModeParameters;
import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.eqasim.core.analysis.TripListener;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.projects.astra16.mode_choice.AstraAvModeParameters;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraAvUtilityEstimator;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.MainModeIdentifier;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class AstraAvModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public AstraAvModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindUtilityEstimator(AstraAvUtilityEstimator.NAME).to(AstraAvUtilityEstimator.class);
		bind(AvModeParameters.class).to(AstraAvModeParameters.class);
	}

	@Provides
	@Singleton
	public AstraAvModeParameters provideAstraAvModeParameters(EqasimAvConfigGroup config)
			throws IOException, ConfigurationException {
		AstraAvModeParameters parameters = AstraAvModeParameters.buildFrom6Feb2020();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("av-mode-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public TripListener provideTripListener(Network network, MainModeIdentifier mainModeIdentifier,
			PlansCalcRouteConfigGroup routeConfig, PersonAnalysisFilter personFilter) {
		// We override this one, because we want AV as a network mode in here.
		List<String> modes = Arrays.asList("car", "av");
		return new TripListener(network, type -> type.endsWith("interaction"), mainModeIdentifier, modes, personFilter);
	}
}
