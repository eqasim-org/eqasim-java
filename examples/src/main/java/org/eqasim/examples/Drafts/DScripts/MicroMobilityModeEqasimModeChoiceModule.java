package org.eqasim.examples.Drafts.DScripts;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.estimators.*;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.estimators.SMMBikeShareEstimator;
import org.eqasim.examples.corsica_drt.Drafts.DGeneralizedMultimodal.GeneralizedStationBasedConstraint;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.costModels.SMMMultimodalCostModel;

import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.predictors.SMMBikeSharePTPredictor;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.predictors.SMMBikeSharingPTBikeSharingPredictor;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.predictors.SMMPTBikeSharePredictor;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.costModels.SMMBikeShareCostModel;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.predictors.SMMBikeSharePredictor;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SMMCostParameters;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SMMModeAvailability;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.cost.KraussBikeShareCostModel;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors.KraussBikeSharePredictor;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors.KraussPersonPredictor;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.utilities.KraussBikeShareEstimator;
import org.eqasim.examples.corsica_drt.Drafts.DGeneralizedMultimodal.sharingPt.PTStationFinder;
import org.eqasim.examples.corsica_drt.Drafts.DGeneralizedMultimodal.sharingPt.SharingPTModeChoiceModule;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SMMParameters;
import org.eqasim.examples.corsica_drt.Drafts.DGeneralizedMultimodal.sharingPt.SharingPTTripConstraint;
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
	public Scenario scenario;
	public String name;
	public String serviceFile;


	public MicroMobilityModeEqasimModeChoiceModule(CommandLine commandLine, Scenario scenario,String name ,String serviceFile) {
		this.commandLine = commandLine;
		this.scenario = scenario;
		this.name=name;
		this.serviceFile=serviceFile;
	}



	@Override
	protected void installEqasimExtension() {
		//Bind Parameters
			bind(ModeParameters.class).to(SMMParameters.class);
		// Create Cost Parameters
//
		SMMBikeShareEstimator generalEstimator=null;
		try {
			generalEstimator=this.addSharingServiceToEqasim((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"),this.commandLine,name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		bindUtilityEstimator("sharing:"+name).toInstance(generalEstimator);
		try {
			bindCostModel("sharing:"+name).toInstance(new SMMBikeShareCostModel("sharing:bikeShare",this.provideCostParameters((EqasimConfigGroup)this.scenario.getConfig().getModules().get("eqasim"),this.commandLine)));
		} catch (Exception e) {
			e.printStackTrace();
		}
//
		bindModeAvailability("ModeAvailability").toInstance(provideModeAvailability());

		Map<String,String>modesByCommand=MicroMobilityModeEqasimModeChoiceModule.processCommandLine(this.commandLine,"sharing-mode-name");
		for (Map.Entry<String, String> entry :modesByCommand.entrySet()) {
			String option = entry.getKey();
			String value = entry.getValue();
			try {
				String[] parts = option.split("\\.");
				if (parts.length != 1) {
					if(parts[1].equals("Intermodal")){
						SMMBikeSharePTBikeShareEstimator BS_PT_BS=null;
						try {
							BS_PT_BS=this.addSharingServiceSharingPTSharing((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"),this.commandLine,"bikeShare","Bike-Share");
						} catch (Exception e) {
							e.printStackTrace();
						}
						bindUtilityEstimator(parts[0]+"_PT_"+parts[0]).toInstance(generalEstimator);
						SMMBikeSharePTEstimator BS_PT_Estimator=null;
						try {
							BS_PT_Estimator=this.addSharingServiceSharingPT((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"),this.commandLine,"bikeShare","Bike-Share");
						} catch (Exception e) {
							e.printStackTrace();
						}
						bindUtilityEstimator(parts[0]+"_PT").toInstance(BS_PT_Estimator);
						SMMPTBikeShareEstimator PT_BS_Estimator=null;
						try {
							PT_BS_Estimator=this.addSharingServicePTSharing((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"),this.commandLine,"bikeShare","Bike-Share");
						} catch (Exception e) {
							e.printStackTrace();
						}
						bindUtilityEstimator("PT_"+parts[0]).toInstance(PT_BS_Estimator);
						PTStationFinder finder= new PTStationFinder(this.scenario.getTransitSchedule().getFacilities());
						SharingServiceSpecification specification = new DefaultSharingServiceSpecification();
						new SharingServiceReader(specification).readURL(
								ConfigGroup.getInputFileURL(getConfig().getContext(), serviceFile));
						InteractionFinder interactionFinder= new StationBasedInteractionFinder(scenario.getNetwork(),specification,1000);
						bindTripConstraintFactory(parts[0]+"_PT_CONSTRAINT").toInstance(provideSharingPTTTripConstraint(finder,scenario,name,interactionFinder));

					}
				}
			}catch (Exception e){}
		}

		bind(KraussPersonPredictor.class);
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
	   SMMCostParameters costParameters= null;
	   try {
		   costParameters = provideCostParameters(config,this.commandLine);
	   } catch (Exception e) {
		   e.printStackTrace();
	   }
	   SMMBikeShareCostModel bikeShareCostModel= new SMMBikeShareCostModel(name, costParameters);
	   return(bikeShareCostModel);
   }




	public static void validateSharingCostParameters(SMMCostParameters parameterDefinition) throws Exception {
		Set<String> sharingKMCosts= parameterDefinition.sharingMinCosts.keySet();
		Set<String> sharingBookingCosts=  parameterDefinition.sharingBookingCosts.keySet();
		boolean isEqual = isEqual(sharingBookingCosts,sharingKMCosts);
		if (isEqual==false) {
			throw new  IllegalArgumentException(" One of the sharing modes does not have booking or km cost");
		}
	}


	public static SMMCostParameters provideCostParameters(EqasimConfigGroup config, CommandLine commandLine) throws Exception {
		SMMCostParameters parameters = SMMCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		SMMCostParameters.applyCommandLineMicromobility("cost-parameter",commandLine,parameters);
		return parameters;
	}
	private SMMBikeShareEstimator addSharingServiceToEqasim(EqasimConfigGroup config, CommandLine commandLine, String name) throws Exception {
		SMMCostParameters costParameters= SMMCostParameters.provideCostParameters(config,commandLine);
		SMMBikeShareCostModel costModel=new SMMBikeShareCostModel(name,costParameters);
		SMMBikeSharePredictor bikePredictor=new SMMBikeSharePredictor(costModel,"sharing:"+name);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SMMParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		SMMBikeShareEstimator bikeEstimator=new SMMBikeShareEstimator(modeParams,bikePredictor,personPredictor,name);
		return bikeEstimator;
	}

	private SMMBikeSharePTBikeShareEstimator addSharingServiceSharingPTSharing(EqasimConfigGroup config, CommandLine commandLine, String name, String underlyingMode) throws Exception {
		SMMCostParameters costParameters= SMMCostParameters.provideCostParameters(config,commandLine);
		SMMMultimodalCostModel costModel=new SMMMultimodalCostModel(name,costParameters);
		SMMBikeSharingPTBikeSharingPredictor predictor =new SMMBikeSharingPTBikeSharingPredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SMMParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		SMMBikeSharePTBikeShareEstimator estimator=new SMMBikeSharePTBikeShareEstimator(modeParams,predictor,name);
		return estimator;
	}
	private SMMEScooterPTEScooterEstimator addSharingServiceSharingPTSharingEScooter(EqasimConfigGroup config, CommandLine commandLine, String name, String underlyingMode) throws Exception {
		SMMCostParameters costParameters= SMMCostParameters.provideCostParameters(config,commandLine);
		SMMMultimodalCostModel costModel=new SMMMultimodalCostModel(name,costParameters);
		SMMBikeSharingPTBikeSharingPredictor predictor =new SMMBikeSharingPTBikeSharingPredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SMMParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		SMMEScooterPTEScooterEstimator estimator=new SMMEScooterPTEScooterEstimator(modeParams,predictor,name);
		return estimator;
	}

	private SMMBikeSharePTEstimator addSharingServiceSharingPT(EqasimConfigGroup config, CommandLine commandLine, String name, String underlyingMode) throws Exception {
		SMMCostParameters costParameters= SMMCostParameters.provideCostParameters(config,commandLine);
		SMMMultimodalCostModel costModel=new SMMMultimodalCostModel(name,costParameters);
		SMMBikeSharePTPredictor predictor = new SMMBikeSharePTPredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SMMParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		SMMBikeSharePTEstimator estimator= new SMMBikeSharePTEstimator(modeParams,predictor,name);
		return estimator;
	}
	private SMMEScooterPTEstimator addSharingServiceSharingPTEScooter(EqasimConfigGroup config, CommandLine commandLine, String name, String underlyingMode) throws Exception {
		SMMCostParameters costParameters= SMMCostParameters.provideCostParameters(config,commandLine);
		SMMMultimodalCostModel costModel=new SMMMultimodalCostModel(name,costParameters);
		SMMBikeSharePTPredictor predictor = new SMMBikeSharePTPredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SMMParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		SMMEScooterPTEstimator estimator= new SMMEScooterPTEstimator(modeParams,predictor,name);
		return estimator;
	}
	private SMMPTBikeShareEstimator addSharingServicePTSharing(EqasimConfigGroup config, CommandLine commandLine, String name, String underlyingMode) throws Exception {
		SMMCostParameters costParameters= SMMCostParameters.provideCostParameters(config,commandLine);
		SMMMultimodalCostModel costModel=new SMMMultimodalCostModel(name,costParameters);
		SMMPTBikeSharePredictor predictor = new SMMPTBikeSharePredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SMMParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		SMMPTBikeShareEstimator estimator= new SMMPTBikeShareEstimator(modeParams,predictor,name);
		return estimator;
	}
	private SMMPTEScooterEstimator addSharingServicePTSharingEScooter(EqasimConfigGroup config, CommandLine commandLine, String name, String underlyingMode) throws Exception {
		SMMCostParameters costParameters= SMMCostParameters.provideCostParameters(config,commandLine);
		SMMMultimodalCostModel costModel=new SMMMultimodalCostModel(name,costParameters);
		SMMPTBikeSharePredictor predictor = new SMMPTBikeSharePredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SMMParameters modeParams= new SharingPTModeChoiceModule(commandLine,null).provideModeChoiceParameters(config);
		SMMPTEScooterEstimator estimator= new SMMPTEScooterEstimator(modeParams,predictor,name);
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
			PTStationFinder stationFinder, Scenario scenario, String name, InteractionFinder interactionFinder) {
		return new GeneralizedStationBasedConstraint.Factory(stationFinder, scenario,name,interactionFinder);
	}
	public SharingPTTripConstraint.Factory provideSharingPTTTripConstraintFreeFloating(
			PTStationFinder stationFinder, Scenario scenario, String name) {
		return new SharingPTTripConstraint.Factory(stationFinder, scenario,name);
	}

	public SMMModeAvailability provideModeAvailability(){
		SMMModeAvailability modeAvailability= SMMModeAvailability.buildDefault();
		try {
			SMMModeAvailability.applyCommandLineAvailability("sharing-mode-name",commandLine,modeAvailability);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return modeAvailability;
	}



}
