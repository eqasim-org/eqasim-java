package org.eqasim.examples.corsica_drt.generalizedMicromobility;

import com.google.common.io.Resources;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.examples.corsica_drt.sharingPt.GeneralizedSharingPT.GeneralizedSharingPTModule;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.sharing.run.SharingConfigGroup;
import org.matsim.contrib.sharing.run.SharingModule;
import org.matsim.contrib.sharing.service.SharingUtils;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SharingMultiplePlusEqasimConfigurableExample {
    static public void main(String[] args) throws CommandLine.ConfigurationException, IllegalAccessException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .allowOptions("use-rejection-constraint") //
                .allowPrefixes("mode-parameter", "cost-parameter","sharing-mode-name") //
                .build();
        URL configUrl = Resources.getResource("corsica/corsica_config.xml");

        Config config = ConfigUtils.loadConfig(configUrl, IDFConfigurator.getConfigGroups());

        config.controler().setLastIteration(3);
        config.qsim().setFlowCapFactor(1e9);
        config.qsim().setStorageCapFactor(1e9);

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
         MicromobilityUtils.addSharingService(controller,config,"Shared-Bike","Station Based",10000.0,null,"C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\examples\\src\\main\\java\\org\\eqasim\\examples\\corsica_drt\\generalizedMicromobility\\SBBSDummy.xml","bikeShare");
         //MicromobilityUtils.addSharingService(controller, config, "eScooter", "Free Floating", 100.0, null, "shared_taxi_vehicles_stations.xml", "eScooter");
        cmd.applyConfiguration(config);
        { // Add the DRT mode to the choice model
            DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);


            Set<String> tripConstraints = new HashSet<>(dmcConfig.getTripConstraints());
//            tripConstraints.add("ShapeFile");
            Set<String> tripFilters = new HashSet<>(Arrays.asList("TourLengthFilter"));
            tripConstraints.add("SHARING_PT_CONSTRAINT");
            dmcConfig.setTripConstraints(tripConstraints);

            //dmcConfig.removeParameterSet(erasedSet);
//            ConfigGroup set = dmcConfig.createParameterSet("tripConstraint:ShapeFile");
//            set.addParam("constrainedModes", "sharing:bikeShare");
//            set.addParam("path", "C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasim-java\\ile_de_france\\src\\main\\resources\\corsica\\extent.shp");
//            set.addParam("requirement", "BOTH");
//            dmcConfig.addParameterSet(set);

//			// Add the Trip constraints to tour filters
//			Set<String> tourConstraints = new HashSet<>(dmcConfig.getTourConstraints());
//			tourConstraints.add("FromTripBased");
//			dmcConfig.setTourConstraints(tourConstraints);
            // Add DRT to cached modes
            Set<String> cachedModes = new HashSet<>();
            cachedModes.addAll(dmcConfig.getCachedModes());
//			cachedModes.add("drt");
            cachedModes.add("bikeShare_PT");
            cachedModes.add("bikeShare_PT_bikeShare");
            //cachedModes.add("PT_bikeShare");
            cachedModes.add("pedelec");
            dmcConfig.setCachedModes(cachedModes);
            dmcConfig.setModeAvailability("KModeAvailability");
        }




        // Set up choice model
        EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

        //Key Apart from modifying the  binders , add the neww estimators, etc etc
        eqasimConfig.setEstimator("bike","KBike");

        eqasimConfig.setEstimator("pt","KPT");
        eqasimConfig.setEstimator("bikeShare_PT","bikeShare_PT");
        eqasimConfig.setEstimator("bikeShare_PT_bikeShare","bikeShare_PT_bikeShare");
        //eqasimConfig.setEstimator("PT_bikeShare","PT_bikeShare");
        eqasimConfig.setEstimator("car","KCar");
        eqasimConfig.setEstimator("walk","KWalk");
        // Set analysis interval
        eqasimConfig.setTripAnalysisInterval(1);

//

        Scenario scenario = ScenarioUtils.createScenario(config);
        IDFConfigurator.configureScenario(scenario);


        ScenarioUtils.loadScenario(scenario);

        // Set up controller (no specific settings needed for scenario)


        IDFConfigurator.configureController(controller);
        controller.addOverridingModule(new EqasimAnalysisModule());
        controller.addOverridingModule(new EqasimModeChoiceModule());
        controller.addOverridingModule(new IDFModeChoiceModule(cmd));
        controller.addOverridingModule(new ModeChoiceModuleExample(cmd,scenario));
        controller.addOverridingModule(new GeneralizedSharingPTModule(scenario,"bikeShare"));
        MicromobilityUtils.addSharingServiceToEqasim(controller,config,cmd,scenario,"bikeShare","C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\examples\\src\\main\\java\\org\\eqasim\\examples\\corsica_drt\\generalizedMicromobility\\SBBSDummy.xml");
        controller.addOverridingModule(new SharingModule());



        // Enable QSim components
        controller.configureQSimComponents(SharingUtils.configureQSim((SharingConfigGroup) config.getModules().get("sharing")));
        controller.run();
}
}
