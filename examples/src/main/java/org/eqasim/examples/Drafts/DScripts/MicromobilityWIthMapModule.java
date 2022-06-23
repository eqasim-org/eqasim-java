package org.eqasim.examples.Drafts.DScripts;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.examples.SMMFramework.GBFSUtils.SharingPTTripWriter;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.constraints.SMMConstraint;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.constraints.SMMStationBasedConstraint;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.costModels.SMMMultimodalCostModel;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.estimators.*;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.predictors.SMMBikeSharePTPredictor;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.predictors.SMMBikeSharingPTBikeSharingPredictor;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.predictors.SMMPTBikeSharePredictor;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.costModels.SMMBikeShareCostModel;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.costModels.SMMEScooterCostModel;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.estimators.SMMBikeShareEstimator;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.estimators.SMMEScooterEstimator;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.predictors.SMMBikeSharePredictor;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.predictors.SMMEScooterPredictor;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SMMCostParameters;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SMMModeAvailability;
import org.eqasim.examples.corsica_drt.Drafts.DGeneralizedMultimodal.sharingPt.PTStationFinder;
import org.eqasim.examples.corsica_drt.Drafts.DGeneralizedMultimodal.sharingPt.SharingPTTripConstraint;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.cost.KraussBikeShareCostModel;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors.KraussBikeSharePredictor;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors.KraussPersonPredictor;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.utilities.KraussBikeShareEstimator;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.constraints.SMMPTStationFinder;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.constraints.SMMTripConstraint;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SMMParameters;
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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.geotools.feature.type.DateUtil.isEqual;
/**
 * Class Module extends Eqasim framework and the Krauss model by creating instances for the SMM modes of estimators and binds them to the controller
 */
