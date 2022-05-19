package org.eqasim.examples.corsica_drt.generalizedMicromobility;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.sharing.run.SharingConfigGroup;
import org.matsim.contrib.sharing.run.SharingServiceConfigGroup;
import org.matsim.contrib.sharing.service.SharingUtils;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;

import java.util.*;

public class MicromobilityUtils implements ParameterDefinition {

    public String mode;
    public String serviceScheme;
    public String serviceArea=null;
    public Double accessDist;

    public static void main(String[] args) throws IllegalAccessException {
//        addSharingService(ConfigUtils.loadConfig("C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\examples\\src\\main\\java\\org\\eqasim\\examples\\corsica_drt\\siouxfalls\\config.xml"),"Shared-Bike","Station Based",10000.0,null,null,null);
//        addSharingService(ConfigUtils.loadConfig("C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\examples\\src\\main\\java\\org\\eqasim\\examples\\corsica_drt\\siouxfalls\\config.xml"),"eScooter","None",10000.0,null,null,null);
    }

    public static void addSharingService(Controler controller, Config config, String mode, String serviceScheme, Double accessDist, String serviceArea, String serviceFile, String name) throws IllegalAccessException {


        SharingConfigGroup sharingConfigGroup = (SharingConfigGroup) config.getModules().get("sharing");
        if (sharingConfigGroup == null) {
            sharingConfigGroup = new SharingConfigGroup();
            config.addModule(sharingConfigGroup);
        }
        Map<String, PlansCalcRouteConfigGroup.ModeRoutingParams> routingParams = config.plansCalcRoute().getModeRoutingParams();
        if(routingParams.containsKey("Shared-Bike")== false && routingParams.containsKey("eScooter")==false) {
            addSharedModes(config);
        }
        // Save for super method
        String serviceSchemes[] = new String[]{"Station Based", "Free Floating"};
        String possibleModes[] = new String[]{"Shared-Bike", "eScooter"};
        boolean contains = Arrays.stream(possibleModes).anyMatch(mode::equals);
        if (contains == false) {
            {
                throw new IllegalArgumentException(" The sharing mode is invalid; please insert Shared-Bike or eScooter");
            }
        } else {
            boolean serviceSchemeVerification = Arrays.stream(serviceSchemes).anyMatch(serviceScheme::equals);
            if (serviceSchemeVerification == false) {
                {
                    throw new IllegalArgumentException(" The service scheme is invalid; please insert Station Based or Free-floating");
                }
            } else {
                if (serviceScheme.equals("Station Based")) {
                    addSharedStationBasedService(config, sharingConfigGroup, accessDist, name, mode, serviceFile);
                } else if (serviceScheme.equals("Free Floating")) {
                    addSharedFreeFloatingService(config, sharingConfigGroup, serviceArea, accessDist, name, mode, serviceFile);
                }

            }
        }
        // We need to add interaction activity types to scoring
        PlanCalcScoreConfigGroup.ActivityParams pickupParams = new PlanCalcScoreConfigGroup.ActivityParams(SharingUtils.PICKUP_ACTIVITY);
        pickupParams.setScoringThisActivityAtAll(false);
        config.planCalcScore().addActivityParams(pickupParams);

        PlanCalcScoreConfigGroup.ActivityParams dropoffParams = new PlanCalcScoreConfigGroup.ActivityParams(SharingUtils.DROPOFF_ACTIVITY);
        dropoffParams.setScoringThisActivityAtAll(false);
        config.planCalcScore().addActivityParams(dropoffParams);

        PlanCalcScoreConfigGroup.ActivityParams bookingParams = new PlanCalcScoreConfigGroup.ActivityParams(SharingUtils.BOOKING_ACTIVITY);
        bookingParams.setScoringThisActivityAtAll(false);
        config.planCalcScore().addActivityParams(bookingParams);

       // return sharingConfigGroup;

    }






    // Method adds the routing parameters for Shared Bike mode & eScooter ( Based on speeds from Hamad et al ,2020)
    public static void addSharedModes(Config config){
        PlansCalcRouteConfigGroup.ModeRoutingParams bikeRoutingParams = new PlansCalcRouteConfigGroup.ModeRoutingParams("Shared-Bike");
        bikeRoutingParams.setTeleportedModeSpeed(3.44);
        bikeRoutingParams.setBeelineDistanceFactor(1.3);
        config.plansCalcRoute().addModeRoutingParams(bikeRoutingParams);


        PlansCalcRouteConfigGroup.ModeRoutingParams eScooterRoutingParams = new PlansCalcRouteConfigGroup.ModeRoutingParams("eScooter");
        eScooterRoutingParams.setTeleportedModeSpeed(2.78);
        eScooterRoutingParams.setBeelineDistanceFactor(1.3);
        config.plansCalcRoute().addModeRoutingParams(eScooterRoutingParams);


//        PlansCalcRouteConfigGroup.ModeRoutingParams walkRoutingParams = new PlansCalcRouteConfigGroup.ModeRoutingParams("walk");
//        walkRoutingParams.setTeleportedModeSpeed(2.0);
//        walkRoutingParams.setBeelineDistanceFactor(1.3);
//        config.plansCalcRoute().addModeRoutingParams(walkRoutingParams);

        // We need to score bike
        PlanCalcScoreConfigGroup.ModeParams bikeScoringParams = new PlanCalcScoreConfigGroup.ModeParams("Shared-Bike");
        config.planCalcScore().addModeParams(bikeScoringParams);
        PlanCalcScoreConfigGroup.ModeParams eScooterScoringParams = new PlanCalcScoreConfigGroup.ModeParams("eScooter");
      config.planCalcScore().addModeParams(eScooterScoringParams);

    }


