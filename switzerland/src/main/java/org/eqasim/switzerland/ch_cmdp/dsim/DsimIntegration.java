package org.eqasim.switzerland.ch_cmdp.dsim;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

/** Explicit engine selection; both engines are included in the normal build. */
public interface DsimIntegration {
    String OPTION = "use-dsim";
    String SIMULATE_BIKES_OPTION = "dsim-simulate-bikes";

    static boolean isEnabled(CommandLine command) throws CommandLine.ConfigurationException {
        String value = command.getOption(OPTION).orElse("false");
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new CommandLine.ConfigurationException("--" + OPTION + " must be true or false: " + value);
    }

    static boolean simulateBikes(CommandLine command) throws CommandLine.ConfigurationException {
        String value = command.getOption(SIMULATE_BIKES_OPTION).orElse("false");
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new CommandLine.ConfigurationException("--" + SIMULATE_BIKES_OPTION
                + " must be true or false: " + value);
    }

    static DsimIntegration load(boolean simulateBikes) {
        return new DsimIntegration2026(simulateBikes);
    }

    static DsimIntegration fromCommand(CommandLine command) throws CommandLine.ConfigurationException {
        return fromCommand(command, null);
    }

    /** XML selects the baseline engine; --use-dsim true may temporarily enable DSim on a QSim config. */
    static DsimIntegration fromCommand(CommandLine command, Config config) throws CommandLine.ConfigurationException {
        boolean enabled = isEnabled(command) || (config != null && "dsim".equals(config.controller().getMobsim()));
        boolean simulateBikes = simulateBikes(command);
        if (simulateBikes && !enabled) {
            throw new CommandLine.ConfigurationException("--" + SIMULATE_BIKES_OPTION
                    + " requires controller.mobsim=dsim or --" + OPTION + " true");
        }
        if (enabled) return load(simulateBikes);
        return null;
    }

    void adaptConfig(Config config);
    void adaptScenario(Scenario scenario);
    void configureController(Controler controller, EqasimConfigurator configurator);
}
