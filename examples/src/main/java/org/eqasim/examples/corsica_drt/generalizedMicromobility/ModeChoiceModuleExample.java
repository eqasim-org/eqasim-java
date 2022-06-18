package org.eqasim.examples.corsica_drt.generalizedMicromobility;

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
import org.eqasim.examples.corsica_drt.mode_choice.cost.KraussPTCostModel;
import org.eqasim.examples.corsica_drt.mode_choice.parameters.KraussCostParameters;
import org.eqasim.examples.corsica_drt.mode_choice.predictors.*;
import org.eqasim.examples.corsica_drt.mode_choice.utilities.*;
import org.eqasim.examples.corsica_drt.sharingPt.SharingPTModeChoiceModule;
import org.eqasim.examples.corsica_drt.sharingPt.SharingPTParameters;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.geotools.feature.type.DateUtil.isEqual;

public class ModeChoiceModuleExample extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public static final String MODE_AVAILABILITY_NAME = "IDFModeAvailability";

	public static final String CAR_COST_MODEL_NAME = "IDFCarCostModel";
	public static final String PT_COST_MODEL_NAME = "IDFPtCostModel";

	public static final String CAR_ESTIMATOR_NAME = "IDFCarUtilityEstimator";
	public static final String BIKE_ESTIMATOR_NAME = "ProxyBikeUtilityEstimator";
	public Scenario scenario;


	public ModeChoiceModuleExample(CommandLine commandLine, Scenario scenario) {
		this.commandLine = commandLine;
		this.scenario = scenario;
	}

	@Override
	protected void installEqasimExtension() {
		//Bind Parameters
			bind(ModeParameters.class).to(SharingPTParameters.class);
		// Create Cost Parameters
		KraussCostParameters kraussCostParameters = null;

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
		bind(KraussPersonPredictor.class);
		bindModeAvailability("GENMODE").to(GeneralizedModeAvailability.class);
	}

	@Provides
	@Singleton
	public SharingPTParameters provideModeChoiceParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		SharingPTParameters parameters = SharingPTParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;

	}

	public KraussBikeSharePredictor provideBikeSharePredictor(KraussBikeShareCostModel costModel, String name){
		KraussBikeSharePredictor temporalPredictor= new KraussBikeSharePredictor(costModel, name);
		return temporalPredictor;

	}

	public KraussBikeShareEstimator provideBikeShareEstimator(String name, SharingPTParameters modeParameters, KraussBikeSharePredictor predictor, KraussPersonPredictor personPredictor){
		KraussBikeShareEstimator temporalEstimator=new KraussBikeShareEstimator(modeParameters,predictor, personPredictor, name);
		return temporalEstimator;

	}


   public GeneralizedBikeShareCostModel provideBikeShareCostModel(EqasimConfigGroup config, String name) throws Exception {
		GeneralizedCostParameters costParameters= provideCostParameters(config);
	   GeneralizedBikeShareCostModel bikeShareCostModel= new GeneralizedBikeShareCostModel(name, costParameters);
	   return(bikeShareCostModel);
   }





	@Provides
	@Singleton
	public KraussBikeShareCostModel provideSharingCostModel(GeneralizedCostParameters parameters) {
		return new KraussBikeShareCostModel(parameters);
	}

//	@Provides
//	@Singleton
//	public KraussCostParameters provideCostParameters(EqasimConfigGroup config) {
//		KraussCostParameters parameters = KraussCostParameters.buildDefault();
//
//		if (config.getCostParametersPath() != null) {
//			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
//		}
//
//		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
//		return parameters;
//	}
	@Provides
	@Singleton
	public  GeneralizedCostParameters provideCostParameters(EqasimConfigGroup config) throws Exception {
		GeneralizedCostParameters parameters = GeneralizedCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		GeneralizedCostParameters.applyCommandLineMicromobility("cost-parameter",commandLine,parameters);
		return parameters;
	}

//	@Provides
//	@Singleton
//	public KraussCarCostModel provideCarCostModel(KraussCostParameters parameters) {
//		return new KraussCarCostModel(parameters);
//	}
	@Provides
	@Singleton
	public KraussCarCostModel provideCarCostModel(GeneralizedCostParameters parameters) {
		return new KraussCarCostModel(parameters);
	}


	@Provides
	@Singleton
	public KraussPTCostModel providePTCostModel(GeneralizedCostParameters parameters) {
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


	public static void validateSharingCostParameters(GeneralizedCostParameters parameterDefinition) throws Exception {
		Set<String> sharingKMCosts= parameterDefinition.sharingMinCosts.keySet();
		Set<String> sharingBookingCosts=  parameterDefinition.sharingBookingCosts.keySet();
		boolean isEqual = isEqual(sharingBookingCosts,sharingKMCosts);
		if (isEqual==false) {
			throw new  IllegalArgumentException(" One of the sharing modes does not have booking or km cost");
		}
	}


//	@Provides
//	@Singleton
	public static GeneralizedCostParameters provideCostParameters(EqasimConfigGroup config, CommandLine commandLine) throws Exception {
		GeneralizedCostParameters parameters = GeneralizedCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		GeneralizedCostParameters.applyCommandLineMicromobility("cost-parameter",commandLine,parameters);
		return parameters;
	}
	private  GeneralizedBikeShareEstimator addSharingServiceToEqasim(EqasimConfigGroup config,CommandLine commandLine, String name) throws Exception {
		GeneralizedCostParameters costParameters=GeneralizedCostParameters.provideCostParameters(config,commandLine);
		org.eqasim.examples.corsica_drt.generalizedMicromobility.GeneralizedBikeShareCostModel costModel=new org.eqasim.examples.corsica_drt.generalizedMicromobility.GeneralizedBikeShareCostModel(name,costParameters);
		GeneralizedBikeSharePredictor bikePredictor=new GeneralizedBikeSharePredictor(costModel,name);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SharingPTParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		GeneralizedBikeShareEstimator bikeEstimator=new GeneralizedBikeShareEstimator(modeParams,bikePredictor,personPredictor,name);
		return bikeEstimator;
	}


}
