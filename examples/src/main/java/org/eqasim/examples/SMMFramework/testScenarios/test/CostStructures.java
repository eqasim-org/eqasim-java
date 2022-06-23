package org.eqasim.examples.SMMFramework.testScenarios.test;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.common.io.Resources;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.examples.SMMFramework.testScenarios.utils.MicromobilityUtils;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.ModeChoiceModuleExample;
import org.eqasim.examples.SMMFramework.testScenarios.utils.SharingRaptorUtils;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceModule;
import org.matsim.contribs.discrete_mode_choice.modules.ModelModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.*;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

public class CostStructures {
    public static void main(String[] args) throws CommandLine.ConfigurationException, FileNotFoundException, IllegalAccessException {


        String baseName=".\\CostScenarios\\Scenario";

        String[] simParams= new String[]{};
        String[] costUnlock=new String[]{"0","0.25","0.5","0.75","1","1.25","1.5","1.75","2"};
        String[] costMin =new String[]{"0","0.05","0.1","0.15","0.2","0.25","0.3","0.35","0.4"};
        for (int i=1;i<3;i++) {
            String fileName = baseName + String.valueOf(i) + ".txt";
            simParams = parseParams(fileName);

            for ( int j=1;j<9;j+=1){
                if(i==1){
                    simParams = parseParams(fileName);
                    simParams[15]= String.valueOf(costUnlock[4]);
                    simParams[17]= String.valueOf(costMin[j]);
                    CommandLine cmd = new CommandLine.Builder(simParams) //
                            .allowOptions("use-rejection-constraint") //
                            .allowPrefixes("mode-parameter", "cost-parameter", "sharing-mode-name") //
                            .build();
                    runAsSMMFramework(cmd,i,(j));

                    simParams = parseParams(fileName);
                    simParams[15]= String.valueOf(costUnlock[j]);
                    simParams[17]= String.valueOf(costMin[4]);
                    cmd = new CommandLine.Builder(simParams) //
                            .allowOptions("use-rejection-constraint") //
                            .allowPrefixes("mode-parameter", "cost-parameter", "sharing-mode-name") //
                            .build();
                    runAsSMMFramework(cmd,i*100,(j));

                }
                if(i==2){
                    simParams = parseParams(fileName);
                    simParams[13]= String.valueOf(costUnlock[4]);
                    simParams[15]= String.valueOf(costMin[j]);
                    CommandLine cmd = new CommandLine.Builder(simParams) //
                            .allowOptions("use-rejection-constraint") //
                            .allowPrefixes("mode-parameter", "cost-parameter", "sharing-mode-name") //
                            .build();
                    runAsSMMFramework(cmd,i,(j));
                    simParams = parseParams(fileName);
                    simParams[13]= String.valueOf(costUnlock[j]);
                    simParams[15]= String.valueOf(costMin[4]);
                     cmd = new CommandLine.Builder(simParams) //
                            .allowOptions("use-rejection-constraint") //
                            .allowPrefixes("mode-parameter", "cost-parameter", "sharing-mode-name") //
                            .build();
                    runAsSMMFramework(cmd,i*100,(j));
                }
            }

        }

    }


