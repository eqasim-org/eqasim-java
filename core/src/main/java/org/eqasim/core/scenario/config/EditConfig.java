package org.eqasim.core.scenario.config;

import com.google.common.base.Verify;
import org.eqasim.core.misc.ClassUtils;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.Config;

import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This script allows to edit a MATSim configuration file exclusively through the command line.
 * While existing modules can be configured in most simulation running scripts using the `config:` prefix, existing modules cannot be removed and non-existing ones cannot be added. This is what this script allows to do.
 * The script can be used with the following command line arguments:
 * - `{@value CMD_INPUT_PATH}`: required
 * - `{@value CMD_OUTPUT_PATH}`: required
 * - optional arguments in the form '{@value CMD_ADD_MODULE_PREFIX}:moduleName' add the given module to the config. Module config groups matching the provided names are searched in the core modules and the optional modules specified by an {@link EqasimConfigurator}
 * - optional arguments in the form `{@value CMD_REMOVE_MODULE_PREFIX}:moduleName` remove the given module from the config
 * - `{@value CMD_EQASIM_CONFIGURATOR}` the full name of a class extending the {@link EqasimConfigurator} class from which optional modules config groups are retrieved
 */
public class EditConfig {

    public static final String CMD_INPUT_PATH = "input-path";
    public static final String CMD_OUTPUT_PATH = "output-path";
    public static final String CMD_ADD_MODULE_PREFIX = "add-module";
    public static final String CMD_REMOVE_MODULE_PREFIX = "remove-module";
    public static final String CMD_EQASIM_CONFIGURATOR= "eqasim-configurator";

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args)
                .requireOptions(CMD_INPUT_PATH, CMD_OUTPUT_PATH)
                .allowPrefixes(CMD_ADD_MODULE_PREFIX)
                .allowPrefixes(CMD_REMOVE_MODULE_PREFIX)
                .allowOptions(CMD_EQASIM_CONFIGURATOR)
                .build();

        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict(CMD_INPUT_PATH));

        Set<String> addedModules = new HashSet<>();
        Set<String> removedModules = new HashSet<>();

        for(String option: cmd.getAvailableOptions()) {
            if(option.startsWith(CMD_ADD_MODULE_PREFIX)) {
                Verify.verify(cmd.getOptionStrict(option).equals("true"), String.format("Options prefix with --%s must be used without values", CMD_ADD_MODULE_PREFIX));
                addedModules.add(option.substring(CMD_ADD_MODULE_PREFIX.length() + 1));
            } else if (option.startsWith(CMD_REMOVE_MODULE_PREFIX)) {
                Verify.verify(cmd.getOptionStrict(option).equals("true"), String.format("Options prefix with --%s must be used without values", CMD_REMOVE_MODULE_PREFIX));
                removedModules.add(option.substring(CMD_REMOVE_MODULE_PREFIX.length() + 1));
            }
        }

        Set<String> conflictingModules = new HashSet<>(addedModules);
        conflictingModules.retainAll(removedModules);
        Verify.verify(conflictingModules.isEmpty(), "Attempting to remove and add the same module %s", conflictingModules.stream().findFirst().orElse(null));

        for(String module: removedModules) {
            config.removeModule(module);
        }

        EqasimConfigurator eqasimConfigurator = cmd.hasOption(CMD_EQASIM_CONFIGURATOR) ? ClassUtils.getInstanceOfClassExtendingOtherClass(cmd.getOptionStrict(CMD_EQASIM_CONFIGURATOR), EqasimConfigurator.class) : new EqasimConfigurator();

        Map<String, ConfigGroup> availableModules = new HashMap<>(ConfigUtils.createConfig().getModules());
        availableModules.putAll(eqasimConfigurator.getRegisteredConfigGroups().stream().collect(Collectors.toMap(ConfigGroup::getName, configGroup -> configGroup)));

        for(String module: addedModules) {
            ConfigGroup configGroup = availableModules.get(module);
            Verify.verify(configGroup != null, String.format("Cannot find config group for module %s, make sure you are using the correct eqasim-configurator", module));
            config.addModule(configGroup);
        }

        cmd.applyConfiguration(config);

        ConfigUtils.writeConfig(config, cmd.getOptionStrict(CMD_OUTPUT_PATH));
    }
}
