package org.eqasim.examples.SMMFramework.testScenarios.utils;


import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

import org.eqasim.examples.SMMFramework.GBFSUtils.ReadFreeFloatingGBFS;
import org.eqasim.examples.SMMFramework.GBFSUtils.ReadStationBasedGBFS;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.sharing.run.SharingConfigGroup;
import org.matsim.contrib.sharing.run.SharingModule;
import org.matsim.contrib.sharing.run.SharingServiceConfigGroup;
import org.matsim.contrib.sharing.service.SharingUtils;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;

import java.io.IOException;
import java.util.*;

public class SharingRaptorUtils implements ParameterDefinition {

    public String mode;
    public String serviceScheme;
    public String serviceArea=null;
    public Double accessDist;

    public static void main(String[] args) throws IllegalAccessException {
//        addSharingService(ConfigUtils.loadConfig("C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\examples\\src\\main\\java\\org\\eqasim\\examples\\corsica_drt\\siouxfalls\\config.xml"),"Shared-Bike","Station Based",10000.0,null,null,null);
//        addSharingService(ConfigUtils.loadConfig("C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\examples\\src\\main\\java\\org\\eqasim\\examples\\corsica_drt\\siouxfalls\\config.xml"),"eScooter","None",10000.0,null,null,null);
    }



