package org.eqasim.examples.corsica_drt;

import java.io.File;
import java.util.Map;

import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.examples.corsica_drt.mode_choice.CorsicaDrtModeAvailability;
import org.eqasim.examples.corsica_drt.mode_choice.cost.DrtCostModel;
import org.eqasim.examples.corsica_drt.mode_choice.parameters.CorsicaDrtCostParameters;
import org.eqasim.examples.corsica_drt.mode_choice.parameters.CorsicaDrtModeParameters;
import org.eqasim.examples.corsica_drt.mode_choice.utilities.DrtPredictor;
import org.eqasim.examples.corsica_drt.mode_choice.utilities.DrtUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.matsim.core.config.CommandLine;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class CorsicaDrtModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public CorsicaDrtModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		// Configure mode availability
		bindModeAvailability(CorsicaDrtModeAvailability.NAME).to(CorsicaDrtModeAvailability.class);

		// Configure choice alternative for DRT
		bindUtilityEstimator("drt").to(DrtUtilityEstimator.class);
		bindCostModel("drt").to(DrtCostModel.class);
		bind(DrtPredictor.class);

		// Define filter for trip analysis
		bind(PersonAnalysisFilter.class).to(DrtPersonAnalysisFilter.class);

		// Override parameter bindings
		bind(ModeParameters.class).to(CorsicaDrtModeParameters.class);
		bind(IDFModeParameters.class).to(CorsicaDrtModeParameters.class);
		bind(IDFCostParameters.class).to(CorsicaDrtCostParameters.class);
	}

	@Provides
	@Singleton
	public DrtCostModel provideDrtCostModel(CorsicaDrtCostParameters parameters) {
		return new DrtCostModel(parameters);
	}

	@Provides
	@Singleton
	public CorsicaDrtCostParameters provideCostParameters(EqasimConfigGroup config) {
		CorsicaDrtCostParameters parameters = CorsicaDrtCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public CorsicaDrtModeParameters provideModeParameters(EqasimConfigGroup config) {
		CorsicaDrtModeParameters parameters = CorsicaDrtModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Named("drt")
	public CostModel provideCarCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
		return getCostModel(factory, config, "drt");
	}

}
