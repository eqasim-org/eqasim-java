package org.eqasim.core.scenario.config;

import java.util.*;
import java.util.stream.Collectors;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;

import com.google.common.base.Verify;

/**
 * This script allows to edit a MATSim configuration file exclusively through the command line. <br/>
 * While existing modules can be configured in most simulation running scripts using the `config:` prefix, existing modules cannot be removed and non-existing ones cannot be added. This is what this script allows to do. <br/>
 * The script can be used with the following command line arguments:
 * <ul>
 * <li>`{@value CMD_INPUT_PATH}`: required</li>
 * <li>`{@value CMD_OUTPUT_PATH}`: required</li>
 * <li>optional arguments in the form '{@value CMD_ADD_MODULE_PREFIX}:moduleName' add the given module to the config. Module config groups matching the provided names are searched in the core modules and the optional modules specified by an {@link EqasimConfigurator} </li>
 * <li>optional arguments in the form `{@value CMD_REMOVE_MODULE_PREFIX}:moduleName` remove the given module from the config </li>
 * </ul>
 * After adding and removing modules, configuration elements can still be adapted using the `config:` prefix.
 * @author Tarek Chouaki (tkchouaki)
 */
public class EditConfig {

    public static final String CMD_INPUT_PATH = "input-path";
    public static final String CMD_OUTPUT_PATH = "output-path";
    public static final String CMD_ADD_MODULE_PREFIX = "add-module";
    public static final String CMD_REMOVE_MODULE_PREFIX = "remove-module";
    public static final String CMD_ADD_PARAMETERSET_PREFIX = "add-parameterset";
    public static final String CMD_REMOVE_PARAMETERSET_PREFIX = "remove-parameterset";

    public static ConfigGroup getParameterSet(ConfigGroup parent, String parameterSetName) {
        String[] components = parameterSetName.split("\\.");
        Collection<? extends ConfigGroup> parameterSets = parent.getParameterSets(components[0]);
        if(parameterSets.isEmpty()) {
            return null;
        }
        if(parameterSets.size() > 1) {
            throw new IllegalArgumentException(String.format("Multiple parameter sets '%s' found", components[0]));
        }
        ConfigGroup parameterSet = parameterSets.iterator().next();
        if(components.length == 1) {
            return parameterSet;
        }
        return getParameterSet(parameterSet, String.join(".", Arrays.copyOfRange(components, 1, components.length)));
    }

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args)
                .requireOptions(CMD_INPUT_PATH, CMD_OUTPUT_PATH)
                .allowPrefixes(CMD_ADD_MODULE_PREFIX)
                .allowPrefixes(CMD_REMOVE_MODULE_PREFIX)
                .allowPrefixes(CMD_ADD_PARAMETERSET_PREFIX)
                .allowPrefixes(CMD_REMOVE_PARAMETERSET_PREFIX)
                .allowOptions(EqasimConfigurator.CONFIGURATOR)
                .build();

        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict(CMD_INPUT_PATH));

        Set<String> addedModules = new HashSet<>();
        Set<String> removedModules = new HashSet<>();
        Set<String> addedParameterSets = new HashSet<>();
        Set<String> removedParameterSets = new HashSet<>();

        for(String option: cmd.getAvailableOptions()) {
            if(option.startsWith(CMD_ADD_MODULE_PREFIX)) {
                Verify.verify(cmd.getOptionStrict(option).equals("true"), String.format("Options prefix with --%s must be used without values", CMD_ADD_MODULE_PREFIX));
                addedModules.add(option.substring(CMD_ADD_MODULE_PREFIX.length() + 1));
            } else if (option.startsWith(CMD_REMOVE_MODULE_PREFIX)) {
                Verify.verify(cmd.getOptionStrict(option).equals("true"), String.format("Options prefix with --%s must be used without values", CMD_REMOVE_MODULE_PREFIX));
                removedModules.add(option.substring(CMD_REMOVE_MODULE_PREFIX.length() + 1));
            } else if(option.startsWith(CMD_ADD_PARAMETERSET_PREFIX)) {
                addedParameterSets.add(option.substring(CMD_ADD_PARAMETERSET_PREFIX.length() + 1));
            } else if(option.startsWith(CMD_REMOVE_PARAMETERSET_PREFIX)) {
                removedParameterSets.add(option.substring(CMD_REMOVE_PARAMETERSET_PREFIX.length() + 1));
            }
        }

        Set<String> conflictingModules = new HashSet<>(addedModules);
        conflictingModules.retainAll(removedModules);
        Verify.verify(conflictingModules.isEmpty(), "Attempting to remove and add the same module %s", conflictingModules.stream().findFirst().orElse(null));

        for(String module: removedModules) {
            config.removeModule(module);
        }

        EqasimConfigurator eqasimConfigurator = EqasimConfigurator.getInstance(cmd);

        Map<String, ConfigGroup> availableModules = new HashMap<>(ConfigUtils.createConfig().getModules());
        availableModules.putAll(eqasimConfigurator.getRegisteredConfigGroups().stream().collect(Collectors.toMap(ConfigGroup::getName, configGroup -> configGroup)));

        for(String module: addedModules) {
            ConfigGroup configGroup = availableModules.get(module);
            Verify.verify(configGroup != null, String.format("Cannot find config group for module %s, make sure you are using the correct eqasim-configurator", module));
            config.addModule(configGroup);
        }

        for(String parameterSet: removedParameterSets.stream().sorted(Comparator.comparingInt(s -> -s.length())).toList()) {
            int firstDotIndex = parameterSet.indexOf('.');
            int lastDotIndex = parameterSet.lastIndexOf('.');
            Verify.verify(firstDotIndex >= 0, "parameterSet to remove must be of the form module.(parentParameterSet.)*targetParameterSet");
            ConfigGroup module = config.getModules().get(parameterSet.substring(0, firstDotIndex));
            if (module == null) {
                continue;
            }
            ConfigGroup parentParameterSet;
            if(firstDotIndex == lastDotIndex) {
                parentParameterSet = module;
            } else {
                parentParameterSet = getParameterSet(module, parameterSet.substring(firstDotIndex+1, lastDotIndex));
            }
            if(parentParameterSet != null) {
                ConfigGroup targetParameterSet = getParameterSet(parentParameterSet, parameterSet.substring(lastDotIndex+1));
                if(targetParameterSet != null) {
                    parentParameterSet.removeParameterSet(targetParameterSet);
                }
            }
        }

        for(String parameterSet: addedParameterSets.stream().sorted(Comparator.comparingInt(String::length)).toList()) {
            String[] components = parameterSet.split("\\.");
            Verify.verify(components.length > 1, "parameterSet to remove must be of the form module.(parentParameterSet.)*targetParameterSet");
            ConfigGroup module = config.getModules().get(components[0]);
            Verify.verify(module != null, String.format("Cannot find module %s to add a parameter set into, please add it first", components[0]));
            ConfigGroup currentParent = module;
            for(int i=1; i<components.length-1; i++) {
                ConfigGroup currentParameterSet = getParameterSet(currentParent, components[i]);
                if(currentParameterSet == null) {
                    currentParameterSet = currentParent.createParameterSet(components[i]);
                    currentParent.addParameterSet(currentParameterSet);
                }
                currentParent = currentParameterSet;
            }
            currentParent.addParameterSet(currentParent.createParameterSet(components[components.length-1]));
        }

        cmd.applyConfiguration(config);

        ConfigUtils.writeConfig(config, cmd.getOptionStrict(CMD_OUTPUT_PATH));
    }
}