    public static void  addSharingServicesCharyParNagel(CommandLine cmd,Controler controller, Config config,Scenario scenario) throws IllegalAccessException {
        HashMap<String,HashMap<String,String>> sharingServicesInput=applyCommandLineServices("sharing-mode-name",cmd);
        try {
            generateServiceFile(sharingServicesInput,config,scenario);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for( String key :sharingServicesInput.keySet()) {
            HashMap<String,String>service=sharingServicesInput.get(key);

//            "Service_File", "Mode", "Scheme","Service_Name","Intermodal","AccessEgress_Distance"
            String serviceFile=service.get("Service_File");
            String mode=service.get("Mode");
            String name=service.get("Service_Name");
            Double accessEgress=Double.parseDouble(service.get("AccessEgress_Distance"));
            String scheme=service.get("Scheme");
            String multimodal=service.get("Multimodal");
            String serviceArea="";
            if(service.keySet().contains("Service_Area")){
                serviceArea=service.get("Service_Area");
            }
            addSharingService(controller,config,mode,scheme,accessEgress,serviceArea,serviceFile,name,multimodal);
//

        }
        PlanCalcScoreConfigGroup scoringConfig=config.planCalcScore();
        // "car_pt interaction" definition
        PlanCalcScoreConfigGroup.ActivityParams paramsSharingPTInterAct = new PlanCalcScoreConfigGroup.ActivityParams("SharingPT interaction");
        paramsSharingPTInterAct.setTypicalDuration(100.0);
        paramsSharingPTInterAct.setScoringThisActivityAtAll(false);

        // "pt_car interaction" definition
        PlanCalcScoreConfigGroup.ActivityParams paramsPTSharingInterAct = new PlanCalcScoreConfigGroup.ActivityParams("PTSharing interaction");
        paramsPTSharingInterAct.setTypicalDuration(100.0);
        paramsPTSharingInterAct.setScoringThisActivityAtAll(false);

        // Adding "car_pt interaction" to the scoring
        scoringConfig.addActivityParams(paramsSharingPTInterAct);
        scoringConfig.addActivityParams(paramsPTSharingInterAct);


        // Adding "car_pt interaction" to the scoring
        scoringConfig.addActivityParams(paramsPTSharingInterAct);
        scoringConfig.addActivityParams(paramsSharingPTInterAct);

        configureSwissRailRaptorMultimodal(config,sharingServicesInput);
        controller.addOverridingModule(new SharingModule());
        controller.configureQSimComponents(SharingUtils.configureQSim((SharingConfigGroup) config.getModules().get("sharing")));

    }
    public static void addSharingService(Controler controller, Config config, String mode, String serviceScheme, Double accessDist, String serviceArea, String serviceFile, String name, String multimodal) throws IllegalAccessException {


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
        String serviceSchemes[] = new String[]{"Station-Based", "Free-floating"};
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
                if (serviceScheme.equals("Station-Based")) {
                    addSharedStationBasedService(config, sharingConfigGroup, accessDist, name, mode, serviceFile);
                } else if (serviceScheme.equals("Free-floating")) {
                    addSharedFreeFloatingService(config, sharingConfigGroup, serviceArea, accessDist, name, mode, serviceFile);
                }
//                if(multimodal.equals("Yes")){
//                    List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
//                    if(modes.contains(SharingUtils.getServiceMode(serviceConfig))) {
//                        modes.add(SharingUtils.getServiceMode(serviceConfig));
//                    }
//                    config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));
//
//                }
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


        PlansCalcRouteConfigGroup.ModeRoutingParams walkRoutingParams = new PlansCalcRouteConfigGroup.ModeRoutingParams("walk");
        walkRoutingParams.setTeleportedModeSpeed(2.0);
        walkRoutingParams.setBeelineDistanceFactor(1.3);
        config.plansCalcRoute().addModeRoutingParams(walkRoutingParams);

        PlansCalcRouteConfigGroup.ModeRoutingParams normalBikeRoutingParams = new PlansCalcRouteConfigGroup.ModeRoutingParams("bike");
        normalBikeRoutingParams.setTeleportedModeSpeed(3.44);
        normalBikeRoutingParams.setBeelineDistanceFactor(1.3);
        config.plansCalcRoute().addModeRoutingParams(normalBikeRoutingParams);

        // We need to score bike
        PlanCalcScoreConfigGroup.ModeParams bikeSC = new PlanCalcScoreConfigGroup.ModeParams("bike");
        config.planCalcScore().addModeParams(bikeSC);
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


        // ... and, we need to define the underlying mode, here "bike".
        serviceConfig.setMode(mode);

        // considered in mode choice.
        List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
        modes.add(SharingUtils.getServiceMode(serviceConfig));
        config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));
    }
    public static void configureSwissRailRaptorMultimodal(Config config,HashMap<String,HashMap<String,String>>services) {
        config.removeModule("swissRailRaptor");
        // add SRR config group
        SwissRailRaptorConfigGroup sRR = new SwissRailRaptorConfigGroup();
        // Allows intermodal
        sRR.setUseIntermodalAccessEgress(true);

        //Creates the parameter for the first walk
        ConfigGroup swissRaptorset = sRR.createParameterSet("intermodalAccessEgress");
        swissRaptorset.addParam("mode", "walk");
        swissRaptorset.addParam("maxRadius", "10000");
        swissRaptorset.addParam("initialSearchRadius", "1000");
        swissRaptorset.addParam("searchExtensionRadius", "10000");
        sRR.addParameterSet(swissRaptorset);


        for (String key : services.keySet()) {
            HashMap<String, String> service = services.get(key);

//            "Service_File", "Mode", "Scheme","Service_Name","Intermodal","AccessEgress_Distance"
            String name = service.get("Service_Name");
            Double accessEgress = Double.parseDouble(service.get("AccessEgress_Distance"));
            String multimodal = service.get("Multimodal");
            if (multimodal.equals("True")) {
                ConfigGroup swissRaptorset2 = sRR.createParameterSet("intermodalAccessEgress");
                swissRaptorset2.addParam("mode", "sharing:" + name);
                swissRaptorset2.addParam("maxRadius", "10000");
                swissRaptorset2.addParam("initialSearchRadius", "1000");
                swissRaptorset2.addParam("searchExtensionRadius", "200");
                sRR.addParameterSet(swissRaptorset2);
            }

        }
        config.addModule(sRR);
    }
    public static void addSharedFreeFloatingService(Config config,SharingConfigGroup configGroup,HashMap<String,String>service) {

        SharingServiceConfigGroup serviceConfig = new SharingServiceConfigGroup();
        configGroup.addService(serviceConfig);

        // ... with a service id. The respective mode will be "sharing:velib".
        serviceConfig.setId(service.get("Name"));

        // ... with freefloating characteristics
        serviceConfig.setMaximumAccessEgressDistance(Double.parseDouble(service.get("AccessEgress_Distance")));
        serviceConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.Freefloating);
        if(service.keySet().contains("Service_Area")) {
            serviceConfig.setServiceAreaShapeFile(service.get("Service_Area"));
        }

        serviceConfig.setServiceInputFile(service.get("Service_File"));

        // ... and, we need to define the underlying mode, here "bike".
        serviceConfig.setMode(service.get("Mode"));

        // considered in mode choice.
        List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
        if(modes.contains(SharingUtils.getServiceMode(serviceConfig))) {
            modes.add(SharingUtils.getServiceMode(serviceConfig));
        }
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

        serviceConfig.setMode(mode);

        // considered in mode choice.
        List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
        modes.add(SharingUtils.getServiceMode(serviceConfig));
        config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));
    }


    static public HashMap<String,HashMap<String,String>> applyCommandLineServices(String prefix, CommandLine cmd) {
        HashMap<String,HashMap<String,String>>  sharingModesInput= new HashMap<>();
        List<String> sharingModes=indentifySharingModes(prefix,cmd);
        int i=0;
        while(i<sharingModes.size()){
            sharingModesInput.put(sharingModes.get(i), new HashMap<String,String>());
            i+=1;
        }
        buildParameters(prefix,cmd,sharingModesInput);
        validateInputGBFS(sharingModesInput);
        return sharingModesInput;
    }
    static public List<String> indentifySharingModes(String prefix, CommandLine cmd) {
        List<String> sharingModes= new ArrayList<>();
        for (String option : cmd.getAvailableOptions()) {
            if (option.startsWith(prefix + ":")) {
                try {
                    String optionPart2=option.split(":")[1];
                    if (optionPart2.startsWith("Service_Name")) {
                        sharingModes.add(cmd.getOptionStrict(option));
                    }
                } catch (CommandLine.ConfigurationException e) {
                    //Should not happen
                }
            }
        }
        return sharingModes;
    }
    static public void buildParameters(String prefix,CommandLine cmd, HashMap<String,HashMap<String,String>>services) {

        for (String option : cmd.getAvailableOptions()) {
            if (option.startsWith(prefix + ":")) {
                try {
                    String optionPart2=option.split(":")[1];
                    String parameter=optionPart2.split("\\.")[0];
                    String serviceName=optionPart2.split("\\.")[1];
                    HashMap<String,String>mapService=services.get(serviceName);
                    mapService.put(parameter,cmd.getOptionStrict(option));

                } catch (CommandLine.ConfigurationException e) {
                    //Should not happen
                }
            }
        }
    }
    static public void validateInput( HashMap<String,HashMap<String,String>> services) {
        ArrayList<String> obligatoryValues = new ArrayList<>(Arrays.asList("Service_File", "Mode", "Scheme","Service_Name","Multimodal","AccessEgress_Distance"));
        for (String key : services.keySet()) {
            HashMap<String,String>service=(HashMap)services.get(key);
            if (service.keySet().containsAll(obligatoryValues)== false) {

                throw new IllegalArgumentException("Please check the service parameters for the service: "+key+"there must be a GBFS, a Mode file , Scheme type and if its multimodal");
            }
        }
    }
    static public void validateInputGBFS( HashMap<String,HashMap<String,String>> services) {
        ArrayList<String> obligatoryValues = new ArrayList<>(Arrays.asList("Mode", "Scheme","Service_Name","Multimodal","AccessEgress_Distance"));
        for (String key : services.keySet()) {
            HashMap<String,String>service=(HashMap)services.get(key);
            if(service.get("Scheme").equals("Station-Based")){
                obligatoryValues = new ArrayList<>(Arrays.asList("Mode", "Scheme","Service_Name","Multimodal","AccessEgress_Distance","StationsGBFS","StationsStatusGBFS"));
                if (service.keySet().containsAll(obligatoryValues)== false) {


                    throw new IllegalArgumentException("Please check the service parameters for the service: "+key+"there must be  two GBFS, a Mode file , Scheme type and if its multimodal");
                }

            }else{
                obligatoryValues = new ArrayList<>(Arrays.asList("Mode", "Scheme","Service_Name","Multimodal","AccessEgress_Distance","FreeVehiclesGBFS"));
                if (service.keySet().containsAll(obligatoryValues)== false) {


                    throw new IllegalArgumentException("Please check the service parameters for the service: "+key+"there must be a GBFS, a Mode file , Scheme type and if its multimodal");
                }

            }

        }
    }
    public static void generateServiceFile(HashMap<String,HashMap<String,String>>services, Config config,Scenario scenario) throws IOException {
        for (String key : services.keySet()) {
            HashMap<String, String> service = (HashMap) services.get(key);
            if(service.get("Scheme").equals("Station-Based")){
                Network network=scenario.getNetwork();
                service.put("Service_File", ReadStationBasedGBFS.readGBFSStationBased(service.get("StationsGBFS"),"Perro",network,service.get("StationsStatusGBFS"),service.get("Service_Name"))) ;
            }
            if(service.get("Scheme").equals("Free-floating")){
                Network network=scenario.getNetwork();
                service.put("Service_File", ReadFreeFloatingGBFS.readGBFSFreeFloating(service.get("FreeVehiclesGBFS"),"Perro",network,service.get("Service_Name"))) ;
            }
        }
    }
}


