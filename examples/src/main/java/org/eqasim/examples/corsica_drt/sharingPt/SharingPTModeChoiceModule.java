package org.eqasim.examples.corsica_drt.sharingPt;

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
import org.eqasim.examples.corsica_drt.CleanModeChoice.costs.GeneralizedBikeShareCostModel;
import org.eqasim.examples.corsica_drt.generalizedMicromobility.GeneralizedCostParameters;
import org.eqasim.examples.corsica_drt.generalizedMicromobility.GeneralizedMultimodal.*;
import org.eqasim.examples.corsica_drt.mode_choice.cost.KraussBikeShareCostModel;
import org.eqasim.examples.corsica_drt.mode_choice.cost.KraussCarCostModel;
import org.eqasim.examples.corsica_drt.mode_choice.cost.KraussPTCostModel;

import org.eqasim.examples.corsica_drt.mode_choice.predictors.*;
import org.eqasim.examples.corsica_drt.mode_choice.utilities.*;
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
			GeneralizedBikeShareCostModel try1= module.provideBikeShareCostModel(configGroup,"Perro");
			GeneralizedBikeShareCostModel try2= module.provideBikeShareCostModel(configGroup,"Cat");
			GeneralizedBikeShareCostModel try3= module.provideBikeShareCostModel(configGroup,"Horse");
		SharingPTParameters params= null;
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
			bind(ModeParameters.class).to(SharingPTParameters.class);
		// Create Cost Parameters
		GeneralizedCostParameters kraussCostParameters = null;
		try {
			Class<?> costParamClass=Class.forName("org.eqasim.examples.corsica_drt.mode_choice.parameters.GeneralizedCostParameters");
			Method costConstructor=costParamClass.getMethod("buildDefault");
			kraussCostParameters= (GeneralizedCostParameters) costConstructor.invoke(kraussCostParameters,null);
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
		Class[] arguments=new Class[]{GeneralizedCostParameters.class,String.class};
		String mode="carProxy";
		Object[] argumentsInput= new Object[]{kraussCostParameters,mode};
		try {
			carCostModelClass=Class.forName("org.eqasim.examples.corsica_drt.mode_choice.cost.KraussCarCostModel");

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
		Class[] argumentsEstimator=new Class[]{CostModel.class,SharingPTParameters.class,Boolean.class};
		Boolean isStatic=true;
		SharingPTParameters sharingPtParam=SharingPTParameters.buildDefault();
		Object[] argumentsInputCarPredictor= new Object[]{kCarCostModel,sharingPtParam, true};
		try {
			estimatorClass=Class.forName("org.eqasim.examples.corsica_drt.mode_choice.predictors.KraussCarPredictor");

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
		Class[] argumentsEst=new Class[]{SharingPTParameters.class,KraussCarPredictor.class, KraussPersonPredictor.class,Boolean.class};
		KraussPersonPredictor kPersonPred=new KraussPersonPredictor();
		Object[] argumentsInputCarEstimator= new Object[]{sharingPtParam,kCarEstimator,kPersonPred, true};
		try {
			estimatorCarClass=Class.forName("org.eqasim.examples.corsica_drt.mode_choice.utilities.KraussCarEstimator");

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

		GeneralizedBikeSharePTBikeShareEstimator generalEstimator=null;
		try {
			generalEstimator=this.addSharingServiceSharingPTSharing((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"),this.commandLine,"bikeShare");
		} catch (Exception e) {
			e.printStackTrace();
		}
		bindUtilityEstimator("Sharing_PT_Sharing").toInstance(generalEstimator);
		GeneralizedBikeSharePTEstimator BS_PT_Estimator=null;
		try {
			BS_PT_Estimator=this.addSharingServiceSharingPT((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"),this.commandLine,"bikeShare");
		} catch (Exception e) {
			e.printStackTrace();
		}
		bindUtilityEstimator("Sharing_PT").toInstance(BS_PT_Estimator);
		GeneralizedPTBikeShareEstimator PT_BS_Estimator=null;
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
	public SharingPTParameters provideModeChoiceParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		SharingPTParameters parameters = SharingPTParameters.buildDefault();

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

	public KraussBikeShareEstimator provideBikeShareEstimator(String name, SharingPTParameters modeParameters, KraussBikeSharePredictor predictor, KraussPersonPredictor personPredictor){
		KraussBikeShareEstimator temporalEstimator=new KraussBikeShareEstimator(modeParameters,predictor, personPredictor, name);
		return temporalEstimator;

	}


   public GeneralizedBikeShareCostModel provideBikeShareCostModel(EqasimConfigGroup config, String name){
		GeneralizedCostParameters costParameters= provideCostParameters(config);
	   GeneralizedBikeShareCostModel bikeShareCostModel= new GeneralizedBikeShareCostModel(costParameters,name);
	   return(bikeShareCostModel);
   }





	@Provides
	@Singleton
	public KraussBikeShareCostModel provideSharingCostModel(GeneralizedCostParameters parameters) {
		return new KraussBikeShareCostModel(parameters);
	}

	@Provides
	@Singleton
	public GeneralizedCostParameters provideCostParameters(EqasimConfigGroup config) {
		GeneralizedCostParameters parameters = GeneralizedCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}

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

	public void createBikeShareEstimator(CommandLine cmnd){
		GeneralizedCostParameters kraussCostParameters = null;
		try {
			Class<?> costParamClass=Class.forName("org.eqasim.examples.corsica_drt.mode_choice.parameters.GeneralizedCostParameters");
			Method costConstructor=costParamClass.getMethod("buildDefault");
			kraussCostParameters= (GeneralizedCostParameters) costConstructor.invoke(kraussCostParameters,null);
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
		Class[] arguments=new Class[]{GeneralizedCostParameters.class,String.class};
		String mode="carProxy";
		Object[] argumentsInput= new Object[]{kraussCostParameters,mode};
		try {
			carCostModelClass=Class.forName("org.eqasim.examples.corsica_drt.mode_choice.cost.KraussCarCostModel");

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
		Class[] argumentsEstimator=new Class[]{CostModel.class,SharingPTParameters.class,Boolean.class};
		Boolean isStatic=true;
		SharingPTParameters sharingPtParam=SharingPTParameters.buildDefault();
		Object[] argumentsInputCarPredictor= new Object[]{kCarCostModel,sharingPtParam, true};
		try {
			estimatorClass=Class.forName("org.eqasim.examples.corsica_drt.mode_choice.predictors.KraussCarPredictor");

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
		Class[] argumentsEst=new Class[]{SharingPTParameters.class,KraussCarPredictor.class, KraussPersonPredictor.class,Boolean.class};
		KraussPersonPredictor kPersonPred=new KraussPersonPredictor();
		Object[] argumentsInputCarEstimator= new Object[]{sharingPtParam,kCarEstimator,kPersonPred, true};
		try {
			estimatorCarClass=Class.forName("org.eqasim.examples.corsica_drt.mode_choice.utilities.KraussCarEstimator");

			Constructor estimatorConstructor=estimatorCarClass.getConstructor(argumentsEst);
			carEstimator= (KraussCarEstimator) createObject(estimatorConstructor,argumentsInputCarEstimator);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}
	private GeneralizedBikeSharePTBikeShareEstimator addSharingServiceSharingPTSharing(EqasimConfigGroup config, CommandLine commandLine, String name) throws Exception {
		GeneralizedCostParameters costParameters=GeneralizedCostParameters.provideCostParameters(config,commandLine);
		GeneralizedMultimodalCostModel costModel=new GeneralizedMultimodalCostModel(name,costParameters);
		GeneralizedBikeSharingPTBikeSharingPredictor predictor =new GeneralizedBikeSharingPTBikeSharingPredictor(costModel,costParameters,name, "bike");
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SharingPTParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		GeneralizedBikeSharePTBikeShareEstimator estimator=new GeneralizedBikeSharePTBikeShareEstimator(modeParams,predictor,name);
		return estimator;
	}
	private GeneralizedBikeSharePTEstimator addSharingServiceSharingPT(EqasimConfigGroup config, CommandLine commandLine, String name) throws Exception {
		GeneralizedCostParameters costParameters=GeneralizedCostParameters.provideCostParameters(config,commandLine);
		GeneralizedMultimodalCostModel costModel=new GeneralizedMultimodalCostModel(name,costParameters);
		GeneralizedBikeSharePTPredictor predictor = new GeneralizedBikeSharePTPredictor(costModel,costParameters,name, "bike");
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SharingPTParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		GeneralizedBikeSharePTEstimator estimator= new GeneralizedBikeSharePTEstimator(modeParams,predictor,name);
		return estimator;
	}
	private GeneralizedPTBikeShareEstimator addSharingServicePTSharing(EqasimConfigGroup config, CommandLine commandLine, String name) throws Exception {
		GeneralizedCostParameters costParameters=GeneralizedCostParameters.provideCostParameters(config,commandLine);
		GeneralizedMultimodalCostModel costModel=new GeneralizedMultimodalCostModel(name,costParameters);
		GeneralizedPTBikeSharePredictor predictor = new GeneralizedPTBikeSharePredictor(costModel,costParameters,name, "bike");
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SharingPTParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		GeneralizedPTBikeShareEstimator estimator= new GeneralizedPTBikeShareEstimator(modeParams,predictor,name);
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
