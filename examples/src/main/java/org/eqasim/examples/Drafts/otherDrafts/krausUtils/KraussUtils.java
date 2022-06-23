package org.eqasim.examples.Drafts.otherDrafts.krausUtils;

import org.matsim.contrib.sharing.run.SharingConfigGroup;
import org.matsim.contrib.sharing.run.SharingServiceConfigGroup;
import org.matsim.contrib.sharing.service.SharingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import java.util.*;

public class KraussUtils {

    public static void addMicromobilityToEqasim(Config config, String id,String type,int maxEgress,String serviceFile,String serviceType){
        SharingConfigGroup sharingConfig = new SharingConfigGroup();
        config.addModule(sharingConfig);
        // We need to define a service ...
        SharingServiceConfigGroup serviceConfig = new SharingServiceConfigGroup();
        sharingConfig.addService(serviceConfig);
        // ... with a service id. The respective mode will be "sharing:velib".
        if(type=="bikeshare"){
            serviceConfig.setMode("bike");


        }else{
            serviceConfig.setMode("bike");
        }
        serviceConfig.setId(id);

        // ... with freefloating characteristics
        serviceConfig.setMaximumAccessEgressDistance(maxEgress);
        if(serviceType=="free floating"){
            serviceConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.Freefloating);

        }else{
            serviceConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.StationBased);
        }

        //serviceConfig.setServiceAreaShapeFile("C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasim-java\\ile_de_france\\src\\main\\resources\\corsica\\corsica_config.xml");

        // ... with a number of available vehicles and their initial locations
        serviceConfig.setServiceInputFile(serviceFile);


        // Finally, we need to make sure that the service mode (sharing:velib) is
        // considered in mode choice.
        List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
        modes.add(SharingUtils.getServiceMode(serviceConfig));
        config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));

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
    }

}



