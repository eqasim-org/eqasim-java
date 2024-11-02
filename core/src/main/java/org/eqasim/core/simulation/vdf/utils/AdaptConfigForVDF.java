package org.eqasim.core.simulation.vdf.utils;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.eqasim.core.simulation.vdf.engine.VDFEngineConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import java.util.HashSet;
import java.util.Set;

public class AdaptConfigForVDF {

    public static void adaptConfigForVDF(Config config, boolean engine) {
        // VDF: Add config group
        config.addModule(new VDFConfigGroup());

        // VDF: Disable queue logic
        config.qsim().setFlowCapFactor(1e9);
        config.qsim().setStorageCapFactor(1e9);


        VDFConfigGroup.getOrCreate(config).setWriteInterval(1);
        VDFConfigGroup.getOrCreate(config).setWriteFlowInterval(1);

        // VDF: Set capacity factor instead (We retrieve it form the Eqasim config group)
        EqasimConfigGroup eqasimConfigGroup = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);
        VDFConfigGroup.getOrCreate(config).setCapacityFactor(eqasimConfigGroup.getSampleSize());

        if(engine) {
            // VDF Engine: Add config group
            config.addModule(new VDFEngineConfigGroup());

            // VDF Engine: Let's not generate network events by default
            VDFEngineConfigGroup.getOrCreate(config).setGenerateNetworkEvents(false);

            // VDF Engine: Remove car from main modes handled by qsim
            Set<String> mainModes = new HashSet<>(config.qsim().getMainModes());
            mainModes.remove("car");
            config.qsim().setMainModes(mainModes);
        }
    }

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine commandLine = new CommandLine.Builder(args)
                .requireOptions("input-config-path", "output-config-path")
                .allowOptions("engine")
                .build();

        String inputPath = commandLine.getOptionStrict("input-config-path");
        String outputPath = commandLine.getOptionStrict("output-config-path");
        boolean engine = Boolean.parseBoolean(commandLine.getOption("engine").orElse("false"));

        EqasimConfigurator eqasimConfigurator = new EqasimConfigurator();
        Config config = ConfigUtils.loadConfig(inputPath);
        eqasimConfigurator.updateConfig(config);
        adaptConfigForVDF(config, engine);
        commandLine.applyConfiguration(config);
        ConfigUtils.writeConfig(config, outputPath);
    }
}
