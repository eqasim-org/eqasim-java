package org.eqasim.examples.corsica_carpooling;

import java.io.File;
import java.util.Map;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.examples.corsica_carpooling.mode_choice.CarpoolingModeAvailability;
import org.eqasim.examples.corsica_carpooling.mode_choice.cost.CarpoolingCostModel;
import org.eqasim.examples.corsica_carpooling.mode_choice.parameters.CarpoolingCostParameters;
import org.eqasim.examples.corsica_carpooling.mode_choice.parameters.CarpoolingModeParameters;
import org.eqasim.examples.corsica_carpooling.mode_choice.utilities.CarpoolingPredictor;
import org.eqasim.examples.corsica_carpooling.mode_choice.utilities.CarpoolingUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.matsim.core.config.CommandLine;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class CarpoolingModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public CarpoolingModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		// Configure mode availability
		bindModeAvailability(CarpoolingModeAvailability.NAME).to(CarpoolingModeAvailability.class);

		// Configure choice alternative for DRT
		bindUtilityEstimator("carpooling").to(CarpoolingUtilityEstimator.class);
		bindCostModel("carpooling").to(CarpoolingCostModel.class);
		bind(CarpoolingPredictor.class);

		// Override parameter bindings
		bind(ModeParameters.class).to(CarpoolingModeParameters.class);
		bind(IDFModeParameters.class).to(CarpoolingModeParameters.class);
		bind(IDFCostParameters.class).to(CarpoolingCostParameters.class);

		addTravelTimeBinding("carpooling").to(Key.get(TravelTime.class, Names.named("car")));
	}

	@Provides
	@Singleton
	public CarpoolingCostModel provideDrtCostModel(CarpoolingCostParameters parameters) {
		return new CarpoolingCostModel(parameters);
	}

	@Provides
	@Singleton
	public CarpoolingCostParameters provideCostParameters(EqasimConfigGroup config) {
		CarpoolingCostParameters parameters = CarpoolingCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public CarpoolingModeParameters provideModeParameters(EqasimConfigGroup config) {
		CarpoolingModeParameters parameters = CarpoolingModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Named("carpooling")
	public CostModel provideCarpoolingCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
		return getCostModel(factory, config, "carpooling");
	}

}
