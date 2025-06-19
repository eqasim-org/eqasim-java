package org.eqasim.core.simulation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.CommandLine;

public class CoreEqasimConfigurator extends EqasimConfigurator {
    private static final Logger logger = LogManager.getLogger(CoreEqasimConfigurator.class);

    public CoreEqasimConfigurator(CommandLine cmd) {
        super(cmd);

        logger.warn(
                "Your are using the CoreEqasimConfigurator.");
        logger.warn("This should only happen if you intentionally set this up for testing or development purposes.");
        logger.warn("The CoreEqasimConfigrator should never be used in production.");

    }
}
