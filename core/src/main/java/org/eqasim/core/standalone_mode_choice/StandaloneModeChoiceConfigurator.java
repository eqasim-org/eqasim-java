package org.eqasim.core.standalone_mode_choice;

import org.eqasim.core.analysis.DefaultPersonAnalysisFilter;
import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.TransitWithAbstractAbstractAccessModuleConfigGroup;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.TransitWithAbstractAccessModule;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.mode_choice.TransitWithAbstractAccessModeChoiceModule;
import org.eqasim.core.simulation.termination.EqasimTerminationModule;
import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.eqasim.core.simulation.vdf.VDFModule;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.utils.timing.TimeInterpretationModule;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StandaloneModeChoiceConfigurator {

    private final Config config;
    private final CommandLine commandLine;

    private final Map<String, List<AbstractModule>> optionalModules;

    public StandaloneModeChoiceConfigurator(Config config, CommandLine commandLine) {
        this.config = config;
        this.commandLine = commandLine;
        this.optionalModules = new LinkedHashMap<>();

        this.registerOptionalModule(new VDFConfigGroup(), new VDFModule());
        this.registerOptionalModule(TransitWithAbstractAbstractAccessModuleConfigGroup.GROUP_NAME, new TransitWithAbstractAccessModeChoiceModule());
        this.registerOptionalModule(TransitWithAbstractAbstractAccessModuleConfigGroup.GROUP_NAME, new TransitWithAbstractAccessModule());
    }

    public Config getConfig() {
        return config;
    }

    public CommandLine getCommandLine() {
        return commandLine;
    }

    protected void registerOptionalModule(ConfigGroup configGroup, AbstractModule module) {
        this.registerOptionalModule(configGroup.getName(), module);
    }

    protected void registerOptionalModule(String moduleName, AbstractModule module) {
        this.optionalModules.computeIfAbsent(moduleName, key -> new ArrayList<>()).add(module);
    }

    public final List<AbstractModule> getModeChoiceModules(Config config) {
        List<AbstractModule> modules = new ArrayList<>();
        new EqasimConfigurator().getModules(config).stream().filter(module -> !(module instanceof EqasimTerminationModule)).forEach(modules::add);
        modules.add(new TimeInterpretationModule());
        modules.add(new StandaloneModeChoiceModule(getConfig()));
        // We add a module that just binds the PersonAnalysisFilter without having to add the whole EqasimAnalysisModule
        // This bind is required for building the TripReaderFromPopulation object
        modules.add(new AbstractModule() {
            @Override
            public void install() {
                bind(PersonAnalysisFilter.class).to(DefaultPersonAnalysisFilter.class);
            }
        });
        modules.addAll(this.getSpecificModeChoiceModules());
        config.getModules().keySet().stream()
                .filter(this.optionalModules::containsKey)
                .flatMap(moduleName -> this.optionalModules.get(moduleName).stream())
                .forEach(modules::add);
        return modules;
    }

    protected List<AbstractModule> getSpecificModeChoiceModules() {
        return new ArrayList<>();
    }

    public static StandaloneModeChoiceConfigurator getSubclassInstance(String className, Config config, CommandLine commandLine) {
        try {
            Class<?> classDescription = Class.forName(className);
            boolean isStandaloneModeChoiceConfigurator = classDescription.equals(StandaloneModeChoiceConfigurator.class);
            Class<?> tempClassDescription = classDescription;
            while(!isStandaloneModeChoiceConfigurator && tempClassDescription != null && !tempClassDescription.equals(Object.class)) {
                tempClassDescription = tempClassDescription.getSuperclass();
                isStandaloneModeChoiceConfigurator = StandaloneModeChoiceConfigurator.class.equals(tempClassDescription);
            }
            if(!isStandaloneModeChoiceConfigurator) {
                throw new IllegalStateException(String.format("Class %s does not extend %s", classDescription.getCanonicalName(), StandaloneModeChoiceConfigurator.class.getCanonicalName()));
            }
            return (StandaloneModeChoiceConfigurator) classDescription.getConstructor(Config.class, CommandLine.class).newInstance(config, commandLine);
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(String.format("Class %s needs to have a constructor that takes arguments of types %s and %s", className, Config.class.getCanonicalName(), CommandLine.class.getCanonicalName()), e);
        }
    }
}
