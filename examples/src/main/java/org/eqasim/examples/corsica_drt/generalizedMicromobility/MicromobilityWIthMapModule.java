package org.eqasim.examples.corsica_drt.generalizedMicromobility;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.examples.corsica_drt.GBFSUtils.SharingPTTripWriter;
import org.eqasim.examples.corsica_drt.generalizedMicromobility.GeneralizedMultimodal.*;
import org.eqasim.examples.corsica_drt.mode_choice.cost.KraussBikeShareCostModel;
import org.eqasim.examples.corsica_drt.mode_choice.predictors.KraussBikeSharePredictor;
import org.eqasim.examples.corsica_drt.mode_choice.predictors.KraussPersonPredictor;
import org.eqasim.examples.corsica_drt.mode_choice.utilities.KraussBikeShareEstimator;
import org.eqasim.examples.corsica_drt.sharingPt.*;
import org.matsim.analysis.TripsAndLegsCSVWriter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.sharing.io.DefaultSharingServiceSpecification;
import org.matsim.contrib.sharing.io.SharingServiceReader;
import org.matsim.contrib.sharing.io.SharingServiceSpecification;
import org.matsim.contrib.sharing.routing.InteractionFinder;
import org.matsim.contrib.sharing.routing.StationBasedInteractionFinder;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.pt.transitSchedule.TransitScheduleUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.geotools.feature.type.DateUtil.isEqual;

