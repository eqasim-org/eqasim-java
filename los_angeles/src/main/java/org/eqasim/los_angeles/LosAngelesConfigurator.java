package org.eqasim.los_angeles;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.los_angeles.mode_choice.LosAngelesModeChoiceModule;
import org.matsim.core.config.CommandLine;

public class LosAngelesConfigurator extends EqasimConfigurator {
    public LosAngelesConfigurator(CommandLine cmd) {
        super();

        registerModule(new LosAngelesModeChoiceModule(cmd));
    }
}
