package org.eqasim.san_francisco;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.san_francisco.mode_choice.SanFranciscoModeChoiceModule;
import org.matsim.core.config.CommandLine;

public class SanFranciscoConfigurator extends EqasimConfigurator {
    public SanFranciscoConfigurator(CommandLine cmd) {
        super(cmd);

        registerModule(new SanFranciscoModeChoiceModule(cmd));
    }
}