public class MicromobilityWIthMapModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;
	public Scenario scenario;
	public HashMap<String, String> service;

	/**
	 * Method creates the modules
	 * @param commandLine command line arguments of SMM modes
	 * @param scenario MATSIm scenario
	 * @param service The SMM service Specification
	 */
	public MicromobilityWIthMapModule(CommandLine commandLine, Scenario scenario, HashMap<String, String> service) {
		this.commandLine = commandLine;
		this.scenario = scenario;
		this.service = service;
	}

	/**
	 * Install a SMM mode in the Eqasim framework
	 */
	
	@Override
	protected void installEqasimExtension() {
		//Bind Parameters
		bind(ModeParameters.class).to(SMMParameters.class);
		// Create Cost Parameters
		String name = service.get("Service_Name");
		String mode = service.get("Mode");
		bind(TripsAndLegsCSVWriter.CustomTripsWriterExtension.class).to(SharingPTTripWriter.class).asEagerSingleton();



		if(mode.equals("Shared-Bike")){
			bindBikeShareService(name,mode);
		}else if(mode.equals("eScooter")){
			bindeScooterService(name,mode);
		}
	}
	/**
	 * Method generates the Eqasim module for a bikesharing service given a name and the routing mode
	 * @param name name of the service
	 * @param mode routing mode
	 */
	public void bindBikeShareService(String name, String mode) {
		// Initialization and creation of Bikesharing estimator
		SMMBikeShareEstimator generalEstimator = null;
		try {
			generalEstimator = this.addSharingServiceToEqasimBS((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//Binding of estimator
		bindUtilityEstimator("sharing:" + name).toInstance(generalEstimator);
		// Binding of the cost model for this service
		try {
			bindCostModel("sharing:" + name).toInstance(new SMMBikeShareCostModel("sharing:" + name, this.provideCostParameters((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine)));
		} catch (Exception e) {
			e.printStackTrace();
		}
		 // Binding of the constraint for the mode
		bindTripConstraintFactory(name).toInstance(provideSMMConstraint(mode, name));
		// Binds the mode availability object for all SMM modes
		bindModeAvailability(name + "ModeAvailability").toInstance(provideModeAvailability());
		// If its multimodal install the SMM-PT mode choice
		if (service.get("Multimodal").equals("Yes")) {

			SMMBikeSharePTBikeShareEstimator BS_PT_BS = null;
			try {
				BS_PT_BS = this.addSharingServiceSharingPTSharing((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name, mode);
			} catch (Exception e) {
				e.printStackTrace();
			}
			bindUtilityEstimator(name + "_PT_" + name).toInstance(BS_PT_BS);
			SMMBikeSharePTEstimator BS_PT_Estimator = null;
			try {
				BS_PT_Estimator = this.addSharingServiceSharingPT((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name, mode);
			} catch (Exception e) {
				e.printStackTrace();
			}
			bindUtilityEstimator(name + "_PT").toInstance(BS_PT_Estimator);
			SMMPTBikeShareEstimator PT_BS_Estimator = null;
			try {
				PT_BS_Estimator = this.addSharingServicePTSharing((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name, mode);
			} catch (Exception e) {
				e.printStackTrace();
			}
			bindUtilityEstimator("PT_" + name).toInstance(PT_BS_Estimator);
		}
		//  If it is station based install the constraint of station based services
		if (service.get("Scheme").equals("Station-Based")) {
			// Creates a proxy SMM service specification object of the Sharing contrib
			SharingServiceSpecification specification = new DefaultSharingServiceSpecification();
			new SharingServiceReader(specification).readURL(
					ConfigGroup.getInputFileURL(getConfig().getContext(), service.get("Service_File")));
			// Creates a QTree for the distances between PT Stations
			QuadTree<TransitStopFacility>  stopsFacilitiesQT= TransitScheduleUtils.createQuadTreeOfTransitStopFacilities(this.scenario.getTransitSchedule());
			// Creates the locator of PT stations
			SMMPTStationFinder finder = new SMMPTStationFinder(stopsFacilitiesQT);
			// Creates the identificator of location of drop off and pick up points
			InteractionFinder interactionFinder = new StationBasedInteractionFinder(scenario.getNetwork(), specification, Double.parseDouble(service.get("AccessEgress_Distance")));
			// Bind contraints
			bindTripConstraintFactory(name + "_PT_CONSTRAINT").toInstance(provideSharingPTTTripConstraint2(finder, scenario, name, interactionFinder));
		} else {
//			// Creates a QTree for the distances between PT Stations
			QuadTree<TransitStopFacility>  stopsFacilitiesQT= TransitScheduleUtils.createQuadTreeOfTransitStopFacilities(this.scenario.getTransitSchedule());
			// Creates the locator of PT stations
			SMMPTStationFinder finder = new SMMPTStationFinder(stopsFacilitiesQT);
			// Bind contraints
			bindTripConstraintFactory(name + "_PT_CONSTRAINT").toInstance(provideSharingPTTTripConstraint2(finder, scenario, name));
		}
		// Bind the person predictor
		bind(KraussPersonPredictor.class);
//		}
	}
	public void bindeScooterService(String name, String mode) {
		// Initialization and creation of eScooter estimator
		SMMEScooterEstimator generalEstimator = null;
		try {
			generalEstimator = this.addSharingServiceToEqasimEscooter((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//Binding of estimator
		bindUtilityEstimator("sharing:" + name).toInstance(generalEstimator);
		//Binding of cost model
		try {
			bindCostModel("sharing:" + name).toInstance(new SMMEScooterCostModel("sharing:" + name, this.provideCostParameters((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine)));
		} catch (Exception e) {
			e.printStackTrace();
		}
        // Binding of the constraint for the mode
		bindTripConstraintFactory(name).toInstance(provideSMMConstraint(mode, name));
		// Binds the mode availability object for all SMM modes
		bindModeAvailability(name + "ModeAvailability").toInstance(provideModeAvailability());
		// If its multimodal install the SMM-PT mode choice
		if (service.get("Multimodal").equals("Yes")) {

			SMMEScooterPTEScooterEstimator ES_PT_ES = null;
			try {
				ES_PT_ES = this.addSharingServiceSharingPTSharingEscooter((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name, mode);
			} catch (Exception e) {
				e.printStackTrace();
			}
			bindUtilityEstimator(name + "_PT_" + name).toInstance(ES_PT_ES);
			SMMEScooterPTEstimator ES_PT_Estimator = null;
			try {
				ES_PT_Estimator = this.addSharingServiceSharingPTEScooter((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name, mode);
			} catch (Exception e) {
				e.printStackTrace();
			}
			bindUtilityEstimator(name + "_PT").toInstance(ES_PT_Estimator);
			SMMPTEScooterEstimator PT_ES_Estimator = null;
			try {
				PT_ES_Estimator = this.addSharingServicePTSharingEScooter((EqasimConfigGroup) this.scenario.getConfig().getModules().get("eqasim"), this.commandLine, name, mode);
			} catch (Exception e) {
				e.printStackTrace();
			}
			bindUtilityEstimator("PT_" + name).toInstance(PT_ES_Estimator);
		}
		//  If it is station based install the constraint of station based services
		if (service.get("Scheme").equals("Station-Based")) {
			// Creates a proxy SMM service specification object of the Sharing contrib
			SharingServiceSpecification specification = new DefaultSharingServiceSpecification();
			new SharingServiceReader(specification).readURL(
					ConfigGroup.getInputFileURL(getConfig().getContext(), service.get("Service_File")));
			// Creates a QTree for the distances between PT Stations
			QuadTree<TransitStopFacility>  stopsFacilitiesQT= TransitScheduleUtils.createQuadTreeOfTransitStopFacilities(this.scenario.getTransitSchedule());
			// Creates the locator of PT stations
			SMMPTStationFinder finder = new SMMPTStationFinder(stopsFacilitiesQT);
			// Creates the identificator of location of drop off and pick up points
			InteractionFinder interactionFinder = new StationBasedInteractionFinder(scenario.getNetwork(), specification, Double.parseDouble(service.get("AccessEgress_Distance")));
			// Bind contraints
			bindTripConstraintFactory(name + "_PT_CONSTRAINT").toInstance(provideSharingPTTTripConstraint2(finder, scenario, name, interactionFinder));
		} else {
			// Creates a QTree for the distances between PT Stations
			Map<Id<TransitStopFacility>, TransitStopFacility>stops=this.scenario.getTransitSchedule().getFacilities();
			// Creates the locator of PT stations
			QuadTree<TransitStopFacility>  stopsFacilitiesQT= TransitScheduleUtils.createQuadTreeOfTransitStopFacilities(this.scenario.getTransitSchedule());
			// Creates the locator of PT stations
			SMMPTStationFinder finder = new SMMPTStationFinder(stopsFacilitiesQT);
			// Bind contraints
			bindTripConstraintFactory(name + "_PT_CONSTRAINT").toInstance(provideSharingPTTTripConstraint2(finder, scenario, name));
		}
		// Bind the person predictor
		bind(KraussPersonPredictor.class);
//		}
	}


	/**
	 * Method creates a bikesharing predictor instance
	 * @param costModel
	 * @param name
	 * @return bike sharing predictor instance
	 */
	public KraussBikeSharePredictor provideBikeSharePredictor(KraussBikeShareCostModel costModel, String name){
		KraussBikeSharePredictor temporalPredictor= new KraussBikeSharePredictor(costModel, name);
		return temporalPredictor;

	}

	/**
	 * Method  creates a instance of bikesharing utility estimator
	 * @param name service name
	 * @param modeParameters Mode parameters object
	 * @param predictor  bikesharing predictor
	 * @param personPredictor person predictor
	 * @return a bike sharing utility estimator for service name
	 */
	public KraussBikeShareEstimator provideBikeShareEstimator(String name, SMMParameters modeParameters, KraussBikeSharePredictor predictor, KraussPersonPredictor personPredictor){
		KraussBikeShareEstimator temporalEstimator=new KraussBikeShareEstimator(modeParameters,predictor, personPredictor, name);
		return temporalEstimator;

	}

	/**
	 * Provides the cost model for a named name eScooter service
	 * @param config
	 * @param name
	 * @return eScooter service cost model
	 */
   public SMMEScooterCostModel provideEScooterCostModel(EqasimConfigGroup config, String name){
	   SMMCostParameters costParameters= null;
	   try {
		   costParameters = provideCostParameters(config,this.commandLine);
	   } catch (Exception e) {
		   e.printStackTrace();
	   }
	   SMMEScooterCostModel costModel= new SMMEScooterCostModel(name, costParameters);
	   return(costModel);
   }
	/**
	 * Method creates a eScooter predictor instance
	 * @param costModel
	 * @param name
	 * @return eScooter predictor instance
	 */

	public SMMEScooterPredictor provideEScooterPredictor(SMMEScooterCostModel costModel, String name){
		SMMEScooterPredictor temporalPredictor= new SMMEScooterPredictor(costModel, name);
		return temporalPredictor;

	}

	/**
	 * Method  creates a instance of eScooter utility estimator
	 * @param name service name
	 * @param modeParameters Mode parameters object
	 * @param predictor  eScooter predictor
	 * @param personPredictor person predictor
	 * @return a eScooter utility estimator for service name
	 */
	public SMMEScooterEstimator provideEScooterEstimator(String name, SMMParameters modeParameters, SMMEScooterPredictor predictor, KraussPersonPredictor personPredictor){
		SMMEScooterEstimator temporalEstimator=new SMMEScooterEstimator(modeParameters,predictor, personPredictor, name);
		return temporalEstimator;

	}

	/**
	 * Provides the cost model for a named name bikesharing service
	 * @param config
	 * @param name
	 * @return bikesharing service cost model
	 */
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


	/**
	 * Method validates that there is a cost per minute and an unlocking cost for each service
	 * @param parameterDefinition
	 * @throws Exception there is no cost for unlocking or min cost
	 */

	public static void validateSharingCostParameters(SMMCostParameters parameterDefinition) throws Exception {
		Set<String> sharingKMCosts= parameterDefinition.sharingMinCosts.keySet();
		Set<String> sharingBookingCosts=  parameterDefinition.sharingBookingCosts.keySet();
		boolean isEqual = isEqual(sharingBookingCosts,sharingKMCosts);
		if (isEqual==false) {
			throw new  IllegalArgumentException(" One of the sharing modes does not have booking or km cost");
		}
	}
	/**
	 * Method generate the cost parameters for the model based on program arguments
	 * @param config
	 * @param commandLine
	 * @return
	 * @throws Exception
	 */

	public static SMMCostParameters provideCostParameters(EqasimConfigGroup config, CommandLine commandLine) throws Exception {
		SMMCostParameters parameters = SMMCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		SMMCostParameters.applyCommandLineMicromobility("cost-parameter",commandLine,parameters);
		return parameters;
	}
	/**
	 * Method creates a  bikesharing service in eqasim by creating predictor, cost model and estimator instances
	 * @param config config for the sharing service specification
	 * @param commandLine command line for customizable argumenrs
	 * @param name name of bikesharing service
	 * @return a bikseharing  utility estimator
	 * @throws Exception
	 */
	private SMMBikeShareEstimator addSharingServiceToEqasimBS(EqasimConfigGroup config, CommandLine commandLine, String name) throws Exception {
		SMMCostParameters costParameters= SMMCostParameters.provideCostParameters(config,commandLine);
		SMMBikeShareCostModel costModel=new SMMBikeShareCostModel(name,costParameters);
		SMMBikeSharePredictor bikePredictor=new SMMBikeSharePredictor(costModel,"sharing:"+name);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SMMParameters modeParams= provideModeChoiceParameters(config);
		SMMBikeShareEstimator bikeEstimator=new SMMBikeShareEstimator(modeParams,bikePredictor,personPredictor,name);
		return bikeEstimator;
	}
	/**
	 * Method creates a  eScooter service in eqasim by creating predictor, cost model and estimator instances
	 * @param config config for the sharing service specification
	 * @param commandLine command line for customizable argumenrs
	 * @param name name of eScooter service
	 * @return a eScooter utility estimator
	 * @throws Exception
	 */
	private SMMEScooterEstimator addSharingServiceToEqasimEscooter(EqasimConfigGroup config, CommandLine commandLine, String name) throws Exception {
		SMMCostParameters costParameters= SMMCostParameters.provideCostParameters(config,commandLine);
		SMMEScooterCostModel costModel=new SMMEScooterCostModel(name,costParameters);
		SMMEScooterPredictor bikePredictor=new SMMEScooterPredictor(costModel,"sharing:"+name);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SMMParameters modeParams= provideModeChoiceParameters(config);
		SMMEScooterEstimator estimator=new SMMEScooterEstimator(modeParams,bikePredictor,personPredictor,name);
		return estimator;
	}
	/**
	 * The following three methods create the estimators for multimodal bikesharing-PT as three modes such as bikesharing-PT;
	 * bikesharing-PT-bikesharing and PT-bikesharing
	 */
	private SMMBikeSharePTBikeShareEstimator addSharingServiceSharingPTSharing(EqasimConfigGroup config, CommandLine commandLine, String name, String underlyingMode) throws Exception {
		SMMCostParameters costParameters= SMMCostParameters.provideCostParameters(config,commandLine);
		SMMMultimodalCostModel costModel=new SMMMultimodalCostModel(name,costParameters);
		SMMBikeSharingPTBikeSharingPredictor predictor =new SMMBikeSharingPTBikeSharingPredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SMMParameters modeParams= provideModeChoiceParameters(config);
		SMMBikeSharePTBikeShareEstimator estimator=new SMMBikeSharePTBikeShareEstimator(modeParams,predictor,name);
		return estimator;
	}
	private SMMBikeSharePTEstimator addSharingServiceSharingPT(EqasimConfigGroup config, CommandLine commandLine, String name, String underlyingMode) throws Exception {
		SMMCostParameters costParameters= SMMCostParameters.provideCostParameters(config,commandLine);
		SMMMultimodalCostModel costModel=new SMMMultimodalCostModel(name,costParameters);
		SMMBikeSharePTPredictor predictor = new SMMBikeSharePTPredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SMMParameters modeParams= provideModeChoiceParameters(config);
		SMMBikeSharePTEstimator estimator= new SMMBikeSharePTEstimator(modeParams,predictor,name);
		return estimator;
	}
	private SMMPTBikeShareEstimator addSharingServicePTSharing(EqasimConfigGroup config, CommandLine commandLine, String name, String underlyingMode) throws Exception {
		SMMCostParameters costParameters= SMMCostParameters.provideCostParameters(config,commandLine);
		SMMMultimodalCostModel costModel=new SMMMultimodalCostModel(name,costParameters);
		SMMPTBikeSharePredictor predictor = new SMMPTBikeSharePredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SMMParameters modeParams= provideModeChoiceParameters(config);
		SMMPTBikeShareEstimator estimator= new SMMPTBikeShareEstimator(modeParams,predictor,name);
		return estimator;
	}
	/**
	 * The following three methods create the estimators for multimodal eScooter-PT as three modes such as eScooter-PT;
	 * eScooter-PT-eScooter and PT-eScooter
	 */
	private SMMEScooterPTEScooterEstimator addSharingServiceSharingPTSharingEScooter(EqasimConfigGroup config, CommandLine commandLine, String name, String underlyingMode) throws Exception {
		SMMCostParameters costParameters= SMMCostParameters.provideCostParameters(config,commandLine);
		SMMMultimodalCostModel costModel=new SMMMultimodalCostModel(name,costParameters);
		SMMBikeSharingPTBikeSharingPredictor predictor =new SMMBikeSharingPTBikeSharingPredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SMMParameters modeParams= provideModeChoiceParameters(config);
		SMMEScooterPTEScooterEstimator estimator=new SMMEScooterPTEScooterEstimator(modeParams,predictor,name);
		return estimator;
	}
	private SMMEScooterPTEScooterEstimator addSharingServiceSharingPTSharingEscooter(EqasimConfigGroup config, CommandLine commandLine, String name, String underlyingMode) throws Exception {
		SMMCostParameters costParameters= SMMCostParameters.provideCostParameters(config,commandLine);
		SMMMultimodalCostModel costModel=new SMMMultimodalCostModel(name,costParameters);
		SMMBikeSharingPTBikeSharingPredictor predictor =new SMMBikeSharingPTBikeSharingPredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SMMParameters modeParams= provideModeChoiceParameters(config);
		SMMEScooterPTEScooterEstimator estimator= new SMMEScooterPTEScooterEstimator(modeParams,predictor,name);
		return estimator;
	}
	


	private SMMEScooterPTEstimator addSharingServiceSharingPTEScooter(EqasimConfigGroup config, CommandLine commandLine, String name, String underlyingMode) throws Exception {
		SMMCostParameters costParameters= SMMCostParameters.provideCostParameters(config,commandLine);
		SMMMultimodalCostModel costModel=new SMMMultimodalCostModel(name,costParameters);
		SMMBikeSharePTPredictor predictor = new SMMBikeSharePTPredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SMMParameters modeParams= provideModeChoiceParameters(config);
		SMMEScooterPTEstimator estimator= new SMMEScooterPTEstimator(modeParams,predictor,name);
		return estimator;
	}

	private SMMPTEScooterEstimator addSharingServicePTSharingEScooter(EqasimConfigGroup config, CommandLine commandLine, String name, String underlyingMode) throws Exception {
		SMMCostParameters costParameters= SMMCostParameters.provideCostParameters(config,commandLine);
		SMMMultimodalCostModel costModel=new SMMMultimodalCostModel(name,costParameters);
		SMMPTBikeSharePredictor predictor = new SMMPTBikeSharePredictor(costModel,costParameters,name, underlyingMode);
		KraussPersonPredictor personPredictor=new KraussPersonPredictor();
		SMMParameters modeParams= provideModeChoiceParameters(config);
		SMMPTEScooterEstimator estimator= new SMMPTEScooterEstimator(modeParams,predictor,name);
		return estimator;
	}
	/**
	 *  Method creates a Map of the program arguments
	 * @param cmd
	 * @param prefix
	 * @return Map of the stored modified values
	 */
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
	/**
	 *The following methods are  in charge of creating the different constraints for SMM and SMM-PT
	 */
	public SMMStationBasedConstraint.Factory provideSharingPTTTripConstraint2(
			SMMPTStationFinder stationFinder, Scenario scenario, String name, InteractionFinder interactionFinder) {
		return new SMMStationBasedConstraint.Factory(stationFinder, scenario,name,interactionFinder);
	}
	public SMMConstraint.Factory provideSMMConstraint(
			String mode,String name) {
		return new SMMConstraint.Factory(mode,name);
	}
	public TripConstraintFactory provideSharingPTTTripConstraint2(
			SMMPTStationFinder stationFinder, Scenario scenario, String name) {
		return new SMMTripConstraint.Factory(stationFinder, scenario,name);
	}

	public SharingPTTripConstraint.Factory provideSharingPTTTripConstraint(
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

	@Provides
	@Singleton
	public SMMParameters provideModeChoiceParameters(EqasimConfigGroup config)
			throws IOException, CommandLine.ConfigurationException {
		SMMParameters parameters = SMMParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;

	}



}
