package org.eqasim.examples.corsica_drt.sharingPt;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.examples.corsica_drt.mode_choice.cost.KraussBikeShareCostModel;
import org.eqasim.examples.corsica_drt.mode_choice.cost.KraussCarCostModel;
import org.eqasim.examples.corsica_drt.mode_choice.cost.KraussEScooterCostModel;
import org.eqasim.examples.corsica_drt.mode_choice.cost.KraussPTCostModel;
import org.eqasim.examples.corsica_drt.mode_choice.parameters.KraussCostParameters;
import org.eqasim.examples.corsica_drt.mode_choice.predictors.*;
import org.eqasim.examples.corsica_drt.mode_choice.utilities.*;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class SharingPTModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public static final String MODE_AVAILABILITY_NAME = "IDFModeAvailability";

	public static final String CAR_COST_MODEL_NAME = "IDFCarCostModel";
	public static final String PT_COST_MODEL_NAME = "IDFPtCostModel";

	public static final String CAR_ESTIMATOR_NAME = "IDFCarUtilityEstimator";
	public static final String BIKE_ESTIMATOR_NAME = "ProxyBikeUtilityEstimator";
	public Scenario scenario;
	public SharingPTModeChoiceModule(CommandLine commandLine, Scenario scenario) {
		this.commandLine = commandLine;
		this.scenario = scenario;
	}

	@Override
	protected void installEqasimExtension() {
		//Bind Parameters
			bind(ModeParameters.class).to(SharingPTParameters.class);

		//Bind Walk
		bindUtilityEstimator("KWalk").to( KraussWalkEstimator.class);
		bind(KraussWalkPredictor.class);


		//Bind bike
		bindUtilityEstimator("KBike").to( KraussBikeEstimator.class);
		bind(KraussBikePredictor.class);

		//Bind Car
		bindUtilityEstimator("KCar").to(KraussCarEstimator.class);
		bindCostModel("car").to(KraussCarCostModel.class);
		bind(KraussCarPredictor.class);

		//Bind PT
		bindUtilityEstimator("KPT").to(KraussPTEstimator.class);
		bindCostModel("pt").to(KraussPTCostModel.class);
		bind(KraussPTPredictor.class);

		//Bind BikeShare
		bindUtilityEstimator("sharing:bikeShare").to(KraussBikeEstimator.class);
		bindCostModel("sharing:bikeShare").to(KraussBikeShareCostModel.class);
		bind(KraussBikeSharePredictor.class);

		//Bind eScooter
		bindUtilityEstimator("sharing:eScooter").to(KraussEScooterEstimator.class);
		bindCostModel("sharing:eScooter").to(KraussEScooterCostModel.class);
		bind(KraussEScooterPredictor.class);
		bindModeAvailability("KModeAvailability").to(SharingPTModeAvailability.class);

		// Bind the estimators
		// Register the estimator
		bindUtilityEstimator("Sharing_PT").to(SharingPTEstimator.class);
		bindUtilityEstimator("PT_Sharing").to(PTSharingEstimator.class);
		bindUtilityEstimator("Sharing_PT_Sharing").to(SharingPTSharingEstimator.class);
		// Bind sharing PT Predictor
		bind(SharingPTPredictor.class);
		bind(PTSharingPredictor.class);
		bind(SharingPTSharingPredictor.class);


		bindTripConstraintFactory("SHARING_PT_CONSTRAINT").to(SharingPTTripConstraint.Factory.class);
		bind(KraussPersonPredictor.class);
	}

	@Provides
	@Singleton
	public SharingPTParameters provideModeChoiceParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		SharingPTParameters parameters = SharingPTParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-choice-parameter", commandLine, parameters);
		return parameters;
	}




	@Provides
	@Singleton
	public KraussBikeShareCostModel provideSharingCostModel(KraussCostParameters parameters) {
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
	public KraussCarCostModel provideCarCostModel(KraussCostParameters parameters) {
		return new KraussCarCostModel(parameters);
	}


	@Provides
	@Singleton
	public KraussPTCostModel providePTCostModel(KraussCostParameters parameters) {
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

	@Provides
	@Singleton
	public SharingPTTripConstraint.Factory provideSharingPTTTripConstraint(
			PTStationFinder stationFinder,Scenario scenario) {

		return new SharingPTTripConstraint.Factory(stationFinder, scenario);
	}
}
