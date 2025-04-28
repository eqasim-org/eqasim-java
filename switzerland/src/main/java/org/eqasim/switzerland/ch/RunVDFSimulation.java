package org.eqasim.switzerland.ch;

import java.util.HashSet;
import java.util.Set;

import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.eqasim.core.simulation.vdf.engine.VDFEngineConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunVDFSimulation {
    static public void main(String[] args) throws ConfigurationException {
        // set preventwaitingtoentertraffic to y if you want to to prevent that waiting
        // traffic has to wait for space in the link buffer
        // this is especially important to avoid high waiting times when we cutout
        // scenarios from a larger scenario.
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path") //
                .allowPrefixes("mode-parameter", "cost-parameter", "preventwaitingtoentertraffic") //
                .build();

        SwitzerlandConfigurator configurator = new SwitzerlandConfigurator(cmd);
        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
        configurator.updateConfig(config);
        cmd.applyConfiguration(config);

        if (cmd.hasOption("preventwaitingtoentertraffic")) {
            if (cmd.getOption("preventwaitingtoentertraffic").get().equals("y")) {
                ((QSimConfigGroup) config.getModules().get(QSimConfigGroup.GROUP_NAME))
                        .setPcuThresholdForFlowCapacityEasing(1.0);
            }
        }

        // VDF: Add config group
        config.addModule(new VDFConfigGroup());

        // VDF: Set capacity factor instead (~0.1 for a 10% simulation in theory... any
        // better advice?)
        // we can use the same as for the queue logic
        VDFConfigGroup.getOrCreate(config).setCapacityFactor(
                ((QSimConfigGroup) config.getModules().get(QSimConfigGroup.GROUP_NAME)).getFlowCapFactor());

        // VDF: Disable queue logic
        config.qsim().setFlowCapFactor(1e9);

        // maybe we do not want to disable storage capacity logic
        // as we might want to have some back propagation delays
        config.qsim().setStorageCapFactor(1e9);

        // VDF: Optional
        VDFConfigGroup.getOrCreate(config).setWriteInterval(1);
        VDFConfigGroup.getOrCreate(config).setWriteFlowInterval(1);

        // VDF Engine: Add config group
        config.addModule(new VDFEngineConfigGroup());

        // VDF Engine: Decide whether to genertae link events or not
        VDFEngineConfigGroup.getOrCreate(config).setGenerateNetworkEvents(false);

        // VDF Engine: Remove car from main modes
        Set<String> mainModes = new HashSet<>(config.qsim().getMainModes());
        mainModes.remove("car");
        config.qsim().setMainModes(mainModes);

        Scenario scenario = ScenarioUtils.createScenario(config);
        configurator.configureScenario(scenario);
        ScenarioUtils.loadScenario(scenario);
        configurator.adjustScenario(scenario);

        Controler controller = new Controler(scenario);
        configurator.configureController(controller);
        controller.run();
    }
}