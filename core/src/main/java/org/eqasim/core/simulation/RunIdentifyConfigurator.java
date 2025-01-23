package org.eqasim.core.simulation;

import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

public class RunIdentifyConfigurator {
    static public void main(String[] args) throws ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args)
                .allowOptions(EqasimConfigurator.CONFIGURATOR)
                .build();

        EqasimConfigurator.getInstance(cmd);
    }
}
