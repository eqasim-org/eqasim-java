package org.eqasim.ile_de_france.utils;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigGroup;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class EqasimScenarioGenerator extends ScenarioGenerator{
    public EqasimScenarioGenerator(String[] args) {
        super(args);
        this.getCmdBuilder().allowOptions("eqasim-configurator-class");
    }

    @Override
    protected ConfigGroup[] getConfigGroups() throws CommandLine.ConfigurationException {
        Map<String, String> extraOptions = getExtraOptions();
        EqasimConfigurator configurator;
        if(extraOptions.containsKey("eqasim-configurator-class")) {
            try {
                Class<?> eqasimConfiguratorClass = Class.forName(extraOptions.get("eqasim-configurator-class"));
                boolean extendsEqasimConfigurator = false;
                for(Class<?> ancestor = eqasimConfiguratorClass; !ancestor.isPrimitive(); ancestor = ancestor.getSuperclass()) {
                    if(ancestor.equals(EqasimConfigurator.class)) {
                        extendsEqasimConfigurator = true;
                        break;
                    }
                }
                if(!extendsEqasimConfigurator) {
                    throw new CommandLine.ConfigurationException(String.format("Specified class %s does not extend org.eqasim.core.simulation.EqasimConfigurator", extraOptions.get("eqasim-configurator-class")));
                }
                try {
                    configurator = (EqasimConfigurator) eqasimConfiguratorClass.getConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    throw new CommandLine.ConfigurationException(String.format("Could not create object of class %s, does it have a public constructor that doesn't take any arguments ?", extraOptions.get("eqasim-configurator-class")));
                }
            } catch (ClassNotFoundException e) {
                throw new CommandLine.ConfigurationException("Class not found " + extraOptions.get("eqasim-configurator-class"));
            }
        } else {
            configurator = new EqasimConfigurator();
        }
        return configurator.getConfigGroups();
    }

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        EqasimScenarioGenerator scenarioGenerator = new EqasimScenarioGenerator(args);
        scenarioGenerator.generate();
    }
}
