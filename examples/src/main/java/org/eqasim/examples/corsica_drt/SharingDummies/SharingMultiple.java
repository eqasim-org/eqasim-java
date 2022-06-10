package org.eqasim.examples.corsica_drt.SharingDummies;

import org.eqasim.examples.corsica_drt.generalizedMicromobility.MicromobilityUtils;
import org.matsim.contrib.sharing.run.SharingConfigGroup;
import org.matsim.contrib.sharing.run.SharingModule;
import org.matsim.contrib.sharing.service.SharingUtils;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

public class SharingMultiple {
    static public void main(String[] args) throws CommandLine.ConfigurationException, IllegalAccessException {

        Config config = ConfigUtils.loadConfig("C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\examples\\src\\main\\java\\org\\eqasim\\examples\\corsica_drt\\siouxfalls\\config.xml");
//
//        // We define bike to be routed based on Euclidean distance.
//        PlansCalcRouteConfigGroup.ModeRoutingParams bikeRoutingParams = new PlansCalcRouteConfigGroup.ModeRoutingParams("bike");
//        bikeRoutingParams.setTeleportedModeSpeed(5.0);
//        bikeRoutingParams.setBeelineDistanceFactor(1.3);
//        config.plansCalcRoute().addModeRoutingParams(bikeRoutingParams);
//////////////////////// ADD E SCOOTER////////////////////////////////////////////////////
//        // We define bike to be routed based on Euclidean distance.
//        PlansCalcRouteConfigGroup.ModeRoutingParams eScooterRoutingParams = new PlansCalcRouteConfigGroup.ModeRoutingParams("eScooter");
//        eScooterRoutingParams.setTeleportedModeSpeed(3.0);
//        eScooterRoutingParams.setBeelineDistanceFactor(1.3);
//        config.plansCalcRoute().addModeRoutingParams(eScooterRoutingParams);
//
//
//        // Walk is deleted by adding bike here, we need to re-add it ...
//        PlansCalcRouteConfigGroup.ModeRoutingParams walkRoutingParams = new PlansCalcRouteConfigGroup.ModeRoutingParams("walk");
//        walkRoutingParams.setTeleportedModeSpeed(2.0);
//        walkRoutingParams.setBeelineDistanceFactor(1.3);
//        config.plansCalcRoute().addModeRoutingParams(walkRoutingParams);
//
//        // By default, "bike" will be simulated using teleportation.
//
//        // We need to add the sharing config group
//        SharingConfigGroup sharingConfig = new SharingConfigGroup();
//        config.addModule(sharingConfig);
//
//        // We need to define a service ...
//        SharingServiceConfigGroup serviceConfig = new SharingServiceConfigGroup();
//        sharingConfig.addService(serviceConfig);
//
//        // ... with a service id. The respective mode will be "sharing:velib".
//        serviceConfig.setId("velib");
//
//        // ... with freefloating characteristics
//        serviceConfig.setMaximumAccessEgressDistance(100000);
//        serviceConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.StationBased);
//        serviceConfig.setServiceAreaShapeFile(null);
//
//        // ... with a number of available vehicles and their initial locations
//        // the following file is an example and it works with the siouxfalls-2014 scenario
//        serviceConfig.setServiceInputFile("shared_taxi_vehicles_stations.xml");
//
//        // ... and, we need to define the underlying mode, here "bike".
//        serviceConfig.setMode("bike");
//        // Finally, we need to make sure that the service mode (sharing:velib) is
//        // considered in mode choice.
//        List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
//        modes.add(SharingUtils.getServiceMode(serviceConfig));
//        config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));
//
//
//        //////////// ADD E SCOOTER TO SHARING CONFIG///////////////////////
//        // We need to define a service ...
//        SharingServiceConfigGroup eScooterServiceConfig = new SharingServiceConfigGroup();
//        sharingConfig.addService(eScooterServiceConfig);
//
//        // ... with a service id. The respective mode will be "sharing:velib".
//        eScooterServiceConfig.setId("eScooter");
//
//        // ... with freefloating characteristics
//        eScooterServiceConfig.setMaximumAccessEgressDistance(10000);
//        eScooterServiceConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.StationBased);
//        eScooterServiceConfig.setServiceAreaShapeFile(null);
//
//        // ... with a number of available vehicles and their initial locations
//        // the following file is an example and it works with the siouxfalls-2014 scenario
//        eScooterServiceConfig.setServiceInputFile("shared_taxi_vehicles_stations.xml");
//
//        // ... and, we need to define the underlying mode, here "bike".
//        eScooterServiceConfig.setMode("eScooter");
//
//        // Finally, we need to make sure that the service mode (sharing:velib) is
//        // considered in mode choice.
//
//        modes.add(SharingUtils.getServiceMode(eScooterServiceConfig));
//        config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));


//        // We need to add interaction activity types to scoring
//        PlanCalcScoreConfigGroup.ActivityParams pickupParams = new PlanCalcScoreConfigGroup.ActivityParams(SharingUtils.PICKUP_ACTIVITY);
//        pickupParams.setScoringThisActivityAtAll(false);
//        config.planCalcScore().addActivityParams(pickupParams);
//
//        PlanCalcScoreConfigGroup.ActivityParams dropoffParams = new PlanCalcScoreConfigGroup.ActivityParams(SharingUtils.DROPOFF_ACTIVITY);
//        dropoffParams.setScoringThisActivityAtAll(false);
//        config.planCalcScore().addActivityParams(dropoffParams);
//
//        PlanCalcScoreConfigGroup.ActivityParams bookingParams = new PlanCalcScoreConfigGroup.ActivityParams(SharingUtils.BOOKING_ACTIVITY);
//        bookingParams.setScoringThisActivityAtAll(false);
//        config.planCalcScore().addActivityParams(bookingParams);

//        // We need to score bike
//        PlanCalcScoreConfigGroup.ModeParams bikeScoringParams = new PlanCalcScoreConfigGroup.ModeParams("bike");
//        config.planCalcScore().addModeParams(bikeScoringParams);
//        // Score e Scooter
//        PlanCalcScoreConfigGroup.ModeParams eScooterScoringParams = new PlanCalcScoreConfigGroup.ModeParams("eScooter");
//        config.planCalcScore().addModeParams(eScooterScoringParams);
        // Write out all events (DEBUG)
        config.controler().setWriteEventsInterval(1);
        config.controler().setWritePlansInterval(1);
        config.controler().setLastIteration(10);

        // Set up controller (no specific settings needed for scenario)
        Controler controller = new Controler(config);

//        // Does not really "override" anything
//        controller.addOverridingModule(new SharingModule());
//
//        // Enable QSim components
//        controller.configureQSimComponents(SharingUtils.configureQSim(sharingConfig));
         MicromobilityUtils.addSharingService(controller,config,"Shared-Bike","Station Based",10000.0,null,"shared_taxi_vehicles_stations.xml","BikeSharing", "No");
         MicromobilityUtils.addSharingService(controller, config, "eScooter", "Free Floating", 100.0, null, "shared_taxi_vehicles_stations.xml", "eScooter", "Yes");
        controller.addOverridingModule(new SharingModule());

        // Enable QSim components
        controller.configureQSimComponents(SharingUtils.configureQSim((SharingConfigGroup) config.getModules().get("sharing")));
        controller.run();
}
}