public class MicromobilityWIthMapModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;
	public Scenario scenario;
	public HashMap<String, String> service;


	public MicromobilityWIthMapModule(CommandLine commandLine, Scenario scenario, HashMap<String, String> service) {
		this.commandLine = commandLine;
		this.scenario = scenario;
		this.service = service;
	}


	@Override
	protected void installEqasimExtension() {
		//Bind Parameters
		bind(ModeParameters.class).to(SharingPTParameters.class);
		// Create Cost Parameters
		String name = service.get("Service_Name");
		String mode = service.get("Mode");
		bind(TripsAndLegsCSVWriter.CustomTripsWriterExtension.class).to(SharingPTTripWriter.class).asEagerSingleton();

//		GeneralizedBikeShareEstimator generalEstimator = null;
//		try {
//			generalEstimator = this.addSharingServiceToEqasimBS((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		bindUtilityEstimator("sharing:" + name).toInstance(generalEstimator);
//		try {
//			bindCostModel("sharing:" + name).toInstance(new GeneralizedBikeShareCostModel("sharing:" + name, this.provideCostParameters((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine)));
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
////
//		bindModeAvailability(name + "ModeAvailability").toInstance(provideModeAvailability());
//		if (service.get("Multimodal").equals("Yes")) {
//			if (mode.equals("eScooter")) {
//				GeneralizedEScooterPTEScooterEstimator ES_PT_ES = null;
//				try {
//					ES_PT_ES = this.addSharingServiceSharingPTSharingEscooter((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name, mode);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				bindUtilityEstimator(name + "_PT_" + name).toInstance(ES_PT_ES);
//				GeneralizedEScooterPTEstimator ES_PT_Estimator = null;
//				try {
//					ES_PT_Estimator = this.addSharingServiceSharingPTEScooter((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name, mode);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				bindUtilityEstimator(name + "_PT").toInstance(ES_PT_Estimator);
//				GeneralizedPTEScooterEstimator PT_ES_Estimator = null;
//				try {
//					PT_ES_Estimator = this.addSharingServicePTSharingEScooter((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name, mode);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				bindUtilityEstimator("PT_" + name).toInstance(PT_ES_Estimator);
//			}
//			if (service.get("Scheme").equals("Station Based")) {
//				PTStationFinder finder = new PTStationFinder(this.scenario.getTransitSchedule().getFacilities());
//				SharingServiceSpecification specification = new DefaultSharingServiceSpecification();
//				new SharingServiceReader(specification).readURL(
//						ConfigGroup.getInputFileURL(getConfig().getContext(), service.get("Service_File")));
//				InteractionFinder interactionFinder = new StationBasedInteractionFinder(scenario.getNetwork(), specification, Double.parseDouble(service.get("AccessEgress_Distance")));
//				bindTripConstraintFactory(name + "_PT_CONSTRAINT").toInstance(provideSharingPTTTripConstraint(finder, scenario, name, interactionFinder));
//			} else {
//				PTStationFinder finder = new PTStationFinder(this.scenario.getTransitSchedule().getFacilities());
//				bindTripConstraintFactory(name + "_PT_CONSTRAINT").toInstance(provideSharingPTTTripConstraint(finder, scenario, name));
//			}
//			bind(KraussPersonPredictor.class);

		if(mode.equals("Shared-Bike")){
			bindBikeShareService(name,mode);
		}else if(mode.equals("eScooter")){
			bindeScooterService(name,mode);
		}
//
//		Map<String,String>modesByCommand= MicromobilityWIthMapModule.processCommandLine(this.commandLine,"sharing-mode-name");
//		for (Map.Entry<String, String> entry :modesByCommand.entrySet()) {
//			String option = entry.getKey();
//			String value = entry.getValue();
//			try {
//				String[] parts = option.split("\\.");
//				if (parts.length != 1) {
//					if(parts[1].equals("Intermodal")){
//						GeneralizedBikeSharePTBikeShareEstimator BS_PT_BS=null;
//						try {
//							BS_PT_BS=this.addSharingServiceSharingPTSharing((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"),this.commandLine,"bikeShare","Bike-Share");
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//						bindUtilityEstimator(parts[0]+"_PT_"+parts[0]).toInstance(generalEstimator);
//						GeneralizedBikeSharePTEstimator BS_PT_Estimator=null;
//						try {
//							BS_PT_Estimator=this.addSharingServiceSharingPT((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"),this.commandLine,"bikeShare","Bike-Share");
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//						bindUtilityEstimator(parts[0]+"_PT").toInstance(BS_PT_Estimator);
//						GeneralizedPTBikeShareEstimator PT_BS_Estimator=null;
//						try {
//							PT_BS_Estimator=this.addSharingServicePTSharing((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"),this.commandLine,"bikeShare","Bike-Share");
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//						bindUtilityEstimator("PT_"+parts[0]).toInstance(PT_BS_Estimator);
//						PTStationFinder finder= new PTStationFinder(this.scenario.getTransitSchedule().getFacilities());
//						SharingServiceSpecification specification = new DefaultSharingServiceSpecification();
//						new SharingServiceReader(specification).readURL(
//								ConfigGroup.getInputFileURL(getConfig().getContext(), service.get("Service_File")));
//						InteractionFinder interactionFinder= new StationBasedInteractionFinder(scenario.getNetwork(),specification,1000);
//						bindTripConstraintFactory(parts[0]+"_PT_CONSTRAINT").toInstance(provideSharingPTTTripConstraint(finder,scenario,name,interactionFinder));
//
//					}
//				}
//			}catch (Exception e){}
//		}
//
//		bind(KraussPersonPredictor.class);
	}

	public void bindBikeShareService(String name, String mode) {

		GeneralizedBikeShareEstimator generalEstimator = null;
		try {
			generalEstimator = this.addSharingServiceToEqasimBS((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		bindUtilityEstimator("sharing:" + name).toInstance(generalEstimator);
		try {
			bindCostModel("sharing:" + name).toInstance(new GeneralizedBikeShareCostModel("sharing:" + name, this.provideCostParameters((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine)));
		} catch (Exception e) {
			e.printStackTrace();
		}
//
		bindTripConstraintFactory(name).toInstance(provideSMMConstraint(mode, name));
		bindModeAvailability(name + "ModeAvailability").toInstance(provideModeAvailability());
		if (service.get("Multimodal").equals("Yes")) {
//			if(mode.equals("eScooter")){
			GeneralizedBikeSharePTBikeShareEstimator BS_PT_BS = null;
			try {
				BS_PT_BS = this.addSharingServiceSharingPTSharing((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name, mode);
			} catch (Exception e) {
				e.printStackTrace();
			}
			bindUtilityEstimator(name + "_PT_" + name).toInstance(BS_PT_BS);
			GeneralizedBikeSharePTEstimator BS_PT_Estimator = null;
			try {
				BS_PT_Estimator = this.addSharingServiceSharingPT((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name, mode);
			} catch (Exception e) {
				e.printStackTrace();
			}
			bindUtilityEstimator(name + "_PT").toInstance(BS_PT_Estimator);
			GeneralizedPTBikeShareEstimator PT_BS_Estimator = null;
			try {
				PT_BS_Estimator = this.addSharingServicePTSharing((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name, mode);
			} catch (Exception e) {
				e.printStackTrace();
			}
			bindUtilityEstimator("PT_" + name).toInstance(PT_BS_Estimator);
		}
		if (service.get("Scheme").equals("Station-Based")) {
//			PTStationFinder finder = new PTStationFinder(this.scenario.getTransitSchedule().getFacilities());
			SharingServiceSpecification specification = new DefaultSharingServiceSpecification();
			new SharingServiceReader(specification).readURL(
					ConfigGroup.getInputFileURL(getConfig().getContext(), service.get("Service_File")));
			QuadTree<TransitStopFacility>  stopsFacilitiesQT= TransitScheduleUtils.createQuadTreeOfTransitStopFacilities(this.scenario.getTransitSchedule());
			PTStationFinder2 finder = new PTStationFinder2(stopsFacilitiesQT);
			InteractionFinder interactionFinder = new StationBasedInteractionFinder(scenario.getNetwork(), specification, Double.parseDouble(service.get("AccessEgress_Distance")));
			bindTripConstraintFactory(name + "_PT_CONSTRAINT").toInstance(provideSharingPTTTripConstraint2(finder, scenario, name, interactionFinder));
		} else {
//			PTStationFinder finder = new PTStationFinder(this.scenario.getTransitSchedule().getFacilities());
			QuadTree<TransitStopFacility>  stopsFacilitiesQT= TransitScheduleUtils.createQuadTreeOfTransitStopFacilities(this.scenario.getTransitSchedule());
			PTStationFinder2 finder = new PTStationFinder2(stopsFacilitiesQT);
			bindTripConstraintFactory(name + "_PT_CONSTRAINT").toInstance(provideSharingPTTTripConstraint2(finder, scenario, name));
		}
		bind(KraussPersonPredictor.class);
//		}
	}
	public void bindeScooterService(String name, String mode) {

		GeneralizedEScooterEstimator generalEstimator = null;
		try {
			generalEstimator = this.addSharingServiceToEqasimEscooter((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		bindUtilityEstimator("sharing:" + name).toInstance(generalEstimator);
		try {
			bindCostModel("sharing:" + name).toInstance(new GeneralizedEScooterCostModel("sharing:" + name, this.provideCostParameters((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine)));
		} catch (Exception e) {
			e.printStackTrace();
		}
//
		bindTripConstraintFactory(name).toInstance(provideSMMConstraint(mode, name));
		bindModeAvailability(name + "ModeAvailability").toInstance(provideModeAvailability());
		if (service.get("Multimodal").equals("Yes")) {
//			if(mode.equals("eScooter")){
			GeneralizedEScooterPTEScooterEstimator ES_PT_ES = null;
			try {
				ES_PT_ES = this.addSharingServiceSharingPTSharingEscooter((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name, mode);
			} catch (Exception e) {
				e.printStackTrace();
			}
			bindUtilityEstimator(name + "_PT_" + name).toInstance(ES_PT_ES);
			GeneralizedEScooterPTEstimator ES_PT_Estimator = null;
			try {
				ES_PT_Estimator = this.addSharingServiceSharingPTEScooter((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name, mode);
			} catch (Exception e) {
				e.printStackTrace();
			}
			bindUtilityEstimator(name + "_PT").toInstance(ES_PT_Estimator);
			GeneralizedPTEScooterEstimator PT_ES_Estimator = null;
			try {
				PT_ES_Estimator = this.addSharingServicePTSharingEScooter((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name, mode);
			} catch (Exception e) {
				e.printStackTrace();
			}
			bindUtilityEstimator("PT_" + name).toInstance(PT_ES_Estimator);
		}
		if (service.get("Scheme").equals("Station-Based")) {
//			PTStationFinder finder = new PTStationFinder(this.scenario.getTransitSchedule().getFacilities());
			SharingServiceSpecification specification = new DefaultSharingServiceSpecification();
			new SharingServiceReader(specification).readURL(
					ConfigGroup.getInputFileURL(getConfig().getContext(), service.get("Service_File")));

			QuadTree<TransitStopFacility>  stopsFacilitiesQT= TransitScheduleUtils.createQuadTreeOfTransitStopFacilities(this.scenario.getTransitSchedule());
			PTStationFinder2 finder = new PTStationFinder2(stopsFacilitiesQT);
			InteractionFinder interactionFinder = new StationBasedInteractionFinder(scenario.getNetwork(), specification, Double.parseDouble(service.get("AccessEgress_Distance")));
			bindTripConstraintFactory(name + "_PT_CONSTRAINT").toInstance(provideSharingPTTTripConstraint2(finder, scenario, name, interactionFinder));
		} else {
//			PTStationFinder finder = new PTStationFinder(this.scenario.getTransitSchedule().getFacilities());
			Map<Id<TransitStopFacility>, TransitStopFacility>stops=this.scenario.getTransitSchedule().getFacilities();
			QuadTree<TransitStopFacility>  stopsFacilitiesQT= TransitScheduleUtils.createQuadTreeOfTransitStopFacilities(this.scenario.getTransitSchedule());
			PTStationFinder2 finder = new PTStationFinder2(stopsFacilitiesQT);
			bindTripConstraintFactory(name + "_PT_CONSTRAINT").toInstance(provideSharingPTTTripConstraint2(finder, scenario, name));
		}
		bind(KraussPersonPredictor.class);
//		}
	}



	public KraussBikeSharePredictor provideBikeSharePredictor(KraussBikeShareCostModel costModel, String name){
		KraussBikeSharePredictor temporalPredictor= new KraussBikeSharePredictor(costModel, name);
		return temporalPredictor;

	}


	public KraussBikeShareEstimator provideBikeShareEstimator(String name, SharingPTParameters modeParameters, KraussBikeSharePredictor predictor, KraussPersonPredictor personPredictor){
		KraussBikeShareEstimator temporalEstimator=new KraussBikeShareEstimator(modeParameters,predictor, personPredictor, name);
		return temporalEstimator;

	}


   public GeneralizedEScooterCostModel provideEScooterCostModel(EqasimConfigGroup config, String name){
	   GeneralizedCostParameters costParameters= null;
	   try {
		   costParameters = provideCostParameters(config,this.commandLine);
	   } catch (Exception e) {
		   e.printStackTrace();
	   }
	   GeneralizedEScooterCostModel costModel= new GeneralizedEScooterCostModel(name, costParameters);
	   return(costModel);
   }


	public GeneralizedEScooterPredictor provideEScooterPredictor(GeneralizedEScooterCostModel costModel, String name){
		GeneralizedEScooterPredictor temporalPredictor= new GeneralizedEScooterPredictor(costModel, name);
		return temporalPredictor;

	}


	public GeneralizedEScooterEstimator provideEScooterEstimator(String name, SharingPTParameters modeParameters, GeneralizedEScooterPredictor predictor, KraussPersonPredictor personPredictor){
		GeneralizedEScooterEstimator temporalEstimator=new GeneralizedEScooterEstimator(modeParameters,predictor, personPredictor, name);
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
		Set<String> sharingKMCosts= parameterDefinition.sharingMinCosts.keySet();
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
	private  GeneralizedBikeShareEstimator addSharingServiceToEqasimBS(EqasimConfigGroup config,CommandLine commandLine, String name) throws Exception {
		GeneralizedCostParameters costParameters=GeneralizedCostParameters.provideCostParameters(config,commandLine);
		GeneralizedBikeShareCostModel costModel=new GeneralizedBikeShareCostModel(name,costParameters);
		GeneralizedBikeSharePredictor bikePredictor=new GeneralizedBikeSharePredictor(costModel,"sharing:"+name);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SharingPTParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		GeneralizedBikeShareEstimator bikeEstimator=new GeneralizedBikeShareEstimator(modeParams,bikePredictor,personPredictor,name);
		return bikeEstimator;
	}
	private GeneralizedEScooterEstimator addSharingServiceToEqasimEscooter(EqasimConfigGroup config, CommandLine commandLine, String name) throws Exception {
		GeneralizedCostParameters costParameters=GeneralizedCostParameters.provideCostParameters(config,commandLine);
		GeneralizedEScooterCostModel costModel=new GeneralizedEScooterCostModel(name,costParameters);
		GeneralizedEScooterPredictor bikePredictor=new GeneralizedEScooterPredictor(costModel,"sharing:"+name);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SharingPTParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		GeneralizedEScooterEstimator estimator=new GeneralizedEScooterEstimator(modeParams,bikePredictor,personPredictor,name);
		return estimator;
	}

	private GeneralizedBikeSharePTBikeShareEstimator addSharingServiceSharingPTSharing(EqasimConfigGroup config, CommandLine commandLine, String name,String underlyingMode) throws Exception {
		GeneralizedCostParameters costParameters=GeneralizedCostParameters.provideCostParameters(config,commandLine);
		GeneralizedMultimodalCostModel costModel=new GeneralizedMultimodalCostModel(name,costParameters);
		GeneralizedBikeSharingPTBikeSharingPredictor predictor =new GeneralizedBikeSharingPTBikeSharingPredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SharingPTParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		GeneralizedBikeSharePTBikeShareEstimator estimator=new GeneralizedBikeSharePTBikeShareEstimator(modeParams,predictor,name);
		return estimator;
	}
	private GeneralizedEScooterPTEScooterEstimator addSharingServiceSharingPTSharingEscooter(EqasimConfigGroup config, CommandLine commandLine, String name,String underlyingMode) throws Exception {
		GeneralizedCostParameters costParameters=GeneralizedCostParameters.provideCostParameters(config,commandLine);
		GeneralizedMultimodalCostModel costModel=new GeneralizedMultimodalCostModel(name,costParameters);
		GeneralizedBikeSharingPTBikeSharingPredictor predictor =new GeneralizedBikeSharingPTBikeSharingPredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SharingPTParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		GeneralizedEScooterPTEScooterEstimator estimator= new GeneralizedEScooterPTEScooterEstimator(modeParams,predictor,name);
		return estimator;
	}
	private GeneralizedEScooterPTEScooterEstimator addSharingServiceSharingPTSharingEScooter(EqasimConfigGroup config, CommandLine commandLine, String name,String underlyingMode) throws Exception {
		GeneralizedCostParameters costParameters=GeneralizedCostParameters.provideCostParameters(config,commandLine);
		GeneralizedMultimodalCostModel costModel=new GeneralizedMultimodalCostModel(name,costParameters);
		GeneralizedBikeSharingPTBikeSharingPredictor predictor =new GeneralizedBikeSharingPTBikeSharingPredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SharingPTParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		GeneralizedEScooterPTEScooterEstimator estimator=new GeneralizedEScooterPTEScooterEstimator(modeParams,predictor,name);
		return estimator;
	}

	private GeneralizedBikeSharePTEstimator addSharingServiceSharingPT(EqasimConfigGroup config, CommandLine commandLine, String name,String underlyingMode) throws Exception {
		GeneralizedCostParameters costParameters=GeneralizedCostParameters.provideCostParameters(config,commandLine);
		GeneralizedMultimodalCostModel costModel=new GeneralizedMultimodalCostModel(name,costParameters);
		GeneralizedBikeSharePTPredictor predictor = new GeneralizedBikeSharePTPredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SharingPTParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		GeneralizedBikeSharePTEstimator estimator= new GeneralizedBikeSharePTEstimator(modeParams,predictor,name);
		return estimator;
	}
	private GeneralizedEScooterPTEstimator addSharingServiceSharingPTEScooter(EqasimConfigGroup config, CommandLine commandLine, String name,String underlyingMode) throws Exception {
		GeneralizedCostParameters costParameters=GeneralizedCostParameters.provideCostParameters(config,commandLine);
		GeneralizedMultimodalCostModel costModel=new GeneralizedMultimodalCostModel(name,costParameters);
		GeneralizedBikeSharePTPredictor predictor = new GeneralizedBikeSharePTPredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SharingPTParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		GeneralizedEScooterPTEstimator estimator= new GeneralizedEScooterPTEstimator(modeParams,predictor,name);
		return estimator;
	}
	private GeneralizedPTBikeShareEstimator addSharingServicePTSharing(EqasimConfigGroup config, CommandLine commandLine, String name,String underlyingMode) throws Exception {
		GeneralizedCostParameters costParameters=GeneralizedCostParameters.provideCostParameters(config,commandLine);
		GeneralizedMultimodalCostModel costModel=new GeneralizedMultimodalCostModel(name,costParameters);
		GeneralizedPTBikeSharePredictor predictor = new GeneralizedPTBikeSharePredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SharingPTParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		GeneralizedPTBikeShareEstimator estimator= new GeneralizedPTBikeShareEstimator(modeParams,predictor,name);
		return estimator;
	}
	private GeneralizedPTEScooterEstimator addSharingServicePTSharingEScooter(EqasimConfigGroup config, CommandLine commandLine, String name,String underlyingMode) throws Exception {
		GeneralizedCostParameters costParameters=GeneralizedCostParameters.provideCostParameters(config,commandLine);
		GeneralizedMultimodalCostModel costModel=new GeneralizedMultimodalCostModel(name,costParameters);
		GeneralizedPTBikeSharePredictor predictor = new GeneralizedPTBikeSharePredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SharingPTParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		GeneralizedPTEScooterEstimator estimator= new GeneralizedPTEScooterEstimator(modeParams,predictor,name);
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
	public GeneralizedStationBasedConstraint2.Factory provideSharingPTTTripConstraint2(
			PTStationFinder2 stationFinder, Scenario scenario, String name, InteractionFinder interactionFinder) {
		return new GeneralizedStationBasedConstraint2.Factory(stationFinder, scenario,name,interactionFinder);
	}
	public GeneralizedSharedMicroMobilityConstraint.Factory provideSMMConstraint(
			String mode,String name) {
		return new GeneralizedSharedMicroMobilityConstraint.Factory(mode,name);
	}
	public TripConstraintFactory provideSharingPTTTripConstraint2(
			PTStationFinder2 stationFinder, Scenario scenario, String name) {
		return new SharingPTTripConstraint2.Factory(stationFinder, scenario,name);
	}
	public GeneralizedStationBasedConstraint.Factory provideSharingPTTTripConstraint(
			PTStationFinder stationFinder, Scenario scenario, String name, InteractionFinder interactionFinder) {
		return new GeneralizedStationBasedConstraint.Factory(stationFinder, scenario,name,interactionFinder);
	}
	public SharingPTTripConstraint.Factory provideSharingPTTTripConstraint(
			PTStationFinder stationFinder, Scenario scenario, String name) {
		return new SharingPTTripConstraint.Factory(stationFinder, scenario,name);
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



}