    public static void addSharedFreeFloatingService(Config config,SharingConfigGroup configGroup, String serviceArea,Double accessDistance, String name, String mode,String serviceFile) {

        SharingServiceConfigGroup serviceConfig = new SharingServiceConfigGroup();
        configGroup.addService(serviceConfig);

        // ... with a service id. The respective mode will be "sharing:velib".
        serviceConfig.setId(name);

        // ... with freefloating characteristics
        serviceConfig.setMaximumAccessEgressDistance(accessDistance);
        serviceConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.Freefloating);
        serviceConfig.setServiceAreaShapeFile(serviceArea);
        serviceConfig.setServiceInputFile(serviceFile);

        // Save for super method
        //String array[] =new String[]{"Station Based", "Free Floating" };

        //boolean contains = Arrays.stream(array).anyMatch(serviceScheme::equals);


        // ... and, we need to define the underlying mode, here "bike".
        serviceConfig.setMode(mode);

        // considered in mode choice.
        List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
        modes.add(SharingUtils.getServiceMode(serviceConfig));
        config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));
    }
    public static void addSharedStationBasedService(Config config, SharingConfigGroup configGroup, Double accessDistance, String name, String mode, String serviceFile) {

        SharingServiceConfigGroup serviceConfig = new SharingServiceConfigGroup();
        configGroup.addService(serviceConfig);

        // ... with a service id. The respective mode will be "sharing:velib".
        serviceConfig.setId(name);

        // ... with freefloating characteristics
        serviceConfig.setMaximumAccessEgressDistance(accessDistance);
        serviceConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.StationBased);
        serviceConfig.setServiceInputFile(serviceFile);
        // Save for super method
        //String array[] =new String[]{"Station Based", "Free Floating" };

        //boolean contains = Arrays.stream(array).anyMatch(serviceScheme::equals);


        // ... and, we need to define the underlying mode, here "bike".
        serviceConfig.setMode(mode);

        // considered in mode choice.
        List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
        modes.add(SharingUtils.getServiceMode(serviceConfig));
        config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));
    }
    // Name must be as sharing:bikeShare !!
    public static void addSharingServiceToEqasim(Controler controller,Config config, CommandLine cmd, Scenario scenario,String name,String serviceFile){
        EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
// Scoring config definition to add the mode cat_pt parameters
        PlanCalcScoreConfigGroup scoringConfig = config.planCalcScore();
        PlanCalcScoreConfigGroup.ModeParams sharingPTParams = new PlanCalcScoreConfigGroup.ModeParams("Sharing_PT");
        PlanCalcScoreConfigGroup.ModeParams pTSharingParams = new PlanCalcScoreConfigGroup.ModeParams("PT_Sharing");
        PlanCalcScoreConfigGroup.ModeParams SharingPTSharingParams = new PlanCalcScoreConfigGroup.ModeParams("Sharing_PT_Sharing");
        scoringConfig.addModeParams(SharingPTSharingParams);
        scoringConfig.addModeParams(sharingPTParams);
        scoringConfig.addModeParams(pTSharingParams);

        // "car_pt interaction" definition
        PlanCalcScoreConfigGroup.ActivityParams paramsSharingPTInterAct = new PlanCalcScoreConfigGroup.ActivityParams("SharingPT_Interaction");
        paramsSharingPTInterAct.setTypicalDuration(100.0);
        paramsSharingPTInterAct.setScoringThisActivityAtAll(false);

        // "pt_car interaction" definition
        PlanCalcScoreConfigGroup.ActivityParams paramsPTSharingInterAct = new PlanCalcScoreConfigGroup.ActivityParams("PTSharing_Interaction");
        paramsPTSharingInterAct.setTypicalDuration(100.0);
        paramsPTSharingInterAct.setScoringThisActivityAtAll(false);

        // Adding "car_pt interaction" to the scoring
        scoringConfig.addActivityParams(paramsSharingPTInterAct);
        scoringConfig.addActivityParams(paramsPTSharingInterAct);


        // Adding "car_pt interaction" to the scoring
        scoringConfig.addActivityParams(paramsPTSharingInterAct);
        scoringConfig.addActivityParams(paramsSharingPTInterAct);

        //Key Apart from modifying the  binders , add the neww estimators, etc etc
        DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);

        // ONLY FOR FREEFLOATING SERVICES
//        Set<String> tripConstraints = new HashSet<>(dmcConfig.getTripConstraints());
//        tripConstraints.add("ShapeFile");
//        Set<String> tripFilters = new HashSet<>(Arrays.asList("TourLengthFilter"));
//        dmcConfig.setTripConstraints(tripConstraints);
//        //dmcConfig.removeParameterSet(erasedSet);
//        ConfigGroup set = dmcConfig.createParameterSet("tripConstraint:ShapeFile");
//        set.addParam("constrainedModes", "sharing:bikeShare");
//        set.addParam("path", "C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasim-java\\ile_de_france\\src\\main\\resources\\corsica\\extent.shp");
//        set.addParam("requirement", "BOTH");
//        dmcConfig.addParameterSet(set);
        Set<String> cachedModes = new HashSet<>();
        cachedModes.addAll(dmcConfig.getCachedModes());
        cachedModes.add("sharing:"+name);
        dmcConfig.setCachedModes(cachedModes);

        eqasimConfig.setCostModel("sharing:"+name,"sharing:"+name);
        eqasimConfig.setEstimator("sharing:"+name,"sharing:"+name);
//        eqasimConfig.setEstimator("PT_bikeShare","PT_bikeShare");
        dmcConfig.setModeAvailability("KModeAvailability");


        controller.addOverridingModule(new MicroMobilityModeEqasimModeChoiceModule(cmd,scenario,name,serviceFile));


    }

}
