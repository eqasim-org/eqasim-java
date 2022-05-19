package org.eqasim.examples.corsica_drt.generalizedMicromobility;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.examples.corsica_drt.generalizedMicromobility.GeneralizedMultimodal.*;
import org.eqasim.examples.corsica_drt.mode_choice.cost.KraussBikeShareCostModel;
import org.eqasim.examples.corsica_drt.mode_choice.predictors.KraussBikeSharePredictor;
import org.eqasim.examples.corsica_drt.mode_choice.predictors.KraussPersonPredictor;
import org.eqasim.examples.corsica_drt.mode_choice.utilities.KraussBikeShareEstimator;
import org.eqasim.examples.corsica_drt.sharingPt.PTStationFinder;
import org.eqasim.examples.corsica_drt.sharingPt.SharingPTModeChoiceModule;
import org.eqasim.examples.corsica_drt.sharingPt.SharingPTParameters;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.sharing.io.DefaultSharingServiceSpecification;
import org.matsim.contrib.sharing.io.SharingServiceReader;
import org.matsim.contrib.sharing.io.SharingServiceSpecification;
import org.matsim.contrib.sharing.routing.InteractionFinder;
import org.matsim.contrib.sharing.routing.StationBasedInteractionFinder;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigGroup;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.geotools.feature.type.DateUtil.isEqual;

public class MicroMobilityModeEqasimModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public static final String MODE_AVAILABILITY_NAME = "IDFModeAvailability";

	public static final String CAR_COST_MODEL_NAME = "IDFCarCostModel";
	public static final String PT_COST_MODEL_NAME = "IDFPtCostModel";

	public static final String CAR_ESTIMATOR_NAME = "IDFCarUtilityEstimator";
	public static final String BIKE_ESTIMATOR_NAME = "ProxyBikeUtilityEstimator";
	public Scenario scenario;
	public String name;
	public String serviceFile;

