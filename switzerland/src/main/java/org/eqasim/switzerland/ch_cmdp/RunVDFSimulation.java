package org.eqasim.switzerland.ch_cmdp;

import ch.sbb.matsim.config.SBBTransitConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.eqasim.core.simulation.vdf.engine.VDFEngineConfigGroup;
import org.eqasim.switzerland.ch.PTLinkVolumesModule;
import org.eqasim.switzerland.ch.PTPassengerCountsModule;
import org.eqasim.switzerland.ch_cmdp.StrategyWeightDecay.StrategyWeightDecayModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.HashSet;
import java.util.Set;

public class RunVDFSimulation {
    static public void main(String[] args) throws ConfigurationException {
        // set preventwaitingtoentertraffic to y if you want to to prevent that waiting
        // traffic has to wait for space in the link buffer
        // this is especially important to avoid high waiting times when we cutout
        // scenarios from a larger scenario.
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path") //
                .allowPrefixes("mode-parameter", "cost-parameter", "preventwaitingtoentertraffic", "samplingRateForPT", "generateNetworkEvents") //
                .build();

        SwitzerlandConfigurator configurator = new SwitzerlandConfigurator(cmd);
        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));

        // add the options from the command line to the config
        configurator.updateConfig(config);
        configurator.configure(config);
        cmd.applyConfiguration(config);

        // add vdf module to the config
        VDFConfigGroup vdfConfig = VDFConfigGroup.getOrCreate(config);
        VDFEngineConfigGroup vdfEngineConfig = VDFEngineConfigGroup.getOrCreate(config);

        if (cmd.hasOption("preventwaitingtoentertraffic")) {
            if (cmd.getOption("preventwaitingtoentertraffic").get().matches("(?i)(y|yes|true)")) {
                ((QSimConfigGroup) config.getModules().get(QSimConfigGroup.GROUP_NAME))
                        .setPcuThresholdForFlowCapacityEasing(1.0);
            }
        }

        // remove pt from the routed modes
        EqasimConfigGroup eqasimConfigGroup = EqasimConfigGroup.get(config);
        eqasimConfigGroup.setUseScheduleBasedTransport(true);
        // remove bike as a routed mode
        // configurator.removeBikeFromRoutedModes(config);

        // VDF: Disable queue logic
        config.qsim().setFlowCapFactor(1e9);

        // maybe we do not want to disable storage capacity logic
        // as we might want to have some back propagation delays
        config.qsim().setStorageCapFactor(1e9);

        // VDF: Optional
        vdfConfig.setWriteInterval(10);
        vdfConfig.setWriteFlowInterval(10);
        vdfConfig.setModes(Set.of(TransportMode.car, "car_passenger", "truck", "bus"));
        vdfEngineConfig.setModes( Set.of(TransportMode.car, "truck") );

        // VDF Engine: Decide whether to genertae link events or not
        vdfEngineConfig.setGenerateNetworkEvents(false);
        if (cmd.hasOption("generateNetworkEvents")) {
            if (cmd.getOption("generateNetworkEvents").get().matches("(?i)(y|yes|true)")) {
                vdfEngineConfig.setGenerateNetworkEvents(true);
            }
        }

        // VDF Engine: Remove car and truck from main modes
        Set<String> mainModes = new HashSet<>(config.qsim().getMainModes());
        mainModes.remove(TransportMode.car);
        mainModes.remove("truck");
        config.qsim().setMainModes(mainModes);

        // scenario
        Scenario scenario = ScenarioUtils.createScenario(config);

        SwissRailRaptorConfigGroup srrConfigGroup = (SwissRailRaptorConfigGroup) config.getModules().getOrDefault("swissRailRaptor", null);
        System.out.println("Found the following transfer penalties: " + srrConfigGroup.getModeToModeTransferPenaltyParameterSets());

        configurator.configureScenario(scenario);
        ScenarioUtils.loadScenario(scenario);
        configurator.adjustScenario(scenario);
        configurator.adjustPTpcu(scenario);
        Controler controller = new Controler(scenario);

        configurator.configureController(controller);
        controller.addOverridingModule(new PTPassengerCountsModule());
        controller.addOverridingModule(new PTLinkVolumesModule());
        controller.addOverridingModule(new StrategyWeightDecayModule());

        // save config
        ConfigUtils.writeConfig(config, "adapted_config.xml");

        controller.run();
    }
}