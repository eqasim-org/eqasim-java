package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.variables;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.cost.KraussBikeShareCostModel;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.cost.KraussCarCostModel;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.cost.KraussEScooterCostModel;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.cost.KraussPTCostModel;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors.*;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SMMCostParameters;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.parameters.KraussCostParameters;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.parameters.KraussModeParameters;
import org.eqasim.examples.corsica_drt.SMMBaseModeChoice.predictors.*;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.utilities.KraussConstantEstimator;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class KraussConstantModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public static final String MODE_AVAILABILITY_NAME = "IDFModeAvailability";

	public static final String CAR_COST_MODEL_NAME = "IDFCarCostModel";
	public static final String PT_COST_MODEL_NAME = "IDFPtCostModel";

	public static final String CAR_ESTIMATOR_NAME = "IDFCarUtilityEstimator";
	public static final String BIKE_ESTIMATOR_NAME = "ProxyBikeUtilityEstimator";

	public KraussConstantModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		//Bind Parameters
		bind(ModeParameters.class).to(KraussModeParameters.class);

		//Bind Walk
		bindUtilityEstimator("KWalk").to( KraussConstantEstimator.class);
		bind(KraussWalkPredictor.class);


		//Bind bike
		bindUtilityEstimator("KBike").to( KraussConstantEstimator.class);
		bind(KraussBikePredictor.class);

		//Bind Car
		bindUtilityEstimator("KCar").to(KraussConstantEstimator.class);
		bindCostModel("car").to(KraussCarCostModel.class);
		bind(KraussCarPredictor.class);

		//Bind PT
		bindUtilityEstimator("KPT").to(KraussConstantEstimator.class);
		bindCostModel("pt").to(KraussPTCostModel.class);
		bind(KraussPTPredictor.class);

		//Bind BikeShare
		bindUtilityEstimator("sharing:bikeShare").to(KraussConstantEstimator.class);
		bindCostModel("sharing:bikeShare").to(KraussBikeShareCostModel.class);
		bind(KraussBikeSharePredictor.class);

		//Bind eScooter
		bindUtilityEstimator("sharing:eScooter").to(KraussConstantEstimator.class);
		bindCostModel("sharing:eScooter").to(KraussEScooterCostModel.class);
		bind(KraussEScooterPredictor.class);
		bindModeAvailability("KModeAvailability").to(KraussModeAvailability.class);


		bind(KraussPersonPredictor.class);
	}

	@Provides
	@Singleton
	public KraussModeParameters provideModeChoiceParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		KraussModeParameters parameters = KraussModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-choice-parameter", commandLine, parameters);
		return parameters;
	}




	@Provides
	@Singleton
	public KraussBikeShareCostModel provideSharingCostModel(SMMCostParameters parameters) {
		return new KraussBikeShareCostModel(parameters);
	}

	@Provides
	@Singleton
	public KraussCostParameters provideCostParameters(EqasimConfigGroup config) {
		KraussCostParameters parameters = KraussCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public KraussCarCostModel provideCarCostModel(SMMCostParameters parameters) {
		return new KraussCarCostModel(parameters);
	}


	@Provides
	@Singleton
	public KraussPTCostModel providePTCostModel(SMMCostParameters parameters) {
		return new KraussPTCostModel(parameters);
	}
	@Provides
	@Named("sharing:bikeShare")
	public CostModel provideCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
		return getCostModel(factory, config, "sharing:bikeShare");
	}
	@Provides
	@Named("sharing:eScooter")
	public CostModel provideEScooterCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
		return getCostModel(factory, config, "sharing:eScooter");
	}

	@Provides
	@Named("car")
	public CostModel provideKraussCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
		return getCostModel(factory, config, "car");
	}
	@Provides
	@Named("pt")
	public CostModel providePTCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
		return getCostModel(factory, config, "pt");
	}
}
