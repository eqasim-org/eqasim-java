package org.eqasim.switzerland.ch_cmdp.StrategyWeightDecay;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.controler.AbstractModule;

public class StrategyWeightDecayModule extends AbstractModule {
    private final Logger logger = LogManager.getLogger(StrategyWeightDecayModule.class);

    @Override
    public void install() {
        logger.info("Installing StrategyWeightDecayModule...");
        addControllerListenerBinding().to(StrategyWeightDecay.class);
    }

}