//	public static void main(String[] args) throws ConfigurationException {
//		URL configUrl = Resources.getResource("corsica/corsica_config.xml");
//
//		Config config = ConfigUtils.loadConfig(configUrl, IDFConfigurator.getConfigGroups());
//		Scenario scenario = ScenarioUtils.createScenario(config);
//		IDFConfigurator.configureScenario(scenario);
//		EqasimConfigGroup configGroup=new EqasimConfigGroup();
//		CommandLine cmd = new CommandLine.Builder(args) //
//				.allowOptions("use-rejection-constraint") //
//				.allowPrefixes("mode-parameter", "cost-parameter") //
//				.build();
//		ScenarioUtils.loadScenario(scenario);
//
//			//MicroMobilityModeEqasimModeChoiceModule module=new MicroMobilityModeEqasimModeChoiceModule(cmd,scenario);
//			GeneralizedBikeShareCostModel try1= module.provideBikeShareCostModel(configGroup,"Perro");
//			GeneralizedBikeShareCostModel try2= module.provideBikeShareCostModel(configGroup,"Cat");
//			GeneralizedBikeShareCostModel try3= module.provideBikeShareCostModel(configGroup,"Horse");
//		SharingPTParameters params= null;
//			try {
//			params=module.provideModeChoiceParameters(configGroup);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		String xs="x";
//	}
	public MicroMobilityModeEqasimModeChoiceModule(CommandLine commandLine, Scenario scenario,String name ,String serviceFile) {
		this.commandLine = commandLine;
		this.scenario = scenario;
		this.name=name;
		this.serviceFile=serviceFile;
	}



	@Override
	protected void installEqasimExtension() {
		//Bind Parameters
			bind(ModeParameters.class).to(SharingPTParameters.class);
		// Create Cost Parameters
//
		GeneralizedBikeShareEstimator generalEstimator=null;
		try {
			generalEstimator=this.addSharingServiceToEqasim((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"),this.commandLine,name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		bindUtilityEstimator("sharing:"+name).toInstance(generalEstimator);
		try {
			bindCostModel("sharing:"+name).toInstance(new org.eqasim.examples.corsica_drt.generalizedMicromobility.GeneralizedBikeShareCostModel("sharing:bikeShare",this.provideCostParameters((EqasimConfigGroup)this.scenario.getConfig().getModules().get("eqasim"),this.commandLine)));
		} catch (Exception e) {
			e.printStackTrace();
		}
//
		bindModeAvailability("KModeAvailability").toInstance(provideModeAvailability());

		Map<String,String>modesByCommand=MicroMobilityModeEqasimModeChoiceModule.processCommandLine(this.commandLine,"sharing-mode-name");
		for (Map.Entry<String, String> entry :modesByCommand.entrySet()) {
			String option = entry.getKey();
			String value = entry.getValue();
			try {
				String[] parts = option.split("\\.");// divide el punto
				if (parts.length != 1) {
					if(parts[1].equals("Intermodal")){
						GeneralizedBikeSharePTBikeShareEstimator BS_PT_BS=null;
						try {
							BS_PT_BS=this.addSharingServiceSharingPTSharing((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"),this.commandLine,"bikeShare");
						} catch (Exception e) {
							e.printStackTrace();
						}
						bindUtilityEstimator(parts[0]+"_PT_"+parts[0]).toInstance(generalEstimator);
						GeneralizedBikeSharePTEstimator BS_PT_Estimator=null;
						try {
							BS_PT_Estimator=this.addSharingServiceSharingPT((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"),this.commandLine,"bikeShare");
						} catch (Exception e) {
							e.printStackTrace();
						}
						bindUtilityEstimator(parts[0]+"_PT").toInstance(BS_PT_Estimator);
						GeneralizedPTBikeShareEstimator PT_BS_Estimator=null;
						try {
							PT_BS_Estimator=this.addSharingServicePTSharing((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"),this.commandLine,"bikeShare");
						} catch (Exception e) {
							e.printStackTrace();
						}
						bindUtilityEstimator("PT_"+parts[0]).toInstance(PT_BS_Estimator);
					}
				}
			}catch (Exception e){}
		}
		PTStationFinder finder= new PTStationFinder(this.scenario.getTransitSchedule().getFacilities());
		bindTripConstraintFactory("SHARING_PT_CONSTRAINT").toInstance(provideSharingPTTTripConstraint(finder,scenario,name,serviceFile));
		bind(KraussPersonPredictor.class);
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
	   GeneralizedCostParameters costParameters= null;
	   try {
		   costParameters = provideCostParameters(config,this.commandLine);
	   } catch (Exception e) {
		   e.printStackTrace();
	   }
	   GeneralizedBikeShareCostModel bikeShareCostModel= new GeneralizedBikeShareCostModel(name, costParameters);
	   return(bikeShareCostModel);
   }




	public static void validateSharingCostParameters(GeneralizedCostParameters parameterDefinition) throws Exception {
		Set<String> sharingKMCosts= parameterDefinition.sharingKMCosts.keySet();
		Set<String> sharingBookingCosts=  parameterDefinition.sharingBookingCosts.keySet();
		boolean isEqual = isEqual(sharingBookingCosts,sharingKMCosts);
		if (isEqual==false) {
			throw new  IllegalArgumentException(" One of the sharing modes does not have booking or km cost");
		}
	}


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
		GeneralizedBikeSharePredictor bikePredictor=new GeneralizedBikeSharePredictor(costModel,"sharing:"+name);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SharingPTParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		GeneralizedBikeShareEstimator bikeEstimator=new GeneralizedBikeShareEstimator(modeParams,bikePredictor,personPredictor,name);
		return bikeEstimator;
	}

//	@Inject
//	public KraussPTCostModel provideCostModel(KraussCostParameters costParams){
//		return  new  KraussPTCostModel(costParams);
//
//	}

	private GeneralizedBikeSharePTBikeShareEstimator addSharingServiceSharingPTSharing(EqasimConfigGroup config, CommandLine commandLine, String name) throws Exception {
		GeneralizedCostParameters costParameters=GeneralizedCostParameters.provideCostParameters(config,commandLine);
		GeneralizedMultimodalCostModel costModel=new GeneralizedMultimodalCostModel(name,costParameters);
		GeneralizedBikeSharingPTBikeSharingPredictor predictor =new GeneralizedBikeSharingPTBikeSharingPredictor(costModel,costParameters,name, "Shared-Bike");
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SharingPTParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		GeneralizedBikeSharePTBikeShareEstimator estimator=new GeneralizedBikeSharePTBikeShareEstimator(modeParams,predictor,name);
		return estimator;
	}
	private GeneralizedBikeSharePTEstimator addSharingServiceSharingPT(EqasimConfigGroup config, CommandLine commandLine, String name) throws Exception {
		GeneralizedCostParameters costParameters=GeneralizedCostParameters.provideCostParameters(config,commandLine);
		GeneralizedMultimodalCostModel costModel=new GeneralizedMultimodalCostModel(name,costParameters);
		GeneralizedBikeSharePTPredictor predictor = new GeneralizedBikeSharePTPredictor(costModel,costParameters,name, "Shared-Bike");
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SharingPTParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		GeneralizedBikeSharePTEstimator estimator= new GeneralizedBikeSharePTEstimator(modeParams,predictor,name);
		return estimator;
	}
	private GeneralizedPTBikeShareEstimator addSharingServicePTSharing(EqasimConfigGroup config, CommandLine commandLine, String name) throws Exception {
		GeneralizedCostParameters costParameters=GeneralizedCostParameters.provideCostParameters(config,commandLine);
		GeneralizedMultimodalCostModel costModel=new GeneralizedMultimodalCostModel(name,costParameters);
		GeneralizedPTBikeSharePredictor predictor = new GeneralizedPTBikeSharePredictor(costModel,costParameters,name, "Shared-Bike");
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SharingPTParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		GeneralizedPTBikeShareEstimator estimator= new GeneralizedPTBikeShareEstimator(modeParams,predictor,name);
		return estimator;
	}

	public static Map<String,String> processCommandLine(CommandLine cmd , String prefix) {
		Map<String, String> values = new HashMap<>();
		for (String option : cmd.getAvailableOptions()) {
			if (option.startsWith(prefix + ":")) {
				try {
					values.put(option.split(":")[1], cmd.getOptionStrict(option));
				} catch (CommandLine.ConfigurationException e) {
					e.printStackTrace();
				}
			}

		}
		return values;
	}
	public GeneralizedStationBasedConstraint.Factory provideSharingPTTTripConstraint(
			PTStationFinder stationFinder, Scenario scenario, String name, String serviceFile) {
		SharingServiceSpecification specification = new DefaultSharingServiceSpecification();
		new SharingServiceReader(specification).readURL(
				ConfigGroup.getInputFileURL(getConfig().getContext(), serviceFile));
		InteractionFinder interactionFinder= new StationBasedInteractionFinder(scenario.getNetwork(),specification,1000);
		return new GeneralizedStationBasedConstraint.Factory(stationFinder, scenario,name,interactionFinder);
	}

	public GeneralizedModeAvailability provideModeAvailability(){
		GeneralizedModeAvailability modeAvailability=GeneralizedModeAvailability.buildDefault();
		try {
			GeneralizedModeAvailability.applyCommandLineAvailability("sharing-mode-name",commandLine,modeAvailability);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return modeAvailability;
	}


//	@Provides
//	@Singleton
//	public static GeneralizedModeAvailability provideModeAvailability(EqasimConfigGroup config, CommandLine commandLine) throws Exception {
//		GeneralizedCostParameters parameters = GeneralizedCostParameters.buildDefault();
//
//		if (config.getCostParametersPath() != null) {
//			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
//		}
//
//		GeneralizedCostParameters.applyCommandLineMicromobility("cost-parameter",commandLine,parameters);
//		return parameters;
//	}
//

}