    public static void runAsSMMFramework(CommandLine cmd, Integer i,Integer j) throws IllegalAccessException, CommandLine.ConfigurationException {
        URL configUrl = Resources.getResource("corsica/corsica_config.xml");
        Config config = ConfigUtils.loadConfig(configUrl, IDFConfigurator.getConfigGroups());

        config.controler().setLastIteration(1);
        config.qsim().setFlowCapFactor(1e9);
        config.qsim().setStorageCapFactor(1e9);

        // Write out all events (DEBUG)
        config.controler().setWriteEventsInterval(1);
        config.controler().setWritePlansInterval(1);
        config.controler().setLastIteration(25);
        String baseDirectory="./CostSMMResultsLDNoInter/Scenario";
        String nameScenario=baseDirectory+String.valueOf(i)+"_"+String.valueOf(j);
        config.controler().setOutputDirectory(nameScenario);
//        // Set up controller (no specific settings needed for scenario)
        Controler controller = new Controler(config);


        // Set up choice model
        EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
        for (StrategyConfigGroup.StrategySettings strategy : config.strategy().getStrategySettings()) {
            if(strategy.getStrategyName().equals("DiscreteModeChoice")) {
                strategy.setWeight(0.2);
            }
            if(strategy.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultSelector.KeepLastSelected)) {
                strategy.setWeight(0.8);
            }
        }
        for(String option: cmd.getAvailableOptions()){
            String x=option;
            String value= (cmd.getOptionStrict(option));
            if(value.equals("No")){
                DiscreteModeChoiceConfigGroup dmcConfig=DiscreteModeChoiceConfigGroup.getOrCreate(config);
                dmcConfig.setModelType(ModelModule.ModelType.Trip);
                Collection<String> tripF=  dmcConfig.getTripFilters();
                tripF.removeAll(tripF);
                dmcConfig.setTripFilters(tripF);
                break;

            }
        }
        //Key Apart from modifying the  binders , add the neww estimators, etc etc
        eqasimConfig.setEstimator("bike","KBike");
        eqasimConfig.setEstimator("pt","KPT");
        eqasimConfig.setEstimator("car","KCar");
        eqasimConfig.setEstimator("walk","KWalk");
        eqasimConfig.setCostModel("pt","pt");
        eqasimConfig.setCostModel("car","car");
        eqasimConfig.setCostModel("pt","pt");
        eqasimConfig.setCostModel("car","car");
        config.strategy().setFractionOfIterationsToDisableInnovation(0.9);
//        // Set analysis interval
        eqasimConfig.setTripAnalysisInterval(1);


        Scenario scenario = ScenarioUtils.createScenario(config);
        scenario= ScenarioUtils.loadScenario(config);
        IDFConfigurator.configureScenario(scenario);
        IDFConfigurator.configureController(controller);
        controller.addOverridingModule( new DiscreteModeChoiceModule());
        controller.addOverridingModule(new EqasimAnalysisModule());
        controller.addOverridingModule(new EqasimModeChoiceModule());
        controller.addOverridingModule( new ModeChoiceModuleExample(cmd,scenario));
        MicromobilityUtils.addSharingServices(cmd,controller,config,scenario);

        ConfigWriter cw=new ConfigWriter(config);
        cw.write("verificationConfig.xml");
        controller.run();
    }
    public static void runAsSharingRaptor(CommandLine cmd, Integer i){
        Config config = ConfigUtils.loadConfig("C:\\Users\\juan_\\Desktop\\TUM\\Semester 4\\Matsim\\eqasim-java-develop\\examples\\src\\main\\java\\org\\eqasim\\examples\\corsica_drt\\siouxfalls\\config.xml", new ConfigGroup[0]);
        config.removeModule("strategy");
        StrategyConfigGroup stratSettings=new StrategyConfigGroup();
        config.addModule(stratSettings);
        config.qsim().setFlowCapFactor(1e9);
        config.qsim().setStorageCapFactor(1e9);
        {
            StrategyConfigGroup.StrategySettings strat=new StrategyConfigGroup.StrategySettings();
            strat.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice);
            strat.setWeight(0.2);
            config.strategy().addStrategySettings(strat);

        }
        {
            StrategyConfigGroup.StrategySettings strat=new StrategyConfigGroup.StrategySettings();
            strat.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.KeepLastSelected);
            strat.setWeight(0.8);
            config.strategy().addStrategySettings(strat);

        }
        config.strategy().setFractionOfIterationsToDisableInnovation(0.9);
        config.controler().setWriteEventsInterval(1);
        config.controler().setWritePlansInterval(1);
        config.controler().setLastIteration(50);
        String baseDirectory="./ComputationalSRResults/Scenario";
        String nameScenario=baseDirectory+String.valueOf(i);
        config.controler().setOutputDirectory(nameScenario);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controller = new Controler(scenario);

        try {
            SharingRaptorUtils.addSharingServicesCharyParNagel(cmd,controller,config,scenario);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        controller.addOverridingModule(new SwissRailRaptorModule());
        controller.run();
        ConfigWriter cw= new ConfigWriter(config);
        cw.write("CorsicaRaptorConfig.xml");
    }
    public static String[] parseParams(String file) throws FileNotFoundException {
        File txt = new File(file);
        Scanner scan = new Scanner(txt);
        ArrayList<String> data = new ArrayList<String>() ;
        while(scan.hasNextLine()){
            data.add(scan.nextLine());
        }
        System.out.println(data);
        String[] simpleArray = data.toArray(new String[]{});
        return simpleArray;
    }
}
