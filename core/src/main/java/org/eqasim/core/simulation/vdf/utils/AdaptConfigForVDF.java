package org.eqasim.core.simulation.vdf.utils;

import java.util.HashSet;
import java.util.Set;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.eqasim.core.simulation.vdf.engine.VDFEngineConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

public class AdaptConfigForVDF {

    public static void adaptConfigForVDF(Config config, boolean useEngine, int generateEventsInterval) {
        // VDF: Add config group
        config.addModule(new VDFConfigGroup());

        // VDF: Disable queue logic
        config.qsim().setFlowCapFactor(1e9);
        config.qsim().setStorageCapFactor(1e9);

        VDFConfigGroup.getOrCreate(config).setWriteInterval(60);
        VDFConfigGroup.getOrCreate(config).setWriteFlowInterval(60);
        VDFConfigGroup.getOrCreate(config).setWriteTravelTimesInterval(60);

        VDFConfigGroup vdfConfigGroup = VDFConfigGroup.getOrCreate(config);
        vdfConfigGroup.setHandler(VDFConfigGroup.HandlerType.SparseHorizon);

        if (useEngine) {
            // VDF Engine: Add config group
            config.addModule(new VDFEngineConfigGroup());

            // VDF Engine: Let's not generate network events by default
            VDFEngineConfigGroup.getOrCreate(config).setGenerateNetworkEventsInterval(generateEventsInterval);

            // VDF Engine: Remove car from main modes handled by qsim
            Set<String> mainModes = new HashSet<>(config.qsim().getMainModes());
            mainModes.remove("car");
            config.qsim().setMainModes(mainModes);
        }
    }

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine commandLine = new CommandLine.Builder(args)
                .requireOptions("input-config-path", "output-config-path")
                .allowOptions("use-engine", "events-interval", EqasimConfigurator.CONFIGURATOR)
                .build();

        String inputPath = commandLine.getOptionStrict("input-config-path");
        String outputPath = commandLine.getOptionStrict("output-config-path");
        boolean useEngine = commandLine.getOption("use-engine").map(Boolean::parseBoolean).orElse(false);
        int generateEventsInterval = commandLine.getOption("events-interval").map(Integer::parseInt).orElse(1);

        EqasimConfigurator eqasimConfigurator = EqasimConfigurator.getInstance(commandLine);
        Config config = ConfigUtils.loadConfig(inputPath);
        eqasimConfigurator.updateConfig(config);
        adaptConfigForVDF(config, useEngine, generateEventsInterval);
        commandLine.applyConfiguration(config);
        ConfigUtils.writeConfig(config, outputPath);
    }
}
