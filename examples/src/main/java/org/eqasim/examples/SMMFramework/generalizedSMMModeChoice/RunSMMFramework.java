package org.eqasim.examples.SMMFramework.generalizedSMMModeChoice;

import com.google.common.io.Resources;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.examples.SMMFramework.testScenarios.utils.MicromobilityUtils;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.modules.ModelModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class RunSMMFramework {
    static public void main(String[] args) throws CommandLine.ConfigurationException, IllegalAccessException, IOException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .allowOptions("use-rejection-constraint") //
                .allowPrefixes("mode-parameter", "cost-parameter","sharing-mode-name") //
                .build();

// I
//        applyCommandLine("sharing-mode-name",cmd);
//        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
////        new MatsimNetworkReader(scenario.getNetwork()).readFile("C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\ile_de_france\\src\\main\\resources\\corsica\\corsica_network.xml.gz");
////        new PopulationReader(scenario).readFile("C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\ile_de_france\\src\\main\\resources\\corsica\\corsica_population.xml.gz");
//
//        GeneralizedModeAvailability modeAvailability=new MicroMobilityModeEqasimModeChoiceModule(cmd,scenario,"x","x").provideModeAvailability();
//        String string="uwu";
        URL configUrl = Resources.getResource("corsica/corsica_config.xml");
        Config config = ConfigUtils.loadConfig(configUrl, IDFConfigurator.getConfigGroups());

        config.controler().setLastIteration(3);
        config.qsim().setFlowCapFactor(1e9);
        config.qsim().setStorageCapFactor(1e9);

        // Write out all events (DEBUG)
        config.controler().setWriteEventsInterval(1);
        config.controler().setWritePlansInterval(1);
        config.controler().setLastIteration(50);

//        // Set up controller (no specific settings needed for scenario)
        Controler controller = new Controler(config);
//
////        // Does not really "override" anything
////        controller.addOverridingModule(new SharingModule());
////
////        // Enable QSim components
////        controller.configureQSimComponents(SharingUtils.configureQSim(sharingConfig));
//         MicromobilityUtils.addSharingService(controller,config,"Shared-Bike","Station Based",10000.0,null,"C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\examples\\src\\main\\java\\org\\eqasim\\examples\\corsica_drt\\generalizedMicromobility\\SBBSDummy.xml","bikeShare");
//         //MicromobilityUtils.addSharingService(controller, config, "eScooter", "Free Floating", 100.0, null, "shared_taxi_vehicles_stations.xml", "eScooter");
//        cmd.applyConfiguration(config);
//        { // Add the DRT mode to the choice model
//            DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);
//
//
//            Set<String> tripConstraints = new HashSet<>(dmcConfig.getTripConstraints());
////            tripConstraints.add("ShapeFile");
//            Set<String> tripFilters = new HashSet<>(Arrays.asList("TourLengthFilter"));
//            tripConstraints.add("bikeShare"+"_PT_CONSTRAINT");
//            dmcConfig.setTripConstraints(tripConstraints);
//
//            Set<String> cachedModes = new HashSet<>();
//            cachedModes.addAll(dmcConfig.getCachedModes());
////			cachedModes.add("drt");
//            cachedModes.add("bikeShare_PT");
//            cachedModes.add("bikeShare_PT_bikeShare");
//
//            dmcConfig.setCachedModes(cachedModes);
//            dmcConfig.setModeAvailability("ModeAvailability");
//        }
//
//
////

        Scenario scenario = ScenarioUtils.createScenario(config);
        scenario= ScenarioUtils.loadScenario(config);
        IDFConfigurator.configureScenario(scenario);

//

//
//        // Set up controller (no specific settings needed for scenario)
//
//        config.removeModule("strategy");
//        StrategyConfigGroup stratSettings=new StrategyConfigGroup();
//        config.addModule(stratSettings);
//        config.strategy().setMaxAgentPlanMemorySize(1);
//        {
//            StrategyConfigGroup.StrategySettings strat=new StrategyConfigGroup.StrategySettings();
//            strat.setStrategyName("DiscreteModeChoice");
//
//            strat.setWeight(0.05);
//            config.strategy().addStrategySettings(strat);
//
//        }
//        {
//            StrategyConfigGroup.StrategySettings strat=new StrategyConfigGroup.StrategySettings();
//            strat.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
//            strat.setWeight(0.95);
//            config.strategy().addStrategySettings(strat);
//
//        }

        IDFConfigurator.configureController(controller);
        DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
                .get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

        dmcConfig.setModeAvailability("GENMODE");

//
//
        // Set up choice model
        EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

        //Key Apart from modifying the  binders , add the neww estimators, etc etc
        eqasimConfig.setEstimator("bike","KBike");

//
        eqasimConfig.setEstimator("pt","KPT");
//        eqasimConfig.setEstimator("bikeShare_PT","bikeShare_PT");
//        eqasimConfig.setEstimator("bikeShare_PT_bikeShare","bikeShare_PT_bikeShare");
//        //eqasimConfig.setEstimator("PT_bikeShare","PT_bikeShare");
        eqasimConfig.setEstimator("car","KCar");
        eqasimConfig.setEstimator("walk","KWalk");
        eqasimConfig.setCostModel("pt","pt");
        eqasimConfig.setCostModel("car","car");


        dmcConfig.setModelType(ModelModule.ModelType.Trip);

        Collection<String> tripF=  dmcConfig.getTripFilters();
        tripF.removeAll(tripF);
        dmcConfig.setTripFilters(tripF);
//        // Set analysis interval
        eqasimConfig.setTripAnalysisInterval(1);
//
        controller.addOverridingModule(new EqasimAnalysisModule());
        controller.addOverridingModule(new EqasimModeChoiceModule());
//        controller.addOverridingModule(new IDFModeChoiceModule(cmd));
        controller.addOverridingModule( new ModeChoiceModuleExample(cmd,scenario));
        MicromobilityUtils.addSharingServices(cmd,controller,config,scenario);
//        controller.addOverridingModule(new GeneralizedSharingPTModule(scenario,"bikeShare"));
//        MicromobilityUtils.addSharingServiceToEqasim(controller,config,cmd,scenario,"bikeShare","C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\examples\\src\\main\\java\\org\\eqasim\\examples\\corsica_drt\\generalizedMicromobility\\SBBSDummy.xml");
//        controller.addOverridingModule(new SharingModule());
//
//
//
//        // Enable QSim components
//        controller.configureQSimComponents(SharingUtils.configureQSim((SharingConfigGroup) config.getModules().get("sharing")));
        ConfigWriter cw=new ConfigWriter(config);
        cw.write("verificationConfig.xml");
        controller.run();

}

    static public HashMap<String,HashMap<String,String>> applyCommandLine(String prefix, CommandLine cmd) {
        HashMap<String,HashMap<String,String>>  sharingModesInput= new HashMap<>();
        List<String> sharingModes=indentifySharingModes(prefix,cmd);
        int i=0;
        while(i<sharingModes.size()){
            sharingModesInput.put(sharingModes.get(i), new HashMap<String,String>());
            i+=1;
        }
        buildParameters(prefix,cmd,sharingModesInput);
        validateInput(sharingModesInput);
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
        ArrayList<String> obligatoryValues = new ArrayList<>(Arrays.asList("Service_File", "Mode", "Scheme","Service_Name"));
        for (String key : services.keySet()) {
                HashMap<String,String>service=(HashMap)services.get(key);
            if (service.keySet().containsAll(obligatoryValues)== false) {


                throw new IllegalArgumentException("Please check the service parameters for the service: "+key+"there must be a GBFS, a Mode file and the Scheme type");
            }
        }
    }
}
