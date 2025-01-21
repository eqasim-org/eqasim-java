package org.eqasim.sao_paulo;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.sao_paulo.mode_choice.SaoPauloModeChoiceModule;
import org.matsim.core.config.CommandLine;

public class SaoPauloConfigurator extends EqasimConfigurator {
    public SaoPauloConfigurator(CommandLine cmd) {
        super(cmd);

        registerModule(new SaoPauloModeChoiceModule(cmd));
    }
}
