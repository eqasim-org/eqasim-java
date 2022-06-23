package org.eqasim.examples.Drafts.DGeneralizedMultimodal.sharingPt;

import com.google.common.io.Resources;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;

import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors.*;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.utilities.*;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.costModels.SMMBikeShareCostModel;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SMMCostParameters;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.costModels.SMMMultimodalCostModel;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.estimators.SMMBikeSharePTBikeShareEstimator;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.estimators.SMMBikeSharePTEstimator;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.estimators.SMMPTBikeShareEstimator;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.predictors.SMMBikeSharePTPredictor;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.predictors.SMMBikeSharingPTBikeSharingPredictor;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.predictors.SMMPTBikeSharePredictor;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.cost.KraussBikeShareCostModel;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.cost.KraussCarCostModel;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.cost.KraussPTCostModel;

import org.eqasim.examples.corsica_drt.Drafts.otherDrafts.sharingPt.SharingPTModeAvailability;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SMMParameters;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;

public class SharingPTModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public static final String MODE_AVAILABILITY_NAME = "IDFModeAvailability";

	public static final String CAR_COST_MODEL_NAME = "IDFCarCostModel";
	public static final String PT_COST_MODEL_NAME = "IDFPtCostModel";

	public static final String CAR_ESTIMATOR_NAME = "IDFCarUtilityEstimator";
	public static final String BIKE_ESTIMATOR_NAME = "ProxyBikeUtilityEstimator";
	public Scenario scenario;

	public static void main(String[] args) throws ConfigurationException {
		URL configUrl = Resources.getResource("corsica/corsica_config.xml");

		Config config = ConfigUtils.loadConfig(configUrl, IDFConfigurator.getConfigGroups());
		Scenario scenario = ScenarioUtils.createScenario(config);
		IDFConfigurator.configureScenario(scenario);
		EqasimConfigGroup configGroup=new EqasimConfigGroup();
		CommandLine cmd = new CommandLine.Builder(args) //
				.allowOptions("use-rejection-constraint") //
				.allowPrefixes("mode-parameter", "cost-parameter") //
				.build();
		ScenarioUtils.loadScenario(scenario);

			SharingPTModeChoiceModule module=new SharingPTModeChoiceModule(cmd,scenario);
			SMMBikeShareCostModel try1= module.provideBikeShareCostModel(configGroup,"Perro");
			SMMBikeShareCostModel try2= module.provideBikeShareCostModel(configGroup,"Cat");
			SMMBikeShareCostModel try3= module.provideBikeShareCostModel(configGroup,"Horse");
		SMMParameters params= null;
			try {
			params=module.provideModeChoiceParameters(configGroup);
		} catch (IOException e) {
			e.printStackTrace();
		}
		String xs="x";
	}
	public SharingPTModeChoiceModule(CommandLine commandLine, Scenario scenario) {
		this.commandLine = commandLine;
		this.scenario = scenario;
	}

	@Override
	protected void installEqasimExtension() {
		//Bind Parameters
			bind(ModeParameters.class).to(SMMParameters.class);
		// Create Cost Parameters
		SMMCostParameters kraussCostParameters = null;
		try {
			Class<?> costParamClass=Class.forName("org.eqasim.examples.corsica_drt.SMMBaseModeChoice.parameters.GeneralizedCostParameters");
			Method costConstructor=costParamClass.getMethod("buildDefault");
			kraussCostParameters= (SMMCostParameters) costConstructor.invoke(kraussCostParameters,null);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		// Create Cost Model
		KraussCarCostModel kCarCostModel=null;
		Class carCostModelClass;
		Class[] arguments=new Class[]{SMMCostParameters.class,String.class};
		String mode="carProxy";
		Object[] argumentsInput= new Object[]{kraussCostParameters,mode};
		try {
			carCostModelClass=Class.forName("org.eqasim.examples.SMMFramework.SMMBaseModeChoice.cost.KraussCarCostModel");

			Constructor costConstructor=carCostModelClass.getConstructor(arguments);
			kCarCostModel= (KraussCarCostModel) createObject(costConstructor,argumentsInput);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		// Create CarPredictor
		KraussCarPredictor kCarEstimator=null;
		Class estimatorClass;
		Class[] argumentsEstimator=new Class[]{CostModel.class, SMMParameters.class,Boolean.class};
		Boolean isStatic=true;
		SMMParameters sharingPtParam= SMMParameters.buildDefault();
		Object[] argumentsInputCarPredictor= new Object[]{kCarCostModel,sharingPtParam, true};
		try {
			estimatorClass=Class.forName("org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors.KraussCarPredictor");

			Constructor predictorConstructor=estimatorClass.getConstructor(argumentsEstimator);
			kCarEstimator= (KraussCarPredictor) createObject(predictorConstructor,argumentsInputCarPredictor);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		// Create CarPredictor
		KraussCarEstimator carEstimator=null;
		Class estimatorCarClass;
		Class[] argumentsEst=new Class[]{SMMParameters.class,KraussCarPredictor.class, KraussPersonPredictor.class,Boolean.class};
		KraussPersonPredictor kPersonPred=new KraussPersonPredictor();
		Object[] argumentsInputCarEstimator= new Object[]{sharingPtParam,kCarEstimator,kPersonPred, true};
		try {
			estimatorCarClass=Class.forName("org.eqasim.examples.SMMFramework.SMMBaseModeChoice.utilities.KraussCarEstimator");

			Constructor estimatorConstructor=estimatorCarClass.getConstructor(argumentsEst);
			carEstimator= (KraussCarEstimator) createObject(estimatorConstructor,argumentsInputCarEstimator);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		//Bind Walk
		bindUtilityEstimator("KWalk").to( KraussWalkEstimator.class);
		bind(KraussWalkPredictor.class);


		//Bind bike
		bindUtilityEstimator("KBike").to( KraussBikeEstimator.class);
		bind(KraussBikePredictor.class);

		//Bind Car
		bindUtilityEstimator("KCar").toInstance(carEstimator);
		bindCostModel("car").to(KraussCarCostModel.class);
		bind(KraussCarPredictor.class);

		//Bind PT
		bindUtilityEstimator("KPT").to(KraussPTEstimator.class);
		bindCostModel("pt").to(KraussPTCostModel.class);
		bind(KraussPTPredictor.class);

		//Bind BikeShare

		// Bind Bike Share 2
//		KraussBikeShareCostModel temporalCostModel=provideSharingCostModel(provideCostParameters((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim")));
//		bindCostModel("sharing:bikeShare").toInstance(temporalCostModel);
//		// Bind The Predictor
//		KraussBikeSharePredictor temporalPredictor=provideBikeSharePredictor(temporalCostModel,"sharing:bikeShare");
//		//bind(temporalPredictor);
//		SharingPTParameters tempModeParameters= null;
//		try {
//			tempModeParameters= provideModeChoiceParameters((EqasimConfigGroup)this.scenario.getConfig().getModules().get("eqasim"));
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (ConfigurationException e) {
//			e.printStackTrace();
//		}

//		// Bind Estimtor
//		KraussBikeShareEstimator bikeShareEstimator=provideBikeShareEstimator("sharing:bikeShare",tempModeParameters,temporalPredictor,kPersonPred);
//		bindUtilityEstimator("sharing:bikeShare").toInstance(bikeShareEstimator);
//		bind(KraussBikeSharePredictor.class);
//		bindCostModel("sharing:bikeShare").toInstance(temporalCostModel);
//		//bindCostModel("sharing:bikeShare").to(KraussBikeShareCostModel.class);
//		bind(KraussBikeSharePredictor.class);
//
//		//Bind eScooter
//		bindUtilityEstimator("sharing:eScooter").to(KraussEScooterEstimator.class);
//		bindCostModel("sharing:eScooter").to(KraussEScooterCostModel.class);
//		bind(KraussEScooterPredictor.class);
		bindModeAvailability("KModeAvailability").to(SharingPTModeAvailability.class);

		// Bind the estimators
		// Register the estimator

//
//		bindUtilityEstimator("Sharing_PT").to(SharingPTEstimator.class);
//		bindUtilityEstimator("PT_Sharing").to(PTSharingEstimator.class);
		//bindUtilityEstimator("Sharing_PT_Sharing").to(SharingPTSharingEstimator.class);

		SMMBikeSharePTBikeShareEstimator generalEstimator=null;
		try {
			generalEstimator=this.addSharingServiceSharingPTSharing((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"),this.commandLine,"bikeShare");
		} catch (Exception e) {
			e.printStackTrace();
		}
		bindUtilityEstimator("Sharing_PT_Sharing").toInstance(generalEstimator);
		SMMBikeSharePTEstimator BS_PT_Estimator=null;
		try {
			BS_PT_Estimator=this.addSharingServiceSharingPT((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"),this.commandLine,"bikeShare");
		} catch (Exception e) {
			e.printStackTrace();
		}
		bindUtilityEstimator("Sharing_PT").toInstance(BS_PT_Estimator);
		SMMPTBikeShareEstimator PT_BS_Estimator=null;
		try {
			PT_BS_Estimator=this.addSharingServicePTSharing((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"),this.commandLine,"bikeShare");
		} catch (Exception e) {
			e.printStackTrace();
		}
		bindUtilityEstimator("PT_Sharing").toInstance(PT_BS_Estimator);

		// Bind sharing PT Predictor
//		bind(SharingPTPredictor.class);
//		bind(PTSharingPredictor.class);
//		bind(SharingPTSharingPredictorModifications.class);

		PTStationFinder finder= new PTStationFinder(this.scenario.getTransitSchedule().getFacilities());
		bindTripConstraintFactory("SHARING_PT_CONSTRAINT").toInstance(provideSharingPTTTripConstraint(finder,scenario,"Sharing"));
		bind(KraussPersonPredictor.class);
	}

	@Provides
	@Singleton
	public SMMParameters provideModeChoiceParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		SMMParameters parameters = SMMParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		String x="Uwu";
		return parameters;

	}

	public KraussBikeSharePredictor provideBikeSharePredictor(KraussBikeShareCostModel costModel, String name){
		KraussBikeSharePredictor temporalPredictor= new KraussBikeSharePredictor(costModel, name);
		return temporalPredictor;

	}

	public KraussBikeShareEstimator provideBikeShareEstimator(String name, SMMParameters modeParameters, KraussBikeSharePredictor predictor, KraussPersonPredictor personPredictor){
		KraussBikeShareEstimator temporalEstimator=new KraussBikeShareEstimator(modeParameters,predictor, personPredictor, name);
		return temporalEstimator;

	}


   public SMMBikeShareCostModel provideBikeShareCostModel(EqasimConfigGroup config, String name){
		SMMCostParameters costParameters= provideCostParameters(config);
	   SMMBikeShareCostModel bikeShareCostModel= new SMMBikeShareCostModel(name, costParameters);
	   return(bikeShareCostModel);
   }





	@Provides
	@Singleton
	public KraussBikeShareCostModel provideSharingCostModel(SMMCostParameters parameters) {
		return new KraussBikeShareCostModel(parameters);
	}

	@Provides
	@Singleton
	public SMMCostParameters provideCostParameters(EqasimConfigGroup config) {
		SMMCostParameters parameters = SMMCostParameters.buildDefault();

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

	public void createBikeShareEstimator(CommandLine cmnd){
		SMMCostParameters kraussCostParameters = null;
		try {
			Class<?> costParamClass=Class.forName("org.eqasim.examples.corsica_drt.SMMBaseModeChoice.parameters.GeneralizedCostParameters");
			Method costConstructor=costParamClass.getMethod("buildDefault");
			kraussCostParameters= (SMMCostParameters) costConstructor.invoke(kraussCostParameters,null);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		// Create Cost Model
		KraussCarCostModel kCarCostModel=null;
		Class carCostModelClass;
		Class[] arguments=new Class[]{SMMCostParameters.class,String.class};
		String mode="carProxy";
		Object[] argumentsInput= new Object[]{kraussCostParameters,mode};
		try {
			carCostModelClass=Class.forName("org.eqasim.examples.SMMFramework.SMMBaseModeChoice.cost.KraussCarCostModel");

			Constructor costConstructor=carCostModelClass.getConstructor(arguments);
			kCarCostModel= (KraussCarCostModel) createObject(costConstructor,argumentsInput);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		// Create CarPredictor
		KraussCarPredictor kCarEstimator=null;
		Class estimatorClass;
		Class[] argumentsEstimator=new Class[]{CostModel.class, SMMParameters.class,Boolean.class};
		Boolean isStatic=true;
		SMMParameters sharingPtParam= SMMParameters.buildDefault();
		Object[] argumentsInputCarPredictor= new Object[]{kCarCostModel,sharingPtParam, true};
		try {
			estimatorClass=Class.forName("org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors.KraussCarPredictor");

			Constructor predictorConstructor=estimatorClass.getConstructor(argumentsEstimator);
			kCarEstimator= (KraussCarPredictor) createObject(predictorConstructor,argumentsInputCarPredictor);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		// Create CarPredictor
		KraussCarEstimator carEstimator=null;
		Class estimatorCarClass;
		Class[] argumentsEst=new Class[]{SMMParameters.class,KraussCarPredictor.class, KraussPersonPredictor.class,Boolean.class};
		KraussPersonPredictor kPersonPred=new KraussPersonPredictor();
		Object[] argumentsInputCarEstimator= new Object[]{sharingPtParam,kCarEstimator,kPersonPred, true};
		try {
			estimatorCarClass=Class.forName("org.eqasim.examples.SMMFramework.SMMBaseModeChoice.utilities.KraussCarEstimator");

			Constructor estimatorConstructor=estimatorCarClass.getConstructor(argumentsEst);
			carEstimator= (KraussCarEstimator) createObject(estimatorConstructor,argumentsInputCarEstimator);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}
	private SMMBikeSharePTBikeShareEstimator addSharingServiceSharingPTSharing(EqasimConfigGroup config, CommandLine commandLine, String name) throws Exception {
		SMMCostParameters costParameters= SMMCostParameters.provideCostParameters(config,commandLine);
		SMMMultimodalCostModel costModel=new SMMMultimodalCostModel(name,costParameters);
		SMMBikeSharingPTBikeSharingPredictor predictor =new SMMBikeSharingPTBikeSharingPredictor(costModel,costParameters,name, "bike");
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SMMParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		SMMBikeSharePTBikeShareEstimator estimator=new SMMBikeSharePTBikeShareEstimator(modeParams,predictor,name);
		return estimator;
	}
	private SMMBikeSharePTEstimator addSharingServiceSharingPT(EqasimConfigGroup config, CommandLine commandLine, String name) throws Exception {
		SMMCostParameters costParameters= SMMCostParameters.provideCostParameters(config,commandLine);
		SMMMultimodalCostModel costModel=new SMMMultimodalCostModel(name,costParameters);
		SMMBikeSharePTPredictor predictor = new SMMBikeSharePTPredictor(costModel,costParameters,name, "bike");
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SMMParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		SMMBikeSharePTEstimator estimator= new SMMBikeSharePTEstimator(modeParams,predictor,name);
		return estimator;
	}
	private SMMPTBikeShareEstimator addSharingServicePTSharing(EqasimConfigGroup config, CommandLine commandLine, String name) throws Exception {
		SMMCostParameters costParameters= SMMCostParameters.provideCostParameters(config,commandLine);
		SMMMultimodalCostModel costModel=new SMMMultimodalCostModel(name,costParameters);
		SMMPTBikeSharePredictor predictor = new SMMPTBikeSharePredictor(costModel,costParameters,name, "bike");
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SMMParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		SMMPTBikeShareEstimator estimator= new SMMPTBikeShareEstimator(modeParams,predictor,name);
		return estimator;
	}

//	@Provides
//	@Singleton
	public SharingPTTripConstraint.Factory provideSharingPTTTripConstraint(
			PTStationFinder stationFinder,Scenario scenario,String name) {

		return new SharingPTTripConstraint.Factory(stationFinder, scenario,name);
	}
	public static Object createObject(Constructor constructor,
									  Object[] arguments) {

		System.out.println("Constructor: " + constructor.toString());
		Object object = null;

		try {
			object = constructor.newInstance(arguments);
			System.out.println("Object: " + object.toString());
			return object;
		} catch (InstantiationException e) {
			System.out.println(e);
		} catch (IllegalAccessException e) {
			System.out.println(e);
		} catch (IllegalArgumentException e) {
			System.out.println(e);
		} catch (InvocationTargetException e) {
			System.out.println(e);
		}
		return object;
	}
}
