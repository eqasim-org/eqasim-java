package org.eqasim.ile_de_france;

import org.eqasim.core.standalone_mode_choice.StandaloneModeChoiceConfigurator;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;

import java.util.List;

public class IDFStandaloneModeChoiceConfigurator extends StandaloneModeChoiceConfigurator {
    public IDFStandaloneModeChoiceConfigurator(Config config, CommandLine commandLine) {
        super(config, commandLine);
    }

    protected List<AbstractModule> getSpecificModeChoiceModules() {
        return List.of(new IDFModeChoiceModule(this.getCommandLine()));
    }
}
